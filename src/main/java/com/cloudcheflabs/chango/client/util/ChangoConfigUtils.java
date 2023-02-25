package com.cloudcheflabs.chango.client.util;

import com.cloudcheflabs.chango.client.domain.ConfigProps;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class ChangoConfigUtils {
    public static String getConfigFile() {
        String changoHome = System.getProperty("user.home") + "/.chango";
        File f = new File(changoHome);
        if (!f.exists()) {
            f.mkdir();
        }

        String configFile = changoHome + "/config";
        return new File(configFile).getAbsolutePath();
    }

    public static ConfigProps getConfigProps() {
        try {
            String json = FileUtils.fileToString(getConfigFile(), false);
            return (ConfigProps)(new ObjectMapper()).readValue(json, ConfigProps.class);
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }

    public static void updateConfigProps(ConfigProps changoEnv) {
        FileUtils.stringToFile(JsonUtils.toJson(changoEnv), getConfigFile(), false);
    }
}
