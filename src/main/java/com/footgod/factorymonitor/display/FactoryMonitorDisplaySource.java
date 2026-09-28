package com.footgod.factorymonitor.display;

import java.util.ArrayList;
import java.util.List;

import com.footgod.factorymonitor.block.entity.FactoryMonitorBlockEntity;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.IItemHandler;

public class FactoryMonitorDisplaySource
        extends DisplaySource {

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

        IItemHandler handler =
                monitor.getItemHandler();

        if (handler == null) {
            return List.of(Component.literal("0"));
        }

        long total = 0;

        for (int slot = 0;
             slot < handler.getSlots();
             slot++) {

            ItemStack stack =
                    handler.getStackInSlot(slot);

            if (stack.isEmpty() ||
                    stack.getCount() <= 0) {
                continue;
            }

            total += stack.getCount();
        }

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
