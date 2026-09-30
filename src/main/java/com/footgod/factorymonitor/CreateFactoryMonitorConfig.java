package com.footgod.factorymonitor;

import com.footgod.factorymonitor.compat.OptionalMods;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class CreateFactoryMonitorConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue DYNAMIC_FE;
    public static final ModConfigSpec.BooleanValue REQUIRE_FE;
    public static final ModConfigSpec.IntValue FE_PER_TICK;

    static {
        ModConfigSpec.Builder builder =
                new ModConfigSpec.Builder();

        builder.push("power");

        boolean electricityModDetected =
                OptionalMods.hasCreateElectricityMod();

        DYNAMIC_FE = builder
                .comment(
                        "If true, 'require_fe' will automatically update every boot based on whether Create: New Age or Create Crafts & Additions is installed.",
                        "If false, 'require_fe' will respect manual user changes and never auto-update."
                )
                .define(
                        "dynamic_fe",
                        true
                );

        REQUIRE_FE = builder
                .comment(
                        "If true, Factory Monitor requires FE power.",
                        "Defaults to true when an electricity mod is installed.",
                        "Controlled automatically if 'dynamic_fe' is true."
                )
                .define(
                        "require_fe",
                        electricityModDetected
                );

        FE_PER_TICK = builder
                .comment(
                        "FE consumed per tick while the Factory Monitor is active."
                )
                .defineInRange(
                        "fe_per_tick",
                        20,
                        0,
                        1000000
                );

        builder.pop();

        SPEC = builder.build();
    }

    private CreateFactoryMonitorConfig() {
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.SERVER,
                SPEC
        );
    }

    /**
     * Call this method on ModConfigEvent.Loading and ModConfigEvent.Reloading
     * to handle the dynamic updating behavior.
     */
    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            // Check if dynamic updates are enabled
            if (DYNAMIC_FE.get()) {
                boolean electricityModDetected = OptionalMods.hasCreateElectricityMod();

                // Automatically set require_fe to match the current mod environment
                if (REQUIRE_FE.get() != electricityModDetected) {
                    REQUIRE_FE.set(electricityModDetected);
                }
            }
        }
    }
}