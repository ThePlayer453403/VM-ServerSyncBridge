package com.tp4.syncbridge;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

//TODO: 在“未知物品”的custom_data中移除对count的记录，以保证同ID物品可以正常堆叠
//TODO: 添加可以自定义的json保存地点
//TODO: 为特殊玩家禁用同步
//TODO: 同步更多信息（如：成就）
//TODO: 附魔兼容（666只要有附魔就消失）

//TODO: 拦截未知数据包

public class VMServerSyncBridge implements ModInitializer {
    public static List<String> MOD_LIST;
    final Base64.Encoder encoder = Base64.getEncoder();
    final Base64.Decoder decoder = Base64.getDecoder();

    @Override
    public void onInitialize() {
        MOD_LIST = getModIds();

        ServerPlayConnectionEvents.JOIN.register(this::loadInventory);
        ServerPlayConnectionEvents.DISCONNECT.register(this::saveInventory);
    }

    public void loadInventory(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();

        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        JSONObject object;

        try (FileReader reader = new FileReader(String.format("E:/%s.json", player.getUuid()))) {
            object = (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException e) {
            return;
        }

        for (int index=0; index<41; index++) {
            if (object.get(String.valueOf(index)) == null) {
                player.getInventory().setStack(index, ItemStack.EMPTY);
            } else {
                JsonElement item = JsonParser.parseString((String) object.get(String.valueOf(index)));
                String name = item.getAsJsonObject().get("id").toString().replace("\"", "");
                String mod = name.split(":")[0];
                if (!MOD_LIST.contains(mod)) {
                    String encode = encoder.encodeToString(object.get(String.valueOf(index)).toString().getBytes(StandardCharsets.UTF_8));
                    ItemStack itemStack = getUnknowItem(name, encode);
                    itemStack.setCount(item.getAsJsonObject().get("count").getAsInt());
                    player.getInventory().setStack(index, itemStack);
                    continue;
                }
                ItemStack itemStack = ItemStack.CODEC.decode(JsonOps.INSTANCE, item).getOrThrow().getFirst();
                player.getInventory().setStack(index, itemStack);
            }
        }
    }

    public void saveInventory(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        JSONObject object = new JSONObject();

        for (int index=0; index<41; index++) {
            ItemStack itemStack = player.getInventory().getStack(index);
            if (itemStack.isEmpty()) {
                object.put(String.valueOf(index), null);
            } else {
                if (itemStack.isOf(Items.FIREWORK_STAR)) {
                    NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
                    if (nbtComponent != null) {
                        NbtCompound nbt = nbtComponent.copyNbt();
                        if (nbt.contains("type") && Objects.equals(Objects.requireNonNull(nbt.get("type")).toString(), "\"unknow_item\"") && nbt.contains("id")) {
                            String decode = new String(decoder.decode(Objects.requireNonNull(nbt.get("id")).toString().replace("\"", "")), StandardCharsets.UTF_8);
                            JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
                            JSONObject data;
                            try {
                                data = (JSONObject) parser.parse(decode);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            data.replace("count", itemStack.getCount());
                            object.put(String.valueOf(index), data.toJSONString());
                            continue;
                        }
                    }
                }
                object.put(String.valueOf(index), ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, itemStack).getOrThrow().toString());
            }
        }
        try {
            Files.writeString(Paths.get(String.format("E:/%s.json", player.getUuid())), object.toJSONString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getModIds() {
        return FabricLoader.getInstance()
                .getAllMods()
                .stream()
                .map(ModContainer::getMetadata)
                .map(ModMetadata::getId)
                .collect(Collectors.toList());
    }

    public ItemStack getUnknowItem(String id, String component) {
        String itemComponent = """
                {
                    "id": "minecraft:firework_star",
                    "count": 1,
                    "components": {
                        "minecraft:item_model": "minecraft:barrier",
                        "minecraft:item_name": "未知物品",
                        "minecraft:custom_data": {
                            "id": "%s",
                            "type": "unknow_item"
                        },
                        "minecraft:lore": [
                            {
                                "text": "物品ID: %s",
                                "italic": false,
                                "color": "yellow"
                            }
                        ]
                    }
                }
                """;

        JsonElement item = JsonParser.parseString(String.format(itemComponent, component, id));

        return ItemStack.CODEC.decode(JsonOps.INSTANCE, item).getOrThrow().getFirst();
    }
}
