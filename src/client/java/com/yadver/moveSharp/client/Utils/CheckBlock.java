package com.yadver.moveSharp.client.Utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

import static com.yadver.moveSharp.client.MoveSharpClient.isCrawling;

public class CheckBlock {
    private static final RegistryKey<Registry<Block>> registryKey = RegistryKeys.BLOCK;

    public static boolean freeBlock(BlockView world, BlockPos blockPos) {
        BlockState blockState = world.getBlockState(blockPos);

        return (blockState.isAir()
                || blockState.getCollisionShape(world, blockPos).isEmpty()
                || blockState.isIn(TagKey.of(registryKey, Identifier.of(
                "minecraft", "crops")))
                || blockState.isIn(TagKey.of(registryKey, Identifier.of(
                "minecraft", "climbable")))
        );
    }

    //  True если блок под игроком пустой.
    public static boolean freeBelow(BlockView world, Vec3d playerPos) {
        Vec3d _blockPos = playerPos.add(0, -1, 0);
        BlockState blockState = world.getBlockState(BlockPos.ofFloored(_blockPos));
        return (freeBlock(world, BlockPos.ofFloored(_blockPos))
                || blockState.isIn(TagKey.of(registryKey, Identifier.of(
                "minecraft", "trapdoors")))
                || blockState.isIn(TagKey.of(registryKey, Identifier.of(
                "minecraft", "slabs")))
                || blockState.isIn(TagKey.of(registryKey, Identifier.of(
                "minecraft", "fences")))
                || blockState.isIn(TagKey.of(registryKey, Identifier.of(
                "minecraft", "fence_gates")))
                || blockState.isIn(TagKey.of(registryKey, Identifier.of(
                "minecraft", "walls")))
        );
    }

    //  True если блок над игроком пустой.
    public static boolean freeAbove(BlockView world, Vec3d playerPos) {
        Vec3d _blockPos = isCrawling ? playerPos.add(0, 1, 0) : playerPos.add(0, 2, 0);
        BlockState blockState = world.getBlockState(BlockPos.ofFloored(_blockPos));

        return (blockState.isAir()
                || blockState.isIn(TagKey.of(
                RegistryKeys.BLOCK, Identifier.of("minecraft", "trapdoors")))
        );
    }
}
