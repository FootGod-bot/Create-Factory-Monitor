package com.footgod.factorymonitor;

import com.footgod.factorymonitor.compat.OptionalMods;
import com.footgod.factorymonitor.registry.ModBlockEntities;
import com.footgod.factorymonitor.registry.ModBlocks;
import com.footgod.factorymonitor.registry.ModDisplaySources;
import com.footgod.factorymonitor.registry.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CreateFactoryMonitor.MOD_ID)
public class CreateFactoryMonitor {

    public static final String MOD_ID =
            "factorymonitor";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    private static final ResourceKey<
            net.minecraft.world.item.CreativeModeTab
            > CREATE_BASE_TAB =
            ResourceKey.create(
                    Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath(
                            "create",
                            "base"
                    )
            );

    public CreateFactoryMonitor(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {

        ModBlocks.register(modEventBus);

        ModItems.register(modEventBus);

        ModBlockEntities.register(modEventBus);

        ModDisplaySources.register(
                modEventBus
        );

        modEventBus.addListener(
                this::addCreativeItems
        );

        modEventBus.addListener(
                this::registerCapabilities
        );

        // Register the config and wire up the loading event listener
        CreateFactoryMonitorConfig.register(
                modContainer
        );
        modEventBus.addListener(
                CreateFactoryMonitorConfig::onConfigLoad
        );

        boolean newAge =
                OptionalMods.isNewAgeLoaded();

        boolean craftsAndAdditions =
                OptionalMods.isCraftsAndAdditionsLoaded();

        LOGGER.info(
                "Create Factory Monitor loaded."
        );

        LOGGER.info(
                "Create: New Age detected: {}",
                newAge
        );

        LOGGER.info(
                "Create Crafts & Additions detected: {}",
                craftsAndAdditions
        );

        LOGGER.info(
                "Create electricity mod detected: {}",
                newAge || craftsAndAdditions
        );

        LOGGER.info(
                "Factory Monitor default require_fe: {}",
                CreateFactoryMonitorConfig
                        .REQUIRE_FE
                        .getDefault()
        );
    }

    private void registerCapabilities(
            RegisterCapabilitiesEvent event
    ) {

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.FACTORY_MONITOR.get(),
                (monitor, side) ->
                        monitor.getItemHandler()
        );
    }

    private void addCreativeItems(
            BuildCreativeModeTabContentsEvent event
    ) {

        if (!event.getTabKey()
                .equals(CREATE_BASE_TAB)) {
            return;
        }

        event.accept(
                ModItems.FACTORY_MONITOR.get()
        );
    }
}