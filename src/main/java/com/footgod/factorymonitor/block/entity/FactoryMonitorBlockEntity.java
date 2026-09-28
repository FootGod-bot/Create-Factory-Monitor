package com.footgod.factorymonitor.block.entity;

import java.util.List;

import com.footgod.factorymonitor.registry.ModBlockEntities;

import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class FactoryMonitorBlockEntity extends SmartBlockEntity {

    public enum MonitorMode {
        TOTAL,
        PROMISED,
        COMBINED
    }

    private MonitorMode mode = MonitorMode.TOTAL;

    private long totalCount;
    private long promisedCount;

    private LogisticallyLinkedBehaviour logisticsBehaviour;

    public FactoryMonitorBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.FACTORY_MONITOR.get(),
                pos,
                state
        );

        setLazyTickRate(10);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        logisticsBehaviour =
                new LogisticallyLinkedBehaviour(this, false);

        behaviours.add(logisticsBehaviour);
    }

    public LogisticallyLinkedBehaviour getLogisticsBehaviour() {
        return logisticsBehaviour;
    }

    public MonitorMode getMode() {
        return mode;
    }

    public void setMode(MonitorMode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            setChanged();
        }
    }

    public long getTotalCount() {
        return totalCount;
    }

    public long getPromisedCount() {
        return promisedCount;
    }

    public long getCombinedCount() {
        return totalCount + promisedCount;
    }

    public long getDisplayedCount() {
        return switch (mode) {
            case TOTAL -> totalCount;
            case PROMISED -> promisedCount;
            case COMBINED -> getCombinedCount();
        };
    }

    public void setCounts(
            long totalCount,
            long promisedCount
    ) {
        this.totalCount = Math.max(0, totalCount);
        this.promisedCount = Math.max(0, promisedCount);

        setChanged();
    }

    /**
     * Gets the current Create logistics network summary.
     *
     * The linked frequency comes from the Create
     * LogisticallyLinkedBehaviour.
     */
    public InventorySummary getRecentNetworkSummary() {
        if (logisticsBehaviour == null) {
            return InventorySummary.EMPTY;
        }

        return LogisticsManager.getSummaryOfNetwork(
                logisticsBehaviour.freqId,
                false
        );
    }

    /**
     * Gets a more up-to-date Create logistics network summary.
     */
    public InventorySummary getAccurateNetworkSummary() {
        if (logisticsBehaviour == null) {
            return InventorySummary.EMPTY;
        }

        return LogisticsManager.getSummaryOfNetwork(
                logisticsBehaviour.freqId,
                true
        );
    }

    public void onMonitorUsed(Player player) {
        player.displayClientMessage(
                Component.literal(
                        "Factory Monitor: " + getDisplayedCount()
                ),
                true
        );
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
                mode.name()
        );

        tag.putLong(
                "TotalCount",
                totalCount
        );

        tag.putLong(
                "PromisedCount",
                promisedCount
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

        String modeName = tag.getString("Mode");

        try {
            mode = MonitorMode.valueOf(modeName);
        } catch (IllegalArgumentException exception) {
            mode = MonitorMode.TOTAL;
        }

        totalCount = Math.max(
                0,
                tag.getLong("TotalCount")
        );

        promisedCount = Math.max(
                0,
                tag.getLong("PromisedCount")
        );
    }
}
