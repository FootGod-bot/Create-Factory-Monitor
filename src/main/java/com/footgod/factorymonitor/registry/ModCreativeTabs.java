package com.footgod.factorymonitor.registry;

import com.footgod.factorymonitor.CreateFactoryMonitor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    CreateFactoryMonitor.MOD_ID
            );

    /*
     * Intentionally empty.
     *
     * Factory Monitor will be inserted into an existing Create tab
     * through the Create/creative-tab integration rather than creating
     * a one-item Factory Monitor tab.
     */

    private ModCreativeTabs() {
    }

    public static void register() {
        TABS.register((IEventBus) Registries.CREATIVE_MODE_TAB);
    }
}