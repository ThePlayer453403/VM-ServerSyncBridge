package com.tp4.syncbridge;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileOperation {
    public static JSONObject readJson(String path) {
        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        try (FileReader reader = new FileReader(path)) {
            return (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException exception) {
            return new JSONObject();
        }
    }
    public static void saveJson(String path, JSONObject object) {
        try {
            Files.writeString(Paths.get(path), object.toJSONString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
