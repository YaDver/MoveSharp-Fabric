package com.yadver.moveSharp.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class ModNetwork {
    private static final Identifier CHANNEL_slide = new Identifier(MoveSharpClient.MOD_ID, "slide");

    public static void playerSliding(UUID uuid, boolean isSliding) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isSliding);
        buf.writeUuid(uuid);
        ClientPlayNetworking.send(CHANNEL_slide, buf);
    }
}
