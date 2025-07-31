package com.yadver.moveSharp.client;

import com.yadver.moveSharp.client.Utils.SmoothAcceleration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

public class MoveSharpClient implements ClientModInitializer {
    public static final String MOD_ID = "move-sharp";
//    private static Logger LOGGER = new Logger();

    static boolean isClimbing = false;
    static boolean isSliding = false;
    public static boolean isCrawling = false;
    static boolean canClimbing = true;
    static boolean canSliding = true;

    //  Если true - увеличивает максимальную высоту для карабканья с 3-х до 4-х блоков.
    static boolean fourBlockClimbMax = true;

    private static double startSlidingPos;
    SmoothAcceleration smoothClimb;
    SmoothAcceleration smoothSlide;

    static double climbingSpeed = 0.15;
    static double slidingSpeed = -0.05;

    //  True если блок под игроком пустой.
    public static boolean freeBelow(BlockView world, Vec3d playerPos) {
        Vec3d _blockPos = playerPos.add(0, -1, 0);
        BlockState blockState = world.getBlockState(BlockPos.ofFloored(_blockPos));
        //  isAir() - Если блок является воздухом.
        //  isReplaceable() - Если блок является заменяемым, например травой, снегом или чем-то подобным.
        //  Посаженые растения не являются ни тем, ни другим. Нужно найти для этого решение.
        return (blockState.isAir() || blockState.isReplaceable());
    }

    //  True если блок над игроком пустой.
    public static boolean freeAbove(BlockView world, Vec3d playerPos) {
        Vec3d _blockPos = isCrawling ? playerPos.add(0, 1, 0) : playerPos.add(0, 2, 0);
        BlockState blockState = world.getBlockState(BlockPos.ofFloored(_blockPos));
        //  isAir() - Если блок является воздухом.
        //  isReplaceable() - Если блок является заменяемым, например травой, снегом или чем-то подобным.
        //  Так как блок над игроком в теории не может быть растением или слоём снега в isReplaceable() нет нужды.
        //  Но так как это работает, я трогать не буду. Вдруг есть блоки, которые считаются заменяемыми, через них можно
        //  проходить, и тогда этот метод будет необходим (возможно в других модах или в майнкрафт есть подобные).
        return (blockState.isAir() || blockState.isReplaceable());
    }

    //  Получаем boolean, когда персонаж стоит у стены и смотрит на неё.
    //  Работа описана внутри метода.
    public static boolean isOnWall(BlockView world, ClientPlayerEntity player, boolean isClimbRequest) {
        Vec3d p_look = player.getRotationVector();
        Vec3d p_pos = player.getPos();
        Vec3d p_vel = player.getVelocity();

        //  Получаем позицию блока перед игроком за счёт смещения позиции игрока на один блок в сторону направления взгляда.
        boolean xORz = Math.abs(p_look.x) > Math.abs(p_look.z);
        Vec3d _blockPos = xORz
                //  делим на два, чтобы карабканье и скольжение работали практически в упоре к стене.
                ? p_pos.add(p_look.x/2, 0, 0) : p_pos.add(0, 0, p_look.z/2);
        StringBuilder blocksFront = new StringBuilder();

        //  Проверяем 4 блока перед персонажем на уровне ног и выше и создаём схему вида 0110, где 0 - блока нет, 1 - есть.
        //  Возможное изменение: вместо методов freeBelow() и freeAbove() добавить в схему два новых значения по краям,
        //  тогда 100001 говорит о том, что перед игроком нет блоков на высоте 4-х блоков, но под ним и над ним есть блоки.
        for (int b = 0; b<=3; b++) {
            if (world.getBlockState(BlockPos.ofFloored(_blockPos.add(0, b, 0))).isAir() ||
                    world.getBlockState(BlockPos.ofFloored(_blockPos.add(0, b, 0))).isReplaceable()) {
                blocksFront.append("0");
            } else blocksFront.append("1");
        }

        //  Проверяем все варианты, при которых игрок может карабкаться или скользить.
        //  isClimbRequest необходим, так как этот метод вызывают как карабканье, так и скольжение.
        //  (Если игрок скользит по стене, то это считается за условие !isClimbing, и работает та логика метода, которая
        //  ограничивает условия, когда может позволить игроку начать карабкаться. А эти ограничения не должны работать
        //  на механику скольжения)
        if (isClimbRequest) {
            if (!isClimbing) {
                switch (blocksFront.toString()) {
                    //  Ограничивает карабканье, если перед игроком блоков нет или 5-й блок (при прыжке
                    //  становиться 4-м) не позволит залезть на стену из-за лимита во максимальной высоте карабканья.
                    case ("0000"), ("0001"), ("0011"), ("0111"), ("1111") -> {
                        return false;
                    }
                    //  Ограничение на максимальную высоту карабканья (от земли) в 4 блока можно установить, если
                    //  параметр fourBlockClimbMax будет true, иначе ограничение будет в 3 блока
                    case ("1110"), ("0110"), ("0010") -> {
                        return fourBlockClimbMax;
                    }
                    case ("0101"), ("0100") -> {
                        return freeAbove(world, p_pos);
                    }
                    default -> {
                        return true;
                    }
                }
            } else {
                switch (blocksFront.toString()) {
                    case ("0000"), ("0011"), ("0010"), ("0001"), ("0111"), ("0101"), ("0110"), ("0100"),
                         ("1000"), ("1011"), ("1001"), ("1010")   -> {
                        if (!isCrawling && (blocksFront.toString().startsWith("01") ||
                                (blocksFront.toString().startsWith("00") && !freeAbove(world, p_pos)) ||
                                (blocksFront.toString().startsWith("10") && !freeAbove(world, p_pos)))) {
                            player.sendMessage(Text.literal("asd"));

                            isCrawling = true;
                            ModNetwork.playerCrawling(true);
                            return true;
                        }
                        if (blocksFront.toString().startsWith("10")) return true;
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
                case ("1000"), ("1100"), ("1110"), ("1101"), ("1111") -> {
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

                if (Client.options.sprintKey.isPressed()) {

                    //  Climbing
                    if (canClimbing &&
                            !isSliding &&
                            freeBelow(world, player.getPos()) &&
                            isOnWall(world, player, true)
                        ) {
                        if (!isClimbing){
                            if (vel.y > 0) {
                                smoothClimb = new SmoothAcceleration(vel.y, climbingSpeed, 0.1);
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
                    if (Client.options.sneakKey.isPressed() &&
                            !isSliding &&
                            !isClimbing &&
                            player.isOnGround()
                    ) isCrawling = true;
                    if (player.isCrawling()) isCrawling = true;
                } else isCrawling = isCrawling && !freeAbove(world, player.getPos());

                //  Sliding
                if (Client.options.sneakKey.isPressed() &&
                        canSliding &&
                        !isCrawling &&
                        !isClimbing &&
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

                //  Crawling
//                if (Client.options.sprintKey.isPressed()) {
//                    if (Client.options.sneakKey.isPressed() &&
//                            !isSliding &&
//                            !isClimbing &&
//                            player.isOnGround()
//                    ) isCrawling = true;
//                    if (player.isCrawling()) isCrawling = true;
//                } else isCrawling = false;

                //  Если игрок падает больше 5 блоков, то он не сможет зацепиться для карабканья или скольжения.
                if (player.fallDistance >= 5) {
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