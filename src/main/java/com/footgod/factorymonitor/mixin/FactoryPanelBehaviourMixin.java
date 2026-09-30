package com.footgod.factorymonitor.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.footgod.factorymonitor.logistics.FactoryMonitorPromiseTracker;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.packagerLink.RequestPromise;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;

@Mixin(FactoryPanelBehaviour.class)
public abstract class FactoryPanelBehaviourMixin {

    @Shadow
    private UUID network;

    @Shadow
    private String recipeAddress;

    @ModifyArg(
            method = "tickRequests",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/simibubi/create/content/logistics/packagerLink/RequestPromiseQueue;" +
                                    "add(Lcom/simibubi/create/content/logistics/packagerLink/RequestPromise;)V"
            ),
            index = 0
    )
    private RequestPromise factoryMonitor$trackPromise(
            RequestPromise promise
    ) {
        FactoryMonitorPromiseTracker.track(
                promise,
                recipeAddress
        );

        return promise;
    }
}