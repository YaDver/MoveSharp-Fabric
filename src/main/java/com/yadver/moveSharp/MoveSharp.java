package com.yadver.moveSharp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Objects;


public class MoveSharp implements ModInitializer {
    public static final String MOD_ID = "move-sharp";

    private static final Identifier CHANNEL_slide = new Identifier(MOD_ID, "slide");


    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL_slide,
                (server, player,
                 serverPlayNetworkHandler, buf,
                 packetSender) ->  {
            if (buf.readBoolean() && player != null) player.fallDistance = 0;
        });
    }
}