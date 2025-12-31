package me.marin.worldbopperplugin.io;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import me.marin.worldbopperplugin.util.VersionUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static me.marin.worldbopperplugin.WorldBopperPlugin.SETTINGS_PATH;
import static me.marin.worldbopperplugin.WorldBopperPlugin.log;

@ToString
public class WorldBopperSettings {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(new TypeToken<List<KeepWorldInfo>>(){}.getType(), new KeepWorldInfoListSerializer())
            .create();

    @Getter
    private static WorldBopperSettings instance = null;

    @SerializedName("worldbopper enabled")
    public boolean worldbopperEnabled = false;

    @SerializedName("worlds to keep")
    public List<KeepWorldInfo> worldsToKeep = new ArrayList<>();

    @SerializedName("version")
    public String version;

    public KeepWorldInfo getKeepWorldInfo(String fileName) {
        for (WorldBopperSettings.KeepWorldInfo keepWorldInfo : WorldBopperSettings.getInstance().worldsToKeep) {
            if (StringUtils.isBlank(keepWorldInfo.getPrefix())) {
                continue;
            }
            if (fileName.startsWith(keepWorldInfo.getPrefix())) {
                return keepWorldInfo;
            }
        }
        return null;
    }

    public static void load() {
        if (!Files.exists(SETTINGS_PATH)) {
            loadDefaultSettings();
            save();
            load(); // force reload (because custom deserializer does some important stuff)
        } else {
            String s;
            try {
                s = FileUtil.readString(SETTINGS_PATH);
            } catch (IOException e) {
                log(Level.ERROR, "Error while reading settings, resetting back to default:\n" + ExceptionUtil.toDetailedString(e));
                loadDefaultSettings();
                return;
            }
            instance = GSON.fromJson(s, WorldBopperSettings.class);
        }
    }

    public static void save() {
        try {
            FileUtil.writeString(SETTINGS_PATH, GSON.toJson(instance));
        } catch (IOException e) {
            log(Level.ERROR, "Failed to save WorldBopper Settings: " + ExceptionUtil.toDetailedString(e));
        }
    }

    private static void loadDefaultSettings() {
        instance = new WorldBopperSettings();
        instance.version = VersionUtil.CURRENT_VERSION.toString();
        instance.worldsToKeep = defaultKeepWorldInfo();
    }

    public static List<KeepWorldInfo> defaultKeepWorldInfo() {
        List<KeepWorldInfo> worldsToKeep = new ArrayList<>();
        // Special world prefixes are added in KeepWorldInfoListSerializer
        worldsToKeep.add(new KeepWorldInfo("Benchmark Reset #", KeepCondition.ALWAYS_DELETE, 1,false));
        worldsToKeep.add(new KeepWorldInfo("New World", KeepCondition.ALWAYS_DELETE, 5,false));
        return worldsToKeep;
    }

    /**
     * Serialized by {@link KeepWorldInfoListSerializer}
     */
    @AllArgsConstructor @Getter @Setter @ToString @EqualsAndHashCode
    public static class KeepWorldInfo {

        private String prefix;

        private KeepCondition condition;

        private int keepLatest;

        private boolean special;

    }

    @AllArgsConstructor @Getter @ToString
    public enum KeepCondition {

        @SerializedName("always delete")
        ALWAYS_DELETE("Always delete", null),

        @SerializedName("nether")
        NETHER("Reached Nether", "rsg.enter_nether"),

        @SerializedName("bastion")
        BASTION("Reached Bastion", "rsg.enter_bastion"),

        @SerializedName("fortress")
        FORTRESS("Reached Fortress", "rsg.enter_fortress"),

        @SerializedName("structure 1")
        STRUCTURE_1("Reached one structure", null),

        @SerializedName("structure 2")
        STRUCTURE_2("Reached both structures", null),

        @SerializedName("nether exit")
        BLIND("Reached Nether Exit", "rsg.first_portal"),

        @SerializedName("stronghold")
        STRONGHOLD("Reached Stronghold", "rsg.enter_stronghold"),

        @SerializedName("end")
        END("Reached End", "rsg.enter_end"),

        @SerializedName("completed")
        COMPLETED("Completed", "rsg.credits");


        private final String display;
        private final String eventName;

        public static KeepCondition match(String s) {
            for (KeepCondition value : KeepCondition.values()) {
                if (value.display.equals(s)) {
                    return value;
                }
            }
            return null;
        }

    }

}