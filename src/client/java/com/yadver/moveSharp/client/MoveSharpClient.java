package com.yadver.moveSharp.client;

import com.yadver.moveSharp.client.Utils.SmoothAcceleration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

public class MoveSharpClient implements ClientModInitializer {
    public static final String MOD_ID = "move-sharp";

    static boolean isClimbing = false;
    static boolean isSliding = false;
    public static boolean isCrawling = false;
    static boolean canClimbing = true;
    static boolean canSliding = true;

    private static double startPos;
    SmoothAcceleration smoothClimb;
    SmoothAcceleration smoothSlide;

    static double climbingSpeed = 0.15;
    static double slidingSpeed = -0.05;

    public static boolean checkBlock(BlockView world, BlockPos blockPos) {
        BlockState blockState = world.getBlockState(blockPos);
        return (blockState.isAir()
                || blockState.isReplaceable()
                || (blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                        "minecraft", "flowers")))
                    && !blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                            "minecraft", "leaves"))))
                || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                        "minecraft", "crops")))
                || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                        "minecraft", "climbable")))
                || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                        "minecraft", "buttons")))
                || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                        "minecraft", "wall_post_override")))
        );
    }

    //  True если блок под игроком пустой.
    public static boolean freeBelow(BlockView world, Vec3d playerPos) {
        Vec3d _blockPos = playerPos.add(0, -1, 0);
        BlockState blockState = world.getBlockState(BlockPos.ofFloored(_blockPos));
        return (checkBlock(world, BlockPos.ofFloored(_blockPos))
                || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                        "minecraft", "trapdoors")))
                || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                        "minecraft", "slabs")))
                || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                        "minecraft", "fences")))
                || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                        "minecraft", "fence_gates")))
                || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                        "minecraft", "walls")))
        );
    }

    //  True если блок над игроком пустой.
    public static boolean freeAbove(BlockView world, Vec3d playerPos) {
        Vec3d _blockPos;
        if (isCrawling) _blockPos = playerPos.add(0, 1, 0);
        else _blockPos = playerPos.add(0, 2, 0);
        BlockState blockState = world.getBlockState(BlockPos.ofFloored(_blockPos));

        return (blockState.isAir()
                || blockState.isIn(TagKey.of(
                RegistryKeys.BLOCK, Identifier.of("minecraft", "trapdoors")))
        );
    }

    //  Получаем boolean, когда персонаж стоит у стены и смотрит на неё.
    //  Работа описана внутри метода.
    public static boolean isOnWall(BlockView world, ClientPlayerEntity player, boolean isClimbRequest , boolean nado) {
        Vec3d p_look = player.getRotationVector();
        Vec3d p_pos = player.getPos();
        Vec3d p_vel = player.getVelocity();

        //  Получаем позицию блока перед игроком за счёт смещения позиции игрока на один блок в сторону направления взгляда.
        boolean xORz = Math.abs(p_look.x) > Math.abs(p_look.z);
        BlockPos _blockPos;
        Vec3d p_look_normal = new Vec3d(
                (xORz) ? ((p_look.x < 0) ? -Math.ceil(-p_look.x) : Math.ceil(p_look.x)) : 0,
                0,
                (xORz) ? 0 : ((p_look.z < 0) ? -Math.ceil(-p_look.z) : Math.ceil(p_look.z))
        );

        //  Делим на два, чтобы карабканье работало практически в упоре к стене.
        if (isClimbRequest) _blockPos = BlockPos.ofFloored(p_pos.add(
                p_look_normal.x * 0.5, 0, p_look_normal.z * 0.5));
        //  Не делим на два, чтобы скольжение работало не в упоре к стене.
        else _blockPos = BlockPos.ofFloored(p_pos.add(
                p_look_normal.x, 0, p_look_normal.z));

        StringBuilder blocksFront = new StringBuilder();
        StringBuilder blocksAbove = new StringBuilder();

        //  Проверяем 4 блока перед персонажем на уровне ног и выше и создаём схему вида 0110, где 0 - блока нет, 1 - есть.
        //  Возможное изменение: вместо методов freeBelow() и freeAbove() добавить в схему два новых значения по краям,
        //  тогда 100001 говорит о том, что перед игроком нет блоков на высоте 4-х блоков, но под ним и над ним есть блоки.
        for (int b = 0; b<=3; b++) {
            if (checkBlock(world, _blockPos.add(0, b, 0))) {
                blocksFront.append("0");
            } else blocksFront.append("1");
        }

        //  Проверяем 4 блока перед персонажем на уровне ног и выше и создаём схему вида 0110, где 0 - блока нет, 1 - есть.
        //  Возможное изменение: вместо методов freeBelow() и freeAbove() добавить в схему два новых значения по краям,
        //  тогда 100001 говорит о том, что перед игроком нет блоков на высоте 4-х блоков, но под ним и над ним есть блоки.
        for (int b = 0; b<=3; b++) {
            if (checkBlock(world, BlockPos.ofFloored(p_pos.add(0, b, 0)))) {
                blocksAbove.append("0");
            } else blocksAbove.append("1");
        }

        //  Проверяем все варианты, при которых игрок может карабкаться или скользить.
        //  isClimbRequest необходим, так как этот метод вызывают как карабканье, так и скольжение.
        //  (Если игрок скользит по стене, то это считается за условие !isClimbing, и работает та логика метода, которая
        //  ограничивает условия, когда может позволить игроку начать карабкаться. А эти ограничения не должны работать
        //  на механику скольжения)
        if (isClimbRequest) {
            BlockPos blockPos = BlockPos.ofFloored(p_pos.add(
                    p_look_normal.x * 0.5,-0.5,p_look_normal.z * 0.5));
            BlockState blockState = world.getBlockState(blockPos);

            if (blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                    "minecraft", "walls")))
                    || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                    "minecraft", "fences")))
                    || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                    "minecraft", "fence_gates")))
//                                    || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
//                                    "minecraft", "slabs")))
            ) return true;

            if (!isClimbing) {
                boolean checkAbove = true;
                for(int i = 0; i < 4; i++) {
                    if(blocksFront.toString().charAt(i) == '0') {
                        if(checkAbove && blocksAbove.toString().charAt(i) == '1') return false;
                        else checkAbove = false;
                        startPos = _blockPos.getY() + i;
                    }
                }
                switch (blocksFront.toString()) {
                    //  Ограничивает карабканье, если перед игроком блоков нет или 5-й блок (при прыжке
                    //  становиться 4-м) не позволит залезть на стену из-за лимита во максимальной высоте карабканья.
                    case ("0000"), ("0001"), ("0011"), ("0111"), ("1111") -> {
                        return false;
                    }
                    default -> {
                        return true;
                    }
                }
            } else {
                switch (blocksFront.toString()) {
                    case ("0000"), ("0011"), ("0010"), ("0001"),
                         ("0111"), ("0101"), ("0110"), ("0100"),
                         ("1000"), ("1011"), ("1001"), ("1010") -> {
                        if (!isCrawling
                                && (blocksFront.toString().startsWith("10")
                                && !freeAbove(world, p_pos))
                        ) {
                            isCrawling = true;
                            ModNetwork.playerCrawling(true);
                            return true;

                        }
                        else if (!isCrawling
                                && blocksFront.toString().startsWith("01")) {
                            if (startPos != _blockPos.getY() && !nado && freeAbove(world, p_pos)) return true;

                            isCrawling = true;
                            ModNetwork.playerCrawling(true);
                            return true;
                        }

                        if (blocksFront.toString().startsWith("0")
                                || (blocksFront.toString().startsWith("1")
                                && (blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                                        "minecraft", "slabs")))
                                || blockState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of(
                                        "minecraft", "trapdoors"))))
                                && (blockPos.getY() + blockState.getCollisionShape(world, blockPos)
                                .getBoundingBox().maxY - p_pos.y) <= 0
                        )) {
                            if (xORz) {
                                player.setVelocity(0.1 * p_look_normal.x, p_vel.y, p_vel.z);
                            } else player.setVelocity(p_vel.x, p_vel.y, 0.1 * p_look_normal.z);
                            return false;
                        }
                        return true;
                    }
                    default -> {
                        return true;
                    }
                }
            }
        } else {
            switch (blocksFront.toString()) {
                case ("1000"), ("1100"), ("1110"), ("1101"), ("1111") -> {
                    if (isSliding) return !(startPos - player.getPos().y > 3);
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

                if (Client.options.sprintKey.isPressed()) {

                    //  Climbing
                    if (canClimbing
                            && !isSliding
                            && !player.isOnGround()
                            && freeBelow(world, player.getPos())
                            && isOnWall(world, player, true, Client.options.sneakKey.isPressed())
                        ) {
                        if (!isClimbing){
                            if (vel.y > 0) {
                                smoothClimb = new SmoothAcceleration(vel.y, climbingSpeed/2 , 0.1);
                            } else smoothClimb = new SmoothAcceleration(0, climbingSpeed, 0.1);
                        }
                        isClimbing = isClimbing || !isCrawling;
                        if (isClimbing) player.setVelocity(vel.x/2, smoothClimb.update() , vel.z/2);
                    } else {
                        if (isClimbing) {
                            canClimbing = false;
                            smoothClimb.restore();
                            isClimbing = false;
                        }
                    }

                    //  Crawling
                    if (Client.options.sneakKey.isPressed()
                            && !isSliding
                            && !isClimbing
                            && player.isOnGround()
                    ) isCrawling = true;
                    if (player.isCrawling()) isCrawling = true;
                } else isCrawling = isCrawling && !freeAbove(world, player.getPos());

                //  Sliding
                if (Client.options.sneakKey.isPressed()
                        && canSliding
                        && !isCrawling
                        && !isClimbing
                        && freeBelow(world, player.getPos())
                        && isOnWall(world, player, false, false)
                ) {
                    if (!isSliding) {
                        startPos = player.getPos().y;
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

                //  Если игрок падает больше 5 блоков, то он не сможет зацепиться для карабканья или скольжения.
                if (player.fallDistance >= 10) {
                    canClimbing = false;
                    canSliding = false;
                }

                if (player.isOnGround()) {
                    canClimbing = true;
                    canSliding = true;
                }

                ModNetwork.playerSliding(isSliding);
                ModNetwork.playerCrawling(isCrawling);
            }
        });
    }
}