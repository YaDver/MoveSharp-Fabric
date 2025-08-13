package com.yadver.moveSharp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.IndexedIterable;

public class MoveSharp implements ModInitializer {
    public static final String MOD_ID = "move-sharp";

    private static final Identifier CHANNEL_slide = new Identifier(MOD_ID, "slide");
    private static final Identifier CHANNEL_crawl = new Identifier(MOD_ID, "crawl");
    public static final TrackedData<Boolean> IS_CRAWLING = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL_slide,
                (server, player,
                 serverPlayNetworkHandler, buf,
                 packetSender) ->  {
//            ServerPlayerEntity p = server.getPlayerManager().getPlayer(buf.readUuid());
            if (buf.readBoolean() && player != null) player.fallDistance = 0;
        });
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL_crawl,
                (server, player,
                 serverPlayNetworkHandler, buf,
                 packetSender) -> {
//            if (player != null && player.getDataTracker() != null) {
                boolean b = buf.readBoolean();
                player.getDataTracker().set(IS_CRAWLING, b);
//            }
        });
    }
}