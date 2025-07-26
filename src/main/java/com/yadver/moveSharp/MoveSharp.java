package com.yadver.moveSharp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MoveSharp implements ModInitializer {
    public static final String MOD_ID = "move-sharp";
//    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Identifier CHANNEL_slide = new Identifier(MOD_ID, "slide");


    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL_slide,
                (server, player,
                 serverPlayNetworkHandler, buf,
                 packetSender) ->  {
            if (buf.readBoolean()) {
//                ServerPlayerEntity player = server.getPlayerManager().getPlayer(buf.readUuid());
                if (player != null) player.fallDistance = 0;
            }
        });
//        LOGGER.info("ABOBA");
    }
}