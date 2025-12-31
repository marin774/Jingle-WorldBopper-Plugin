package me.marin.worldbopperplugin.io;

import com.google.gson.*;
import me.marin.worldbopperplugin.WorldBopperPlugin;
import me.marin.worldbopperplugin.util.SpecialPrefix;
import org.apache.logging.log4j.Level;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeepWorldInfoListSerializer implements JsonSerializer<List<WorldBopperSettings.KeepWorldInfo>>, JsonDeserializer<List<WorldBopperSettings.KeepWorldInfo>> {

    private static final String WORLD_PREFIX = "world prefix";
    private static final String KEEP_CONDITION = "keep condition";
    private static final String KEEP_LATEST = "keep lastest";

    @Override
    public JsonElement serialize(List<WorldBopperSettings.KeepWorldInfo> keepWorldInfos, Type type, JsonSerializationContext context) {
        JsonArray array = new JsonArray();

        for (WorldBopperSettings.KeepWorldInfo keepWorldInfo : keepWorldInfos) {
            JsonObject obj = new JsonObject();
            obj.addProperty(WORLD_PREFIX, keepWorldInfo.getPrefix());
            obj.add(KEEP_CONDITION, context.serialize(keepWorldInfo.getCondition()));
            obj.addProperty(KEEP_LATEST, keepWorldInfo.getKeepLatest());

            array.add(obj);
        }

        return array;
    }

    @Override
    public List<WorldBopperSettings.KeepWorldInfo> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        List<WorldBopperSettings.KeepWorldInfo> list = new ArrayList<>();
        JsonArray array = jsonElement.getAsJsonArray();

        // special prefixes are inserted at the end
        Map<String, WorldBopperSettings.KeepCondition> specialPrefixesCondition = new HashMap<>();

        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();

            String prefix = obj.get(WORLD_PREFIX).getAsString();
            int keepLatest = obj.get(KEEP_LATEST) != null ? obj.get(KEEP_LATEST).getAsInt() : 5;
            WorldBopperSettings.KeepCondition condition = context.deserialize(obj.get(KEEP_CONDITION), WorldBopperSettings.KeepCondition.class);
            if (SpecialPrefix.isSpecialPrefixExact(prefix)) {
                specialPrefixesCondition.put(prefix, condition);
            } else {
                list.add(new WorldBopperSettings.KeepWorldInfo(prefix, condition, keepLatest,false));
            }
        }

        WorldBopperPlugin.log(Level.DEBUG, "Adding special prefixes to the start...");

        SpecialPrefix.ALL_PREFIXES.forEach((k, v) -> {
            list.add(0, new WorldBopperSettings.KeepWorldInfo(k, specialPrefixesCondition.getOrDefault(k, v.getDefaultKeepCondition()), v.getKeepLatest(), true));
        });

        return list;
    }
}
