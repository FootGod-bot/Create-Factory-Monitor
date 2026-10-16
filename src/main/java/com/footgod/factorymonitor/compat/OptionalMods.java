package com.footgod.factorymonitor.compat;

import net.neoforged.fml.ModList;

public final class OptionalMods {

    public static final String NEW_AGE =
            "create_new_age";

    public static final String CRAFTS_AND_ADDITIONS =
            "createaddition";

    public static final String FLUID_LOGISTICS =
            "fluidlogistics";

    private OptionalMods() {
    }

    public static boolean isNewAgeLoaded() {
        return ModList.get().isLoaded(NEW_AGE);
    }

    public static boolean isCraftsAndAdditionsLoaded() {
        return ModList.get().isLoaded(CRAFTS_AND_ADDITIONS);
    }

    public static boolean isFluidLogisticsLoaded() {return ModList.get().isLoaded(FLUID_LOGISTICS);}

    public static boolean hasCreateElectricityMod() {
        return isNewAgeLoaded()
                || isCraftsAndAdditionsLoaded();
    }
}