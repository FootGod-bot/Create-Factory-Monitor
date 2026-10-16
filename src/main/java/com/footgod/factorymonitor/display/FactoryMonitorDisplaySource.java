package com.footgod.factorymonitor.display;

import java.util.List;

import com.footgod.factorymonitor.block.entity.FactoryMonitorBlockEntity;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
public class FactoryMonitorDisplaySource
        extends DisplaySource {

    private final boolean countStacks;

    public FactoryMonitorDisplaySource() {
        this(false);
    }

    public FactoryMonitorDisplaySource(boolean countStacks) {
        this.countStacks = countStacks;
    }

    @Override
    public List<MutableComponent> provideText(
            DisplayLinkContext context,
            DisplayTargetStats stats
    ) {

        if (context == null) {
            return List.of(Component.literal("0"));
        }

        if (!(context.getSourceBlockEntity()
                instanceof FactoryMonitorBlockEntity monitor)) {
            return List.of(Component.literal("0"));
        }

        long total = monitor.getDisplayCount(countStacks);

        return List.of(
                Component.literal(
                        Long.toString(total)
                )
        );
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 10;
    }

    @Override
    public boolean shouldPassiveReset() {
        return true;
    }
}
