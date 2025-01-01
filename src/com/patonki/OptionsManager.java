package com.patonki;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class OptionsManager {
    private final HashMap<String, String> values = new HashMap<>();
    private final HashMap<String, String> defaultValues = new HashMap<>();

    private final String optionsFile;
    private static final String defaultOptions =
                    "height=260\n" +
                    "width=272\n" +
                    "fontSize=19\n" +
                    "ftpHost=null\n" +
                    "ftpUsername=null\n" +
                    "ftpPassword=null\n" +
                    "user=default";

    public OptionsManager(String optionsFile) {
        this.optionsFile = optionsFile;
    }
    @SuppressWarnings("CallToPrintStackTrace")
    public void readConfig() {
        String config = defaultOptions;
        try {
            File userOptions = new File(optionsFile);
            Path path = Paths.get(optionsFile);
            if (!userOptions.exists()) {
                Files.write(path, defaultOptions.getBytes(StandardCharsets.UTF_8));
            }

            config = new String(Files.readAllBytes(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        parseValues(defaultOptions, defaultValues);
        parseValues(config, values);
    }
    private void parseValues(String config, HashMap<String, String> values) {
        String[] lines = config.split("\n");
        for (String line : lines) {
            int firstEquals = line.indexOf('=');
            if (firstEquals == -1) continue;
            String variable = line.substring(0, firstEquals);
            String value = line.substring(firstEquals+1);
            if (value.equals("null")) value = null;
            values.put(variable, value);
        }
    }
    @SuppressWarnings("CallToPrintStackTrace")
    public void writeConfig() {
        StringBuilder res = new StringBuilder();
        for (String variable : values.keySet()) {
            res.append(variable).append('=').append(values.get(variable)).append("\n");
        }
        try {
            Files.write(Paths.get(optionsFile), res.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public int getIntProperty(String key) {
        try {
            return Integer.parseInt(getStringProperty(key));
        } catch (NumberFormatException e) {
            return Integer.parseInt(defaultValues.get(key));
        }
    }
    public String getStringProperty(String key) {
        if (!values.containsKey(key)) {
            return defaultValues.get(key);
        }
        return values.get(key);
    }
    public void setProperty(String key, String value) {
        values.put(key, value);
    }
}
