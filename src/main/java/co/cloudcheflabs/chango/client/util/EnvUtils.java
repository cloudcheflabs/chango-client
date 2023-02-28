package co.cloudcheflabs.chango.client.util;

import java.lang.reflect.Field;
import java.util.Map;

public class EnvUtils {

    public static String getEnv(String key) {
        Map<String, String> envMap = System.getenv();
        return envMap.get(key);
    }

    public static void setEnv(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }
}
