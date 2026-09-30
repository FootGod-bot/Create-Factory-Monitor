package com.footgod.factorymonitor.registry;

import com.footgod.factorymonitor.CreateFactoryMonitor;

import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    CreateFactoryMonitor.MOD_ID
            );

    public static final DeferredItem<LogisticallyLinkedBlockItem>
            FACTORY_MONITOR =
            ITEMS.register(
                    "factory_monitor",
                    () -> new LogisticallyLinkedBlockItem(
                            ModBlocks.FACTORY_MONITOR.get(),
                            new net.minecraft.world.item.Item.Properties()
                    )
            );

    private ModItems() {}

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}