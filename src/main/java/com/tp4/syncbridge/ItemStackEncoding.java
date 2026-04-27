package com.tp4.syncbridge;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryOps;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ItemStackEncoding {
    private static final Base64.Encoder base64Encoder = Base64.getEncoder();
    private static final Base64.Decoder base64Decoder = Base64.getDecoder();
    private static final Item UnknowItem = Items.FIREWORK_STAR;

    public static String encodeItemStack(ItemStack itemStack, RegistryOps<JsonElement> ops) {
        if (itemStack.isEmpty()) {return null;}

        if (itemStack.isOf(UnknowItem)) {
            NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            if (nbtComponent != null) {
                NbtCompound nbt = nbtComponent.copyNbt();
                if (Objects.equals(nbt.getString("type", ""), "unknow_item") && nbt.contains("data")) {
                    JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
                    try {
                        JSONObject data = (JSONObject) parser.parse(base64Decoder.decode(nbt.getString("data", "")));
                        data.put("count", itemStack.getCount());
                        return base64Encoder.encodeToString(data.toJSONString().getBytes(StandardCharsets.UTF_8));
                    } catch (ParseException exception) {
                        return null;
                    }
                }
            }
        }

        String data = ItemStack.CODEC.encodeStart(ops, itemStack).getOrThrow().toString();
        return base64Encoder.encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    public static ItemStack decodeItemStack(String string, RegistryOps<JsonElement> ops) {
        if (string == null) {return ItemStack.EMPTY;}
        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        JSONObject data;
        try {
            data = (JSONObject) parser.parse(base64Decoder.decode(string));
        } catch (ParseException e) {
            return ItemStack.EMPTY;
        }

        if (data.containsKey("id") && !modID().contains(data.getAsString("id").split(":")[0])) {
            int count = data.getAsNumber("count").intValue();
            data.remove("count");
            ItemStack itemStack = getUnknowItem(data.getAsString("id"), base64Encoder.encodeToString(data.toJSONString().getBytes(StandardCharsets.UTF_8)));
            itemStack.setCount(count);
            return itemStack;
        }

        return ItemStack.CODEC.decode(ops, JsonParser.parseString(data.toJSONString())).getOrThrow().getFirst();
    }

    private static List<String> modID() {
        return FabricLoader.getInstance()
                .getAllMods()
                .stream()
                .map(ModContainer::getMetadata)
                .map(ModMetadata::getId)
                .collect(Collectors.toList());
    }

    private static ItemStack getUnknowItem(String id, String component) {
        String itemComponent = """
                {
                    "id": "minecraft:firework_star",
                    "count": 1,
                    "components": {
                        "minecraft:item_model": "minecraft:barrier",
                        "minecraft:item_name": "未知物品",
                        "minecraft:custom_data": {
                            "data": "%s",
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
