package com.footgod.factorymonitor.registry;

import com.footgod.factorymonitor.CreateFactoryMonitor;
import com.footgod.factorymonitor.block.FactoryMonitorBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(
                    CreateFactoryMonitor.MOD_ID
            );

    public static final DeferredBlock<FactoryMonitorBlock>
            FACTORY_MONITOR =
            BLOCKS.registerBlock(
                    "factory_monitor",
                    FactoryMonitorBlock::new,
                    BlockBehaviour.Properties.of()
                            .destroyTime(2.0F)
                            .explosionResistance(6.0F)
                            .sound(SoundType.METAL)
            );

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}