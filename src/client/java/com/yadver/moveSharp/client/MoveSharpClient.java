package com.yadver.moveSharp.client;

import com.yadver.moveSharp.client.Utils.SmoothAcceleration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

import java.util.Objects;

public class MoveSharpClient implements ClientModInitializer {
    public static final String MOD_ID = "move-sharp";

    static boolean isClimbing = false;
    static boolean isSliding = false;
    static boolean canClimbing = true;
    static boolean canSliding = true;

    private static double startSlidingPos;
    SmoothAcceleration smoothClimb;
    SmoothAcceleration smoothSlide;

    static double climbingSpeed = 0.15;
    static double slidingSpeed = -0.05;

    //  True если блок под игроком пустой.
    public static boolean freeBelow(BlockView world, Vec3d playerPos) {
        Vec3d _blockPos = playerPos.add(0, -1, 0);
        BlockState blockState = world.getBlockState(BlockPos.ofFloored(_blockPos));
        return (blockState.isAir() || blockState.isReplaceable());
    }

    //  True если блок над игроком пустой.
    public static boolean freeAbove(BlockView world, Vec3d playerPos) {
        Vec3d _blockPos = playerPos.add(0, 2, 0);
        BlockState blockState = world.getBlockState(BlockPos.ofFloored(_blockPos));
        return (blockState.isAir() || blockState.isReplaceable());
    }

    public static boolean isOnWall(BlockView world, ClientPlayerEntity player, boolean isClimbRequest) {
        Vec3d p_look = player.getRotationVector();
        Vec3d p_pos = player.getPos();
        Vec3d p_vel = player.getVelocity();

        //  Получаем позицию блока перед игроком засчёт смещения позиции игрока на один блок в сторону направления взгляда.
        boolean xORz = Math.abs(p_look.x) > Math.abs(p_look.z);
        Vec3d _blockPos = xORz
                ? p_pos.add(p_look.x, 0, 0) : p_pos.add(0, 0, p_look.z);
        StringBuilder blocksFront = new StringBuilder();

        //  Проверяем 4 блока перед персонажем на уровне ног и выше и создаём схему вида 0110, где 0 - блока нет, 1 - есть.
        for (int b = 0; b<=3; b++) {
            if (world.getBlockState(BlockPos.ofFloored(_blockPos.add(0, b, 0))).isAir() ||
                    world.getBlockState(BlockPos.ofFloored(_blockPos.add(0, b, 0))).isReplaceable()) {
                blocksFront.append("0");
            } else blocksFront.append("1");
        }

        //  Проверяем все варианты, при которых игрок может карабкаться. Важно учитывать, что 1000 не являеться возможным
        //  для карабканья (легче просто запрыгнуть на один блок), но оно должно учитываться, если игрок уже карабкается,
        //  иначе он не сможет забратся на блок, над котором 3 и более пустых блока. Для этого следует добавить переменную
        //  или метод isClimbing.
        if (isClimbRequest) {
            if (!isClimbing) {
                switch (blocksFront.toString()) {
                    case ("0000"), ("1110"), ("0001"), ("0011"), ("0111"), ("1111") -> {
                        return false;
                    }
                    case ("0101"), ("0110") -> {
                        return freeAbove(world, p_pos);
                    }
                    default -> {
                        return true;
                    }
                }
            } else {
                switch (blocksFront.toString()) {
                    case ("0000"), ("0011"), ("0010"), ("0001") -> {
                        if (xORz) {
                            player.setVelocity(0.1 * Math.round(p_look.x), p_vel.y, p_vel.z);
                        } else player.setVelocity(p_vel.x, p_vel.y, 0.1 * Math.round(p_look.z));

                        return false;
                    }
                    default -> {
                        return true;
                    }
                }
            }
        } else {
            switch (blocksFront.toString()) {
                case ("1000"), ("1100"), ("1110"), ("1111") -> {
                    if (isSliding) return !(startSlidingPos - player.getPos().y > 3);
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(Client -> {
            ClientPlayerEntity player = Client.player;
            ClientWorld world = Client.world;

            if (player != null && world != null) {
                Vec3d vel = player.getVelocity();

                //  Climbing
                if (Client.options.sprintKey.isPressed() &&
                        canClimbing &&
                        freeBelow(world, player.getPos()) &&
                        isOnWall(world, player, true)
                        ) {
                    if (!isClimbing){
                        if (vel.y > 0) {
                            smoothClimb = new SmoothAcceleration(vel.y, climbingSpeed, 0.1);
                        } else smoothClimb = new SmoothAcceleration(0, climbingSpeed, 0.1);
                    }
                    isClimbing = true;
                    player.setVelocity(vel.x, smoothClimb.update() , vel.z);
                } else {
                    if (isClimbing) {
                        canClimbing = false;
                        smoothClimb.restore();
                        isClimbing = false;
                    }
                }

                //  Sliding
                if (Client.options.sneakKey.isPressed() &&
                        canSliding &&
                        freeBelow(world, player.getPos()) &&
                        isOnWall(world, player, false)
                ) {
                    if (!isSliding) {
                        startSlidingPos = player.getPos().y;
                        smoothSlide = new SmoothAcceleration(vel.y, slidingSpeed, 0.1);
                    }

                    player.setVelocity(vel.x/2, smoothSlide.update(), vel.z/2);

                    isSliding = true;
                } else {
                    if (isSliding) {
                        canSliding = false;
                        isSliding = false;
                    }
                    if (smoothSlide != null) smoothSlide.restore();
                }

                if (player.isOnGround()) {
                    canClimbing = true;
                    canSliding = true;
                }
                ModNetwork.playerSliding(isSliding);
            }
        });
    }
}