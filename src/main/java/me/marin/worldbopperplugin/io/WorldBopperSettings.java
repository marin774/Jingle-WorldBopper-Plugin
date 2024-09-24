package me.marin.worldbopperplugin.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import me.marin.worldbopperplugin.util.VersionUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;

import static me.marin.worldbopperplugin.WorldBopperPlugin.SETTINGS_PATH;

public class WorldBopperSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static WorldBopperSettings instance = null;

    @SerializedName("worldbopper enabled")
    public boolean worldbopperEnabled = false;

    @SerializedName("keep nether worlds")
    public boolean keepNetherWorlds = true;

    @SerializedName("max worlds to keep")
    public long savesBuffer = 100;

    @SerializedName("version")
    public String version;

    public static WorldBopperSettings getInstance() {
        return instance;
    }

    public static void load() {
        if (!Files.exists(SETTINGS_PATH)) {
            instance = new WorldBopperSettings();
            instance.version = VersionUtil.CURRENT_VERSION.toString();
            save();
        } else {
            String s;
            try {
                s = FileUtil.readString(SETTINGS_PATH);
            } catch (IOException e) {
                instance = new WorldBopperSettings();
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

}