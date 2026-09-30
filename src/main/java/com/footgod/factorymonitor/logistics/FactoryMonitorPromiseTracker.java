package com.footgod.factorymonitor.logistics;

import java.util.Map;
import java.util.WeakHashMap;

import com.simibubi.create.content.logistics.packagerLink.RequestPromise;

import net.minecraft.world.item.ItemStack;

public final class FactoryMonitorPromiseTracker {

    private static final Map<RequestPromise, PromiseInfo> PROMISES =
            new WeakHashMap<>();

    private FactoryMonitorPromiseTracker() {
    }

    public static synchronized void track(
            RequestPromise promise,
            String address
    ) {
        if (promise == null) {
            return;
        }

        PROMISES.put(
                promise,
                new PromiseInfo(
                        address == null ? "" : address
                )
        );
    }

    public static synchronized String getAddress(
            RequestPromise promise
    ) {
        PromiseInfo info = PROMISES.get(promise);
        return info == null ? "" : info.address();
    }

    public static synchronized boolean hasAddress(
            RequestPromise promise
    ) {
        return PROMISES.containsKey(promise);
    }

    private record PromiseInfo(String address) {
    }
}