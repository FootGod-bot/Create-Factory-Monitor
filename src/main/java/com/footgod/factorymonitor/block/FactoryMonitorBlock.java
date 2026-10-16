package com.footgod.factorymonitor.block;

import com.footgod.factorymonitor.block.entity.FactoryMonitorBlockEntity;
import com.footgod.factorymonitor.registry.FactoryMonitorBlockEntities;

import com.mojang.serialization.MapCodec;

import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class FactoryMonitorBlock
        extends BaseEntityBlock
        implements IBE<FactoryMonitorBlockEntity> {

    public static final MapCodec<FactoryMonitorBlock> CODEC =
            simpleCodec(
                    FactoryMonitorBlock::new
            );

    public FactoryMonitorBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public Class<FactoryMonitorBlockEntity> getBlockEntityClass() {
        return FactoryMonitorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FactoryMonitorBlockEntity>
    getBlockEntityType() {
        return FactoryMonitorBlockEntities.FACTORY_MONITOR.get();
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return IBE.super.newBlockEntity(
                pos,
                state
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {

        boolean sneaking =
                player.isShiftKeyDown();

        boolean wrench =
                stack.getItem() instanceof WrenchItem;

        if (sneaking || wrench) {

            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (blockEntity
                    instanceof FactoryMonitorBlockEntity monitor) {

                /*
                 * Only the server changes the actual mode.
                 *
                 * The server also sends the action-bar message
                 * to the player who performed the interaction.
                 */
                if (!level.isClientSide &&
                        player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {

                    monitor.toggleResourceMode(
                            serverPlayer
                    );
                }

                return ItemInteractionResult.sidedSuccess(
                        level.isClientSide
                );
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {
        IBE.onRemove(
                state,
                level,
                pos,
                newState
        );
    }
}