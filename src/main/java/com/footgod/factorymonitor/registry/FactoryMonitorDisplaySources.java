package com.footgod.factorymonitor.registry;

import com.footgod.factorymonitor.CreateFactoryMonitor;
import com.footgod.factorymonitor.display.FactoryMonitorDisplaySource;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class FactoryMonitorDisplaySources {

    public static final ResourceLocation
            FACTORY_MONITOR_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CreateFactoryMonitor.MOD_ID,
                    "factory_monitor"
            );

    public static final ResourceLocation
            FACTORY_MONITOR_STACKS_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CreateFactoryMonitor.MOD_ID,
                    "factory_monitor_stacks"
            );

    public static final FactoryMonitorDisplaySource
            FACTORY_MONITOR =
            new FactoryMonitorDisplaySource();

    public static final FactoryMonitorDisplaySource
            FACTORY_MONITOR_STACKS =
            new FactoryMonitorDisplaySource(true);

    private FactoryMonitorDisplaySources() {
    }

    public static void register(
            IEventBus eventBus
    ) {
        eventBus.addListener(
                FactoryMonitorDisplaySources::registerDisplaySources
        );
    }

    private static void registerDisplaySources(
            RegisterEvent event
    ) {

        event.register(
                CreateRegistries.DISPLAY_SOURCE,
                registry -> {

                    registry.register(
                            FACTORY_MONITOR_ID,
                            FACTORY_MONITOR
                    );

                    registry.register(
                            FACTORY_MONITOR_STACKS_ID,
                            FACTORY_MONITOR_STACKS
                    );

                    /*
                     * Make this Display Source available when
                     * a Display Link is connected to the
                     * Factory Monitor block entity.
                     */
                    DisplaySource.BY_BLOCK_ENTITY.add(
                            FactoryMonitorBlockEntities
                                    .FACTORY_MONITOR
                                    .get(),
                            FACTORY_MONITOR
                    );
                    DisplaySource.BY_BLOCK_ENTITY.add(
                            FactoryMonitorBlockEntities
                                    .FACTORY_MONITOR
                                    .get(),
                            FACTORY_MONITOR_STACKS
                    );
                }
        );
    }
}
