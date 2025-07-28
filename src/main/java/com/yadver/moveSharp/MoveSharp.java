package com.yadver.moveSharp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class MoveSharp implements ModInitializer {
    public static final String MOD_ID = "move-sharp";
    public static boolean isCrawling = false;

    private static final Identifier CHANNEL_slide = new Identifier(MOD_ID, "slide");
    private static final Identifier CHANNEL_crawl = new Identifier(MOD_ID, "crawl");


    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL_slide,
                (server, player,
                 serverPlayNetworkHandler, buf,
                 packetSender) ->  {
            if (buf.readBoolean() && player != null) player.fallDistance = 0;
        });
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL_crawl,
                (server, player,
                 serverPlayNetworkHandler, buf,
                 packetSender) ->  {
            isCrawling = buf.readBoolean();
        });
    }
}