package com.footgod.factorymonitor;

import com.footgod.factorymonitor.registry.ModBlockEntities;

import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(
        value = CreateFactoryMonitor.MOD_ID,
        dist = Dist.CLIENT
)
public class CreateFactoryMonitorClient {

    public CreateFactoryMonitorClient(IEventBus modEventBus) {

        modEventBus.addListener(
                this::registerRenderers
        );
    }

    private void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {

        event.registerBlockEntityRenderer(
                ModBlockEntities.FACTORY_MONITOR.get(),
                SmartBlockEntityRenderer::new
        );
    }
}