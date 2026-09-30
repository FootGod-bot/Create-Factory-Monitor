package com.footgod.factorymonitor.block.entity;

import java.util.ArrayList;
import java.util.List;

import com.footgod.factorymonitor.CreateFactoryMonitor;
import com.footgod.factorymonitor.CreateFactoryMonitorConfig;
import com.footgod.factorymonitor.logistics.FactoryMonitorPromiseTracker;
import com.footgod.factorymonitor.registry.ModBlockEntities;

import com.google.common.collect.ImmutableList;
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
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
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

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

@EventBusSubscriber(
        modid = CreateFactoryMonitor.MOD_ID
)
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

    /*
     * ============================================================
     * FE POWER
     * ============================================================
     */

    private final MonitorEnergyStorage energyStorage =
            new MonitorEnergyStorage(
                    1000,
                    100,
                    100
            );

    private boolean previousRequireFE;

    /**
     * Registers ONLY the FE capability.
     *
     * The item handler capability is still registered by
     * CreateFactoryMonitor.java.
     */
    @SubscribeEvent
    public static void registerCapabilities(
            RegisterCapabilitiesEvent event
    ) {

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.FACTORY_MONITOR.get(),
                (monitor, side) -> {

                    /*
                     * When require_fe is disabled, the Factory Monitor
                     * does not expose an FE capability at all.
                     */
                    if (!CreateFactoryMonitorConfig.REQUIRE_FE.get()) {
                        return null;
                    }

                    return monitor.energyStorage;
                }
        );
    }

    public FactoryMonitorBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.FACTORY_MONITOR.get(),
                pos,
                state
        );

        mode = MonitorMode.STORED;

        itemHandler =
                new MonitorItemHandler(this);

        setLazyTickRate(10);
    }

    @Override
    public void addBehaviours(
            List<BlockEntityBehaviour> behaviours
    ) {

        filtering =
                new FilteringBehaviour(
                        this,
                        new MonitorFilterSlot()
                ).withCallback(stack -> {

                    itemHandler.invalidate();

                    if (level != null &&
                            !level.isClientSide) {

                        setChanged();
                        sendData();
                    }
                });

        filtering.setLabel(
                Component.translatable(
                        "factorymonitor.filter"
                )
        );

        behaviours.add(filtering);

        logisticsBehaviour =
                new LogisticallyLinkedBehaviour(
                        this,
                        false
                );

        behaviours.add(
                logisticsBehaviour
        );

        northMode =
                createModeSlider(
                        Direction.NORTH
                );

        southMode =
                createModeSlider(
                        Direction.SOUTH
                );

        eastMode =
                createModeSlider(
                        Direction.EAST
                );

        westMode =
                createModeSlider(
                        Direction.WEST
                );

        behaviours.add(northMode);
        behaviours.add(southMode);
        behaviours.add(eastMode);
        behaviours.add(westMode);
    }

    private ScrollValueBehaviour createModeSlider(
            Direction side
    ) {

        ScrollValueBehaviour behaviour =
                new MonitorModeScrollValueBehaviour(
                        this,
                        side
                )
                        .between(
                                0,
                                MonitorMode.values().length - 1
                        )
                        .withFormatter(
                                FactoryMonitorBlockEntity::formatMode
                        );

        behaviour.withCallback(value -> {

            MonitorMode[] modes =
                    MonitorMode.values();

            if (value < 0 ||
                    value >= modes.length) {
                return;
            }

            MonitorMode newMode =
                    modes[value];

            if (mode == null) {
                mode =
                        MonitorMode.STORED;
            }

            if (mode != newMode) {

                mode =
                        newMode;

                itemHandler.invalidate();

                setChanged();

                if (level != null &&
                        !level.isClientSide) {

                    sendData();
                }
            }

            syncModeSliders(
                    behaviour
            );
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

            int value =
                    getMode().ordinal();

            if (northMode != null &&
                    northMode != source) {

                northMode.setValue(value);
            }

            if (southMode != null &&
                    southMode != source) {

                southMode.setValue(value);
            }

            if (eastMode != null &&
                    eastMode != source) {

                eastMode.setValue(value);
            }

            if (westMode != null &&
                    westMode != source) {

                westMode.setValue(value);
            }

        } finally {

            updatingModeSliders =
                    false;
        }
    }

    private static String formatMode(
            int value
    ) {

        return switch (value) {

            case 0 ->
                    Component.translatable(
                            "factorymonitor.mode.stored"
                    ).getString();

            case 1 ->
                    Component.translatable(
                            "factorymonitor.mode.promised"
                    ).getString();

            case 2 ->
                    Component.translatable(
                            "factorymonitor.mode.total"
                    ).getString();

            default ->
                    Component.translatable(
                            "factorymonitor.mode.stored"
                    ).getString();
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

    public void setMode(
            MonitorMode newMode
    ) {

        if (newMode == null) {
            newMode =
                    MonitorMode.STORED;
        }

        if (mode == newMode) {
            syncModeSliders();
            return;
        }

        mode =
                newMode;

        itemHandler.invalidate();

        syncModeSliders();

        setChanged();

        if (level != null &&
                !level.isClientSide) {

            sendData();
        }
    }

    /*
     * ============================================================
     * FE POWER HELPERS
     * ============================================================
     */

    private boolean requiresFE() {
        return CreateFactoryMonitorConfig.REQUIRE_FE.get();
    }

    /**
     * Returns true when the monitor is allowed to provide data.
     *
     * If FE is disabled in the config, the monitor always works.
     *
     * If FE is enabled, it must have enough FE for the next tick.
     */
    private boolean hasPower() {

        if (!requiresFE()) {
            return true;
        }

        int cost =
                CreateFactoryMonitorConfig.FE_PER_TICK.get();

        if (cost <= 0) {
            return true;
        }

        return energyStorage.getEnergyStored() >= cost;
    }

    /**
     * Consumes FE for one active tick.
     */
    private void consumePower() {

        if (!requiresFE()) {
            return;
        }

        int cost =
                CreateFactoryMonitorConfig.FE_PER_TICK.get();

        if (cost <= 0) {
            return;
        }

        if (energyStorage.getEnergyStored() >= cost) {

            energyStorage.extractEnergy(
                    cost,
                    false
            );
        }
    }

    public IEnergyStorage getEnergyStorage() {

        if (!requiresFE()) {
            return null;
        }

        return energyStorage;
    }

    @Override
    public void tick() {

        super.tick();

        if (level == null) {
            return;
        }

        boolean requireFE =
                requiresFE();

        /*
         * If the config changes while the world is running,
         * invalidate the capabilities and virtual inventory.
         */
        if (previousRequireFE != requireFE) {

            previousRequireFE =
                    requireFE;

            level.invalidateCapabilities(
                    worldPosition
            );

            itemHandler.invalidate();

            if (!level.isClientSide) {
                sendData();
            }
        }

        if (level.isClientSide) {
            return;
        }

        /*
         * Only consume FE when the monitor actually requires it.
         */
        if (requireFE &&
                hasPower()) {

            consumePower();

            itemHandler.invalidate();
        }
    }

    /*
     * ============================================================
     * Create Threshold Switch integration
     * ============================================================
     */

    @Override
    public int getMinValue() {

        if (!hasPower()) {
            return 0;
        }

        return 0;
    }

    @Override
    public int getCurrentValue() {

        /*
         * No FE = no monitor data.
         */
        if (!hasPower()) {
            return 0;
        }

        long total = 0;

        for (ItemStack stack :
                getExposedItems()) {

            if (stack.isEmpty() ||
                    stack.getCount() <= 0) {

                continue;
            }

            total +=
                    stack.getCount();

            if (total >=
                    Integer.MAX_VALUE - 1) {

                return Integer.MAX_VALUE - 1;
            }
        }

        return (int) total;
    }

    @Override
    public int getMaxValue() {

        /*
         * No FE = 0 max.
         */
        if (!hasPower()) {
            return 0;
        }

        return Integer.MAX_VALUE - 1;
    }

    @Override
    public MutableComponent format(
            int value
    ) {

        return Component.literal(
                Integer.toString(value)
        );
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public InventorySummary getRecentNetworkSummary() {

        /*
         * No FE = no Display Link data.
         */
        if (!hasPower()) {
            return InventorySummary.EMPTY;
        }

        if (logisticsBehaviour == null) {
            return InventorySummary.EMPTY;
        }

        return LogisticsManager.getSummaryOfNetwork(
                logisticsBehaviour.freqId,
                false
        );
    }

    public InventorySummary getAccurateNetworkSummary() {

        /*
         * No FE = no Display Link data.
         */
        if (!hasPower()) {
            return InventorySummary.EMPTY;
        }

        if (logisticsBehaviour == null) {
            return InventorySummary.EMPTY;
        }

        return LogisticsManager.getSummaryOfNetwork(
                logisticsBehaviour.freqId,
                true
        );
    }

    private RequestPromiseQueue getPromiseQueue() {

        if (!hasPower()) {
            return null;
        }

        if (logisticsBehaviour == null) {
            return null;
        }

        return Create.LOGISTICS.getQueuedPromises(
                logisticsBehaviour.freqId
        );
    }

    private void addPromisedItem(
            InventorySummary destination,
            RequestPromise promise
    ) {

        if (!hasPower()) {
            return;
        }

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

    private boolean passesFilter(
            RequestPromise promise
    ) {

        if (!hasPower()) {
            return false;
        }

        if (filtering == null) {
            return true;
        }

        ItemStack filterStack =
                filtering.getFilter();

        if (filterStack == null ||
                filterStack.isEmpty()) {

            return true;
        }

        if (filterStack.getItem()
                instanceof PackageFilterItem) {

            String wantedAddress =
                    PackageItem.getAddress(
                            filterStack
                    );

            String promisedAddress =
                    FactoryMonitorPromiseTracker
                            .getAddress(
                                    promise
                            );

            return PackageItem.matchAddress(
                    promisedAddress,
                    wantedAddress
            );
        }

        return filtering.test(
                promise.promisedStack.stack
        );
    }

    private InventorySummary getPromisedNetworkSummary() {

        InventorySummary summary =
                new InventorySummary();

        if (!hasPower()) {
            return summary;
        }

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

    public List<ItemStack> getExposedItems() {

        /*
         * This is the main protection for the virtual inventory.
         */
        if (!hasPower()) {
            return List.of();
        }

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

        if (source == null ||
                source.isEmpty()) {

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

        if (source == null ||
                source.isEmpty()) {

            return;
        }

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
                filterStack.getItem()
                        instanceof PackageFilterItem;
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

        /*
         * Save FE so the monitor doesn't lose its stored power
         * when the chunk unloads.
         */
        tag.putInt(
                "Energy",
                energyStorage.getEnergyStored()
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
                    MonitorMode.valueOf(
                            modeName
                    );

        } catch (IllegalArgumentException exception) {

            mode =
                    MonitorMode.STORED;
        }

        if (mode == null) {
            mode =
                    MonitorMode.STORED;
        }

        energyStorage.setEnergy(
                tag.getInt("Energy")
        );

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
            this.monitor =
                    monitor;
        }

        private void refresh() {

            if (!dirty) {
                return;
            }

            /*
             * No FE = zero virtual inventory.
             */
            if (!monitor.hasPower()) {

                snapshot =
                        List.of();

                dirty =
                        false;

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

    /**
     * Custom EnergyStorage so NBT loading can restore FE directly.
     */
    private static class MonitorEnergyStorage
            extends EnergyStorage {

        private MonitorEnergyStorage(
                int capacity,
                int maxReceive,
                int maxExtract
        ) {
            super(
                    capacity,
                    maxReceive,
                    maxExtract
            );
        }

        @Override
        public int receiveEnergy(
                int toReceive,
                boolean simulate
        ) {

            /*
             * Absolutely refuse FE when require_fe is disabled.
             */
            if (!CreateFactoryMonitorConfig
                    .REQUIRE_FE
                    .get()) {

                return 0;
            }

            return super.receiveEnergy(
                    toReceive,
                    simulate
            );
        }

        @Override
        public int extractEnergy(
                int toExtract,
                boolean simulate
        ) {

            if (!CreateFactoryMonitorConfig
                    .REQUIRE_FE
                    .get()) {

                return 0;
            }

            return super.extractEnergy(
                    toExtract,
                    simulate
            );
        }

        private void setEnergy(int energy) {

            this.energy =
                    Math.max(
                            0,
                            Math.min(
                                    energy,
                                    capacity
                            )
                    );
        }
    }

    private static class MonitorModeScrollValueBehaviour
            extends ScrollValueBehaviour {

        private MonitorModeScrollValueBehaviour(
                FactoryMonitorBlockEntity monitor,
                Direction side
        ) {

            super(
                    Component.translatable(
                            "factorymonitor.mode"
                    ),
                    monitor,
                    new MonitorModeSlot(side)
            );
        }

        @Override
        public ValueSettingsBoard createBoard(
                net.minecraft.world.entity.player.Player player,
                net.minecraft.world.phys.BlockHitResult hitResult
        ) {

            return new ValueSettingsBoard(
                    this.label,
                    MonitorMode.values().length - 1,
                    1,
                    ImmutableList.of(
                            Component.translatable(
                                    "factorymonitor.mode"
                            )
                    ),
                    new ValueSettingsFormatter(
                            this::formatValue
                    )
            );
        }

        public MutableComponent formatValue(
                ValueSettings settings
        ) {

            return Component.translatable(
                    switch (settings.value()) {

                        case 0 ->
                                "factorymonitor.mode.stored";

                        case 1 ->
                                "factorymonitor.mode.promised";

                        case 2 ->
                                "factorymonitor.mode.total";

                        default ->
                                "factorymonitor.mode.stored";
                    }
            );
        }

        private String formatMode(
                int value
        ) {

            return switch (value) {

                case 0 -> "Stored";
                case 1 -> "Promised";
                case 2 -> "Total";
                default -> "Stored";
            };
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