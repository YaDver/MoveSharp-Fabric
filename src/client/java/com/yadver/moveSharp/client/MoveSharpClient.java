package com.yadver.moveSharp.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

public class MoveSharpClient implements ClientModInitializer {
    public static final String MOD_ID = "move-sharp";
    static boolean isClimbing = false;
    static boolean isSliding = false;

    //  True если блок под игроком пустой.
    public static boolean isBlockBelow(BlockView world, Vec3d playerPos) {
        Vec3d _blockPos = playerPos.add(0, -1, 0);
        return world.getBlockState(BlockPos.ofFloored(_blockPos)).isAir() ||
                world.getBlockState(BlockPos.ofFloored(_blockPos)).isReplaceable() ||
                world.getBlockState(BlockPos.ofFloored(_blockPos)).hasSidedTransparency();
    }

    //  True если блок над игроком пустой.
    public static boolean isBlockAbove(BlockView world, Vec3d playerPos) {
        Vec3d _blockPos = playerPos.add(0, 2, 0);
        return !(world.getBlockState(BlockPos.ofFloored(_blockPos)).isAir() ||
               world.getBlockState(BlockPos.ofFloored(_blockPos)).isReplaceable());
    }

    public static boolean isMoveToWall(BlockView world, ClientPlayerEntity player) {
        Vec3d p_look = player.getRotationVector();
        Vec3d p_pos = player.getPos();

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
        if (!isClimbing) {
            switch (blocksFront.toString()) {
                case ("0000") -> {
                    return false;
                }
                default -> {
                    return true;
                }
            }
        } else {
            switch (blocksFront.toString()) {
                case ("0000") -> {
                    if (xORz) {
                        player.setVelocity(0.1*Math.round(p_look.x), player.getVelocity().y, player.getVelocity().z);
                    } else player.setVelocity(player.getVelocity().x, player.getVelocity().y, 0.1*Math.round(p_look.z));
                    isClimbing = false;
                    return false;
                }
                default -> {
                    return true;
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
//                player.sendMessage(Text.literal(player.fallDistance + ""));
                if (Client.options.sprintKey.isPressed() &&
                        player.getRotationVector().y > 0 &&
                        isBlockBelow(world, player.getPos()) &&
//                        isBlockAbove(world, player.getPos()) &&
                        isMoveToWall(world, player)
                        ) {
                    isClimbing = true;
                    player.setVelocity(player.getVelocity().x, 0.15, player.getVelocity().z);
                } else {
                    isClimbing = false;
                }
                if (Client.options.sneakKey.isPressed() &&
//                        player.getRotationVector().y > 0 &&
                        isBlockBelow(world, player.getPos()) &&
//                        isBlockAbove(world, player.getPos()) &&
                        isMoveToWall(world, player)
                ) {
                    player.setVelocity(player.getVelocity().x, player.getVelocity().y/2, player.getVelocity().z);
                    isSliding = true;
                } else isSliding = false;

                ModNetwork.playerSliding(player.getUuid(), isSliding);
//                ModNetwork.playerClimbing(player.getUuid(), isClimbing);
            }
        });

//        ClientPlayNetworking.registerGlobalReceiver(new Identifier(MOD_ID, "velocity"),
//                (Client, clientPlayNetworkHandler,
//                 buf, packetSender) -> {
//                    assert Client.player != null;
//                    Client.player.setVelocity(buf.readVector3f().x, buf.readVector3f().y, buf.readVector3f().z);
//                });
    }
}