package com.footgod.factorymonitor.compat;

import net.neoforged.fml.ModList;

public final class OptionalMods {

    public static final String NEW_AGE =
            "create_new_age";

    public static final String CRAFTS_AND_ADDITIONS =
            "createaddition";

    private OptionalMods() {
    }

    public static boolean isNewAgeLoaded() {
        return ModList.get().isLoaded(NEW_AGE);
    }

    public static boolean isCraftsAndAdditionsLoaded() {
        return ModList.get().isLoaded(CRAFTS_AND_ADDITIONS);
    }

    public static boolean hasCreateElectricityMod() {
        return isNewAgeLoaded()
                || isCraftsAndAdditionsLoaded();
    }
}