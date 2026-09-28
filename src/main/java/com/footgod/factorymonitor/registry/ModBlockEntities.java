package com.footgod.factorymonitor.registry;

import com.footgod.factorymonitor.CreateFactoryMonitor;
import com.footgod.factorymonitor.block.entity.FactoryMonitorBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>>
            BLOCK_ENTITY_TYPES =
            DeferredRegister.create(
                    net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,
                    CreateFactoryMonitor.MOD_ID
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<FactoryMonitorBlockEntity>
            > FACTORY_MONITOR =
            BLOCK_ENTITY_TYPES.register(
                    "factory_monitor",
                    () -> BlockEntityType.Builder.of(
                            FactoryMonitorBlockEntity::new,
                            ModBlocks.FACTORY_MONITOR.get()
                    ).build(null)
            );

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}