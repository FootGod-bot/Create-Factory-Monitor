package com.footgod.factorymonitor.block;

import com.footgod.factorymonitor.block.entity.FactoryMonitorBlockEntity;
import com.footgod.factorymonitor.registry.ModBlockEntities;

import com.mojang.serialization.MapCodec;

import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

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
        return ModBlockEntities.FACTORY_MONITOR.get();
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