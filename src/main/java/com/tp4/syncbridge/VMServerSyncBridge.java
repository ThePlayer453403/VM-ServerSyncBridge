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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;


public class VMServerSyncBridge implements ModInitializer {
    public static RegistryOps<JsonElement> ops;
    public static JSONObject config;
    public static final Logger logger = LogManager.getLogger();

    @Override
    public void onInitialize() {
        config = Config.loadConfig();

        ServerPlayConnectionEvents.JOIN.register(this::loadInventory);
        ServerPlayConnectionEvents.DISCONNECT.register(this::saveInventory);
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> ops = RegistryOps.of(JsonOps.INSTANCE, server.getRegistryManager()));
    }

    public void loadInventory(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        JSONObject inventory = FileOperation.readJson(String.valueOf(Paths.get(config.getAsString("sync_dir"), "inventory")), String.format("%s.json", handler.player.getUuid()));
        for (int i=0; i<41; i++) {
            handler.player.getInventory().setStack(i, ItemStackEncoding.decodeItemStack(inventory.getAsString(String.valueOf(i)), ops));
        }
        JSONObject enderchest = FileOperation.readJson(String.valueOf(Paths.get(config.getAsString("sync_dir"), "enderchest")), String.format("%s.json", handler.player.getUuid()));
        for (int i=0; i<27; i++) {
            handler.player.getEnderChestInventory().setStack(i, ItemStackEncoding.decodeItemStack(enderchest.getAsString(String.valueOf(i)), ops));
        }
    }

    public void saveInventory(ServerPlayNetworkHandler handler, MinecraftServer server) {
        JSONObject inventory = new JSONObject();
        for (int i=0; i<41; i++) {
            inventory.put(String.valueOf(i), ItemStackEncoding.encodeItemStack(handler.player.getInventory().getStack(i), ops));
        }
        FileOperation.saveJson(String.valueOf(Paths.get(config.getAsString("sync_dir"), "inventory")), String.format("%s.json", handler.player.getUuid()), inventory);
        JSONObject enderchest = new JSONObject();
        for (int i=0; i<27; i++) {
            enderchest.put(String.valueOf(i), ItemStackEncoding.encodeItemStack(handler.player.getEnderChestInventory().getStack(i), ops));
        }
        FileOperation.saveJson(String.valueOf(Paths.get(config.getAsString("sync_dir"), "enderchest")), String.format("%s.json", handler.player.getUuid()), enderchest);
    }
}
