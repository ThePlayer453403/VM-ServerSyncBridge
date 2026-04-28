package com.tp4.syncbridge;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileOperation {
    public static JSONObject readJson(String path, String file) {
        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        try (FileReader reader = new FileReader(Paths.get(Paths.get(path, file).toString()).toFile())) {
            return (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException exception) {
            return new JSONObject();
        }
    }
    public static void saveJson(String path, String file, JSONObject object) {
        Path full_path = Paths.get(Paths.get(path, file).toString());
        try {
            Files.writeString(full_path, object.toJSONString());
        } catch (IOException exception) {
            try {
                Files.createDirectories(Path.of(path));
                Files.writeString(full_path, object.toJSONString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
