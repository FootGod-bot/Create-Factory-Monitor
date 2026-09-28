package com.footgod.factorymonitor;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(CreateFactoryMonitor.MODID)
public class CreateFactoryMonitor {
    public static final String MODID = "factorymonitor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateFactoryMonitor(IEventBus modEventBus, ModContainer modContainer) {
    }
}
