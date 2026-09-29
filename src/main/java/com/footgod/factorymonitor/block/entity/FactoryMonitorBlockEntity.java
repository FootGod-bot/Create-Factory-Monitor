package com.footgod.factorymonitor.block.entity;

import java.util.ArrayList;
import java.util.List;

import com.footgod.factorymonitor.logistics.FactoryMonitorPromiseTracker;
import com.footgod.factorymonitor.registry.ModBlockEntities;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.filter.PackageFilterItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.packagerLink.RequestPromise;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.createmod.catnip.math.VecHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.items.IItemHandler;

public class FactoryMonitorBlockEntity extends SmartBlockEntity
        implements ThresholdSwitchObservable {

    public enum MonitorMode {
        STORED,
        PROMISED,
        TOTAL
    }

    private MonitorMode mode;

    private LogisticallyLinkedBehaviour logisticsBehaviour;
    private FilteringBehaviour filtering;

    private ScrollValueBehaviour northMode;
    private ScrollValueBehaviour southMode;
    private ScrollValueBehaviour eastMode;
    private ScrollValueBehaviour westMode;

    private final MonitorItemHandler itemHandler;

    private boolean updatingModeSliders;

    public FactoryMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FACTORY_MONITOR.get(), pos, state);

        mode = MonitorMode.STORED;
        itemHandler = new MonitorItemHandler(this);

        setLazyTickRate(10);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

        filtering =
                new FilteringBehaviour(
                        this,
                        new MonitorFilterSlot()
                ).withCallback(stack -> {
                    itemHandler.invalidate();

                    if (level != null && !level.isClientSide) {
                        setChanged();
                        sendData();
                    }
                });

        filtering.setLabel(
                Component.translatable("factorymonitor.filter")
        );

        behaviours.add(filtering);

        logisticsBehaviour =
                new LogisticallyLinkedBehaviour(this, false);

        behaviours.add(logisticsBehaviour);

        northMode = createModeSlider(Direction.NORTH);
        southMode = createModeSlider(Direction.SOUTH);
        eastMode = createModeSlider(Direction.EAST);
        westMode = createModeSlider(Direction.WEST);

        behaviours.add(northMode);
        behaviours.add(southMode);
        behaviours.add(eastMode);
        behaviours.add(westMode);
    }

    private ScrollValueBehaviour createModeSlider(Direction side) {

        ScrollValueBehaviour behaviour =
                new ScrollValueBehaviour(
                        Component.translatable("factorymonitor.mode"),
                        this,
                        new MonitorModeSlot(side)
                )
                        .between(
                                0,
                                MonitorMode.values().length - 1
                        )
                        .withFormatter(this::formatMode);

        behaviour.withCallback(value -> {

            MonitorMode[] modes = MonitorMode.values();

            if (value < 0 || value >= modes.length) {
                return;
            }

            MonitorMode newMode = modes[value];

            if (mode == null) {
                mode = MonitorMode.STORED;
            }

            if (mode != newMode) {
                mode = newMode;

                itemHandler.invalidate();

                setChanged();

                if (level != null && !level.isClientSide) {
                    sendData();
                }
            }

            syncModeSliders(behaviour);
        });

        return behaviour;
    }

    private void syncModeSliders() {
        syncModeSliders(null);
    }

    private void syncModeSliders(
            ScrollValueBehaviour source
    ) {

        if (updatingModeSliders) {
            return;
        }

        updatingModeSliders = true;

        try {
            int value = getMode().ordinal();

            if (northMode != null && northMode != source) {
                northMode.setValue(value);
            }

            if (southMode != null && southMode != source) {
                southMode.setValue(value);
            }

            if (eastMode != null && eastMode != source) {
                eastMode.setValue(value);
            }

            if (westMode != null && westMode != source) {
                westMode.setValue(value);
            }

        } finally {
            updatingModeSliders = false;
        }
    }

    private String formatMode(int value) {

        return switch (value) {
            case 0 -> "Stored";
            case 1 -> "Promised";
            case 2 -> "Total";
            default -> "Stored";
        };
    }

    public LogisticallyLinkedBehaviour getLogisticsBehaviour() {
        return logisticsBehaviour;
    }

    public FilteringBehaviour getFiltering() {
        return filtering;
    }

    public MonitorMode getMode() {
        return mode == null
                ? MonitorMode.STORED
                : mode;
    }

    public void setMode(MonitorMode newMode) {

        if (newMode == null) {
            newMode = MonitorMode.STORED;
        }

        if (mode == newMode) {
            syncModeSliders();
            return;
        }

        mode = newMode;

        itemHandler.invalidate();

        syncModeSliders();

        setChanged();

        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    /*
     * ============================================================
     * Create Threshold Switch integration
     * ============================================================
     *
     * The Threshold Switch gets three independent values:
     *
     *   Minimum  = 0
     *   Current  = actual amount exposed by this monitor
     *   Maximum  = Integer.MAX_VALUE - 1
     *
     * This intentionally does NOT use the IItemHandler's slot
     * capacity. That prevents Create from calculating a small or
     * zero maximum based on the virtual inventory slots.
     */

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {

        long total = 0;

        for (ItemStack stack : getExposedItems()) {

            if (stack.isEmpty() || stack.getCount() <= 0) {
                continue;
            }

            total += stack.getCount();

            if (total >= Integer.MAX_VALUE - 1) {
                return Integer.MAX_VALUE - 1;
            }
        }

        return (int) total;
    }

    @Override
    public int getMaxValue() {
        return Integer.MAX_VALUE - 1;
    }

    @Override
    public MutableComponent format(int value) {
        return Component.literal(Integer.toString(value));
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public InventorySummary getRecentNetworkSummary() {

        if (logisticsBehaviour == null) {
            return InventorySummary.EMPTY;
        }

        return LogisticsManager.getSummaryOfNetwork(
                logisticsBehaviour.freqId,
                false
        );
    }

    public InventorySummary getAccurateNetworkSummary() {

        if (logisticsBehaviour == null) {
            return InventorySummary.EMPTY;
        }

        return LogisticsManager.getSummaryOfNetwork(
                logisticsBehaviour.freqId,
                true
        );
    }

    private RequestPromiseQueue getPromiseQueue() {

        if (logisticsBehaviour == null) {
            return null;
        }

        return Create.LOGISTICS.getQueuedPromises(
                logisticsBehaviour.freqId
        );
    }

    /**
     * Adds a promised item to the summary.
     *
     * Package Filters are handled separately because the actual
     * destination belongs to the RequestPromise's Factory Panel
     * request rather than the promised ItemStack itself.
     */
    private void addPromisedItem(
            InventorySummary destination,
            RequestPromise promise
    ) {

        if (promise == null ||
                promise.promisedStack == null ||
                promise.promisedStack.stack == null ||
                promise.promisedStack.stack.isEmpty() ||
                promise.promisedStack.count <= 0) {
            return;
        }

        if (!passesFilter(promise)) {
            return;
        }

        destination.add(
                promise.promisedStack.stack,
                promise.promisedStack.count
        );
    }

    /**
     * Applies the Factory Monitor's top filter.
     *
     * Normal Create filters operate on the promised item.
     *
     * A Package Filter instead operates on the destination
     * address captured from the exact RequestPromise.
     */
    private boolean passesFilter(
            RequestPromise promise
    ) {

        if (filtering == null) {
            return true;
        }

        ItemStack filterStack =
                filtering.getFilter();

        if (filterStack == null ||
                filterStack.isEmpty()) {
            return true;
        }

        if (filterStack.getItem() instanceof PackageFilterItem) {

            String wantedAddress =
                    PackageItem.getAddress(filterStack);

            String promisedAddress =
                    FactoryMonitorPromiseTracker.getAddress(promise);

            return PackageItem.matchAddress(
                    promisedAddress,
                    wantedAddress
            );
        }

        return filtering.test(
                promise.promisedStack.stack
        );
    }

    /**
     * Builds the promised inventory while preserving the
     * destination associated with each RequestPromise.
     */
    private InventorySummary getPromisedNetworkSummary() {

        InventorySummary summary =
                new InventorySummary();

        RequestPromiseQueue promises =
                getPromiseQueue();

        if (promises == null) {
            return summary;
        }

        for (RequestPromise promise :
                promises.flatten(false)) {

            addPromisedItem(
                    summary,
                    promise
            );
        }

        return summary;
    }

    /**
     * Builds the complete inventory exposed to Threshold Switch.
     */
    public List<ItemStack> getExposedItems() {

        InventorySummary stored =
                getAccurateNetworkSummary();

        InventorySummary promised =
                getPromisedNetworkSummary();

        InventorySummary exposed =
                new InventorySummary();

        switch (getMode()) {

            case STORED -> {
                addFilteredSummary(
                        exposed,
                        stored
                );
            }

            case PROMISED -> {
                /*
                 * Promised was already filtered while reading
                 * RequestPromise objects so Package Filters can
                 * inspect their destinations.
                 */
                addSummary(
                        exposed,
                        promised
                );
            }

            case TOTAL -> {
                addFilteredSummary(
                        exposed,
                        stored
                );

                addSummary(
                        exposed,
                        promised
                );
            }
        }

        List<ItemStack> result =
                new ArrayList<>();

        for (BigItemStack entry :
                exposed.getStacks()) {

            if (entry.stack.isEmpty() ||
                    entry.count <= 0) {
                continue;
            }

            long remaining =
                    entry.count;

            int maxStackSize =
                    entry.stack.getMaxStackSize();

            if (maxStackSize <= 0) {
                maxStackSize = 64;
            }

            while (remaining > 0) {

                int amount =
                        (int) Math.min(
                                remaining,
                                maxStackSize
                        );

                ItemStack stack =
                        entry.stack.copy();

                stack.setCount(amount);

                result.add(stack);

                remaining -= amount;
            }
        }

        return result;
    }

    private void addSummary(
            InventorySummary destination,
            InventorySummary source
    ) {

        if (source == null || source.isEmpty()) {
            return;
        }

        for (BigItemStack entry :
                source.getStacks()) {

            if (entry.stack.isEmpty() ||
                    entry.count <= 0) {
                continue;
            }

            destination.add(
                    entry.stack,
                    entry.count
            );
        }
    }

    private void addFilteredSummary(
            InventorySummary destination,
            InventorySummary source
    ) {

        if (source == null || source.isEmpty()) {
            return;
        }

        /*
         * Package Filters have no meaning for stored inventory,
         * because there is no destination-bearing package there.
         */
        if (isPackageFilter()) {
            return;
        }

        for (BigItemStack entry :
                source.getStacks()) {

            if (entry.stack.isEmpty()) {
                continue;
            }

            if (filtering != null &&
                    !filtering.getFilter().isEmpty() &&
                    !filtering.test(entry.stack)) {
                continue;
            }

            destination.add(
                    entry.stack,
                    entry.count
            );
        }
    }

    private boolean isPackageFilter() {

        if (filtering == null) {
            return false;
        }

        ItemStack filterStack =
                filtering.getFilter();

        return filterStack != null &&
                !filterStack.isEmpty() &&
                filterStack.getItem() instanceof PackageFilterItem;
    }

    @Override
    public void lazyTick() {

        super.lazyTick();

        if (level == null ||
                level.isClientSide) {
            return;
        }

        itemHandler.invalidate();

        sendData();
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {

        super.write(
                tag,
                registries,
                clientPacket
        );

        tag.putString(
                "Mode",
                getMode().name()
        );
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {

        super.read(
                tag,
                registries,
                clientPacket
        );

        String modeName =
                tag.getString("Mode");

        try {
            mode =
                    MonitorMode.valueOf(modeName);

        } catch (IllegalArgumentException exception) {

            mode =
                    MonitorMode.STORED;
        }

        if (mode == null) {
            mode =
                    MonitorMode.STORED;
        }

        itemHandler.invalidate();

        if (clientPacket) {
            syncModeSliders();
        }
    }

    private static class MonitorItemHandler
            implements IItemHandler {

        private final FactoryMonitorBlockEntity monitor;

        private List<ItemStack> snapshot =
                List.of();

        private boolean dirty =
                true;

        private MonitorItemHandler(
                FactoryMonitorBlockEntity monitor
        ) {
            this.monitor = monitor;
        }

        private void refresh() {

            if (!dirty) {
                return;
            }

            snapshot =
                    monitor.getExposedItems();

            dirty =
                    false;
        }

        private void invalidate() {
            dirty = true;
        }

        @Override
        public int getSlots() {

            refresh();

            return snapshot.size();
        }

        @Override
        public ItemStack getStackInSlot(
                int slot
        ) {

            refresh();

            if (slot < 0 ||
                    slot >= snapshot.size()) {
                return ItemStack.EMPTY;
            }

            return snapshot
                    .get(slot)
                    .copy();
        }

        @Override
        public ItemStack insertItem(
                int slot,
                ItemStack stack,
                boolean simulate
        ) {
            return stack.copy();
        }

        @Override
        public ItemStack extractItem(
                int slot,
                int amount,
                boolean simulate
        ) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(
                int slot
        ) {

            refresh();

            if (slot < 0 ||
                    slot >= snapshot.size()) {
                return 0;
            }

            return snapshot
                    .get(slot)
                    .getMaxStackSize();
        }

        @Override
        public boolean isItemValid(
                int slot,
                ItemStack stack
        ) {
            return false;
        }
    }

    private static class MonitorModeSlot
            extends ValueBoxTransform.Sided {

        private MonitorModeSlot(
                Direction side
        ) {
            this.direction =
                    side;
        }

        @Override
        protected Vec3 getSouthLocation() {

            return VecHelper.voxelSpace(
                    8f,
                    8f,
                    15.5f
            );
        }

        @Override
        protected boolean isSideActive(
                BlockState state,
                Direction side
        ) {
            return side.getAxis().isHorizontal();
        }
    }

    private static class MonitorFilterSlot
            extends ValueBoxTransform.Sided {

        private MonitorFilterSlot() {
            this.direction =
                    Direction.UP;
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(
                    8f,
                    8f,
                    15.5f
            );
        }

        @Override
        protected boolean isSideActive(
                BlockState state,
                Direction side
        ) {
            return side == Direction.UP;
        }
    }
}