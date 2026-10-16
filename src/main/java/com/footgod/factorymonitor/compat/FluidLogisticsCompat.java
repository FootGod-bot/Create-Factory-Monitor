package com.footgod.factorymonitor.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.content.logistics.BigItemStack;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import com.yision.fluidlogistics.api.packager.PackageResource;
import com.yision.fluidlogistics.api.packager.PackageResourceType;
import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
import com.yision.fluidlogistics.api.packager.PackageResources;

public final class FluidLogisticsCompat {

    private FluidLogisticsCompat() {
    }

    public static boolean isAvailable() {
        return OptionalMods.isFluidLogisticsLoaded()
                && PackageResources.isBootstrapped();
    }

    public static boolean isFluidResource(ItemStack stack) {
        if (!isAvailable()
                || stack == null
                || stack.isEmpty()) {
            return false;
        }

        Optional<PackageResource> resource =
                PackageResources.readResource(stack);

        return resource.isPresent()
                && PackageResourceTypes.FLUID
                .equals(resource.get().typeId());
    }

    public static Optional<FluidStack> getFluid(ItemStack stack) {
        if (!isFluidResource(stack)) {
            return Optional.empty();
        }

        Optional<PackageResource> resource =
                PackageResources.readResource(stack);

        if (resource.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    PackageResourceTypes.getFluid(
                            resource.get().key()
                    )
            );
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public static int getAmount(ItemStack stack) {
        if (!isFluidResource(stack)) {
            return 0;
        }

        Optional<PackageResource> resource =
                PackageResources.readResource(stack);

        if (resource.isEmpty()) {
            return 0;
        }

        return resource.get().amount();
    }

    public static long getTotalAmount(
            Iterable<BigItemStack> stacks
    ) {
        long total = 0;

        for (BigItemStack entry : stacks) {
            if (entry == null
                    || entry.stack == null
                    || entry.stack.isEmpty()
                    || entry.count <= 0) {
                continue;
            }

            if (!isFluidResource(entry.stack)) {
                continue;
            }

            long amount = getAmount(entry.stack);

            if (amount <= 0) {
                continue;
            }

            long addition =
                    amount * entry.count;

            if (Long.MAX_VALUE - total < addition) {
                return Integer.MAX_VALUE - 1;
            }

            total += addition;

            if (total >= Integer.MAX_VALUE - 1) {
                return Integer.MAX_VALUE - 1;
            }
        }

        return total;
    }

    public static List<ItemStack> createVirtualStacks(
            Iterable<BigItemStack> stacks
    ) {
        List<ItemStack> result =
                new ArrayList<>();

        if (!isAvailable()) {
            return result;
        }

        for (BigItemStack entry : stacks) {
            if (entry == null
                    || entry.stack == null
                    || entry.stack.isEmpty()
                    || entry.count <= 0) {
                continue;
            }

            Optional<PackageResource> resource =
                    PackageResources.readResource(entry.stack);

            if (resource.isEmpty()
                    || !PackageResourceTypes.FLUID
                    .equals(resource.get().typeId())) {
                continue;
            }

            PackageResource fluidResource =
                    resource.get();

            Optional<PackageResourceType> type =
                    PackageResources.findType(entry.stack);

            if (type.isEmpty()) {
                continue;
            }

            ItemStack normalizedKey =
                    fluidResource.key();

            int maxPerPackage =
                    type.get().maxPerPackage(
                            normalizedKey
                    );

            if (maxPerPackage <= 0) {
                continue;
            }

            long totalAmount =
                    (long) fluidResource.amount()
                            * entry.count;

            while (totalAmount > 0) {
                int amount =
                        (int) Math.min(
                                totalAmount,
                                maxPerPackage
                        );

                ItemStack carrier =
                        type.get().createCarrier(
                                normalizedKey,
                                amount
                        );

                if (!carrier.isEmpty()) {
                    carrier.setCount(1);
                    result.add(carrier);
                }

                totalAmount -= amount;
            }
        }

        return result;
    }

    public static boolean isFilledBucket(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return false;
        }

        if (!(stack.getItem() instanceof BucketItem)) {
            return false;
        }

        return !getBucketFluid(stack).isEmpty();
    }

    public static FluidStack getBucketFluid(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return FluidStack.EMPTY;
        }

        if (!(stack.getItem() instanceof BucketItem)) {
            return FluidStack.EMPTY;
        }

        return FluidUtil
                .getFluidContained(stack)
                .orElse(FluidStack.EMPTY);
    }

    public static boolean matchesBucket(
            ItemStack resource,
            ItemStack bucket
    ) {
        if (!isFluidResource(resource)
                || !isFilledBucket(bucket)) {
            return false;
        }

        Optional<FluidStack> resourceFluid =
                getFluid(resource);

        if (resourceFluid.isEmpty()) {
            return false;
        }

        FluidStack bucketFluid =
                getBucketFluid(bucket);

        if (bucketFluid.isEmpty()) {
            return false;
        }

        return FluidStack.isSameFluid(
                resourceFluid.get(),
                bucketFluid
        );
    }
}