package com.tp4.syncbridge;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Config {
    public static JSONObject loadConfig() {
        JSONObject config;
        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        try (FileReader reader = new FileReader("./config/sync.json")) {
            config = (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException exception) {
            createConfig();
            return loadConfig();
        }
        if (config.containsKey("sync_dir")) {
            return config;
        } else {
            createConfig();
            return loadConfig();
        }
    }

    public static void createConfig() {
        try {
            if (
                    new File("./config").mkdirs() ||
                    new File("./config/sync.json").createNewFile()
            ) {
                Files.writeString(Paths.get("./config/sync.json"), """
                            {
                                "sync_dir": "./sync"
                            }
                            """);
            } else {
                throw new RuntimeException();
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
