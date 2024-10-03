package me.marin.worldbopperplugin;

import com.google.common.io.Resources;
import me.marin.worldbopperplugin.gui.ConfigGUI;
import me.marin.worldbopperplugin.io.InstanceManagerRunnable;
import me.marin.worldbopperplugin.io.WorldBopperSettings;
import me.marin.worldbopperplugin.util.UpdateUtil;
import me.marin.worldbopperplugin.util.VersionUtil;
import me.marin.worldbopperplugin.util.WorldBopperUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.JingleAppLaunch;
import xyz.duncanruns.jingle.JingleOptions;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.plugin.PluginManager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static me.marin.worldbopperplugin.util.VersionUtil.CURRENT_VERSION;
import static me.marin.worldbopperplugin.util.VersionUtil.version;

public class WorldBopperPlugin {

    public static final Path WORLD_BOPPER_FOLDER_PATH = Jingle.FOLDER.resolve("worldbopper-plugin");
    public static final Path PLUGINS_PATH = Jingle.FOLDER.resolve("plugins");
    public static final Path SETTINGS_PATH = WORLD_BOPPER_FOLDER_PATH.resolve("settings.json");

    public static ConfigGUI configGUI;

    public static void initialize() {
        log(Level.INFO, "Running WorldBopper Plugin v" + CURRENT_VERSION + "!");

        boolean isFirstLaunch = !WORLD_BOPPER_FOLDER_PATH.toFile().exists();
        WORLD_BOPPER_FOLDER_PATH.toFile().mkdirs();

        if (isFirstLaunch) {
            UpdateUtil.importSettingsFromJulti();
        }

        WorldBopperSettings.load();
        VersionUtil.Version version = version(WorldBopperSettings.getInstance().version);
        if (version.isOlderThan(CURRENT_VERSION)) {
            updateFrom(version);
        }

        WorldBopperUtil.runTimerAsync(new InstanceManagerRunnable(), 1000);

        VersionUtil.deleteOldVersionJars();
        UpdateUtil.checkForUpdatesAndUpdate(true);

        configGUI = new ConfigGUI();
        JingleGUI.addPluginTab("World Bopper", configGUI);
    }

    public static void updateFrom(VersionUtil.Version version) {
        log(Level.INFO, "Updating data from version " + version + ".");
        if (version.isOlderThan(version("1.1.1"))) {
            if (WorldBopperSettings.getInstance().worldsToKeep.isEmpty()) {
                WorldBopperSettings.getInstance().worldsToKeep = WorldBopperSettings.defaultKeepWorldInfo();
            }
            log(Level.INFO, "[1.1.1] Added missing default prefixes");
        }

        WorldBopperSettings.getInstance().version = CURRENT_VERSION.toString();
        WorldBopperSettings.save();
        log(Level.INFO, "Updated data to v" + CURRENT_VERSION);
    }

    public static void log(Level level, String message) {
        Jingle.log(level, "(WorldBopper) " + message);
    }

    public static void main(String[] args) throws IOException {
        JingleAppLaunch.launchWithDevPlugin(args, PluginManager.JinglePluginData.fromString(
                Resources.toString(Resources.getResource(WorldBopperPlugin.class, "/jingle.plugin.json"), Charset.defaultCharset())
        ), WorldBopperPlugin::initialize);
    }

}
