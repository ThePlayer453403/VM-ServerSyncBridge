package com.tp4.syncbridge;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minidev.json.JSONObject;

import java.io.*;

public class VMServerSyncBridge implements ModInitializer {
    public static RegistryOps<JsonElement> ops;

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register(this::loadInventory);
        ServerPlayConnectionEvents.DISCONNECT.register(this::saveInventory);
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> ops = RegistryOps.of(JsonOps.INSTANCE, server.getRegistryManager()));
    }

    public void loadInventory(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        JSONObject object = FileOperation.readJson(String.format("E:/%s.json", handler.player.getUuid()));
        for (int i=0; i<41; i++) {
            handler.player.getInventory().setStack(i, ItemStackEncoding.decodeItemStack(object.getAsString(String.valueOf(i)), ops));
        }
    }

    public void saveInventory(ServerPlayNetworkHandler handler, MinecraftServer server) {
        JSONObject object = new JSONObject();
        for (int i=0; i<41; i++) {
            System.out.println(handler.player.getInventory().getStack(i));
            object.put(String.valueOf(i), ItemStackEncoding.encodeItemStack(handler.player.getInventory().getStack(i), ops));
        }
        System.out.println("saved!");
        FileOperation.saveJson(String.format("E:/%s.json", handler.player.getUuid()), object);
    }
}
