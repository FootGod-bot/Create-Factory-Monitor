package com.footgod.factorymonitor;

import com.footgod.factorymonitor.compat.OptionalMods;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class CreateFactoryMonitorConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue REQUIRE_FE;
    public static final ModConfigSpec.IntValue FE_PER_TICK;

    static {
        ModConfigSpec.Builder builder =
                new ModConfigSpec.Builder();

        builder.push("power");

        boolean electricityModDetected =
                OptionalMods.hasCreateElectricityMod();

        REQUIRE_FE = builder
                .comment(
                        "If true, Factory Monitor requires FE power.",
                        "Defaults to true when Create: New Age or Create Crafts & Additions is installed.",
                        "This can be manually changed to false."
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
}