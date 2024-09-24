package me.marin.worldbopperplugin.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.marin.worldbopperplugin.util.VersionUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static me.marin.worldbopperplugin.WorldBopperPlugin.SETTINGS_PATH;

@ToString
public class WorldBopperSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Getter
    private static WorldBopperSettings instance = null;

    @SerializedName("worldbopper enabled")
    public boolean worldbopperEnabled = false;

    @SerializedName("worlds to keep")
    public List<KeepWorldInfo> worldsToKeep = new ArrayList<>();

    @SerializedName("boppable worlds buffer")
    public long savesBuffer = 100;

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
        } else {
            String s;
            try {
                s = FileUtil.readString(SETTINGS_PATH);
            } catch (IOException e) {
                Jingle.log(Level.ERROR, "(WorldBopper) Error while reading settings, resetting back to default:\n" + ExceptionUtil.toDetailedString(e));
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
            Jingle.log(Level.ERROR, "(WorldBopper) Failed to save WorldBopper Settings: " + ExceptionUtil.toDetailedString(e));
        }
    }

    private static void loadDefaultSettings() {
        instance = new WorldBopperSettings();
        instance.version = VersionUtil.CURRENT_VERSION.toString();
        instance.worldsToKeep = defaultKeepWorldInfo();
    }

    public static List<KeepWorldInfo> defaultKeepWorldInfo() {
        List<KeepWorldInfo> worldsToKeep = new ArrayList<>();
        worldsToKeep.add(new KeepWorldInfo("Random Speedrun #", KeepCondition.NETHER));
        worldsToKeep.add(new KeepWorldInfo("Set Speedrun #", KeepCondition.END));
        worldsToKeep.add(new KeepWorldInfo("Benchmark Reset #", KeepCondition.ALWAYS_DELETE));
        worldsToKeep.add(new KeepWorldInfo("New World", KeepCondition.COMPLETED));
        worldsToKeep.add(new KeepWorldInfo("Practice Seed", KeepCondition.BLIND));
        worldsToKeep.add(new KeepWorldInfo("Seed Paster", KeepCondition.BLIND));
        return worldsToKeep;
    }

    @AllArgsConstructor @Getter @Setter @ToString
    public static class KeepWorldInfo {

        @SerializedName("world prefix")
        private String prefix;

        @SerializedName("keep condition")
        private KeepCondition condition;

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