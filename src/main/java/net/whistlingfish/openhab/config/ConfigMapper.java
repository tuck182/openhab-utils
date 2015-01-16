package net.whistlingfish.openhab.config;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

public class ConfigMapper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .disable(FAIL_ON_UNKNOWN_PROPERTIES);

    private Map<String, Object> config;

    public ConfigMapper(Dictionary<String, ?> config) {
        this.config = new HashMap<String, Object>();
        for (Enumeration<String> iterator = config.keys(); iterator.hasMoreElements();) {
            String key = iterator.nextElement();
            this.config.put(key, config.get(key));
        }
    }

    public void bindTo(Object object) {
        String json;
        try {
            json = OBJECT_MAPPER.writeValueAsString(config);
            OBJECT_MAPPER.readerForUpdating(object).readValue(json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to map config", e);
        }
    }

}
