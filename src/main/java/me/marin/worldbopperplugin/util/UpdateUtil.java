package me.marin.worldbopperplugin.util;

import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import com.google.gson.JsonObject;
import lombok.Data;
import me.marin.worldbopperplugin.gui.UpdateProgressFrame;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.JingleAppLaunch;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.GrabUtil;
import xyz.duncanruns.jingle.util.PowerShellUtil;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static me.marin.worldbopperplugin.WorldBopperPlugin.PLUGINS_PATH;
import static me.marin.worldbopperplugin.WorldBopperPlugin.SETTINGS_PATH;

/**
 * Most code from <a href="https://github.com/DuncanRuns/Julti/blob/main/src/main/java/xyz/duncanruns/julti/util/UpdateUtil.java">Julti</a>
 */
public class UpdateUtil {

    public static void checkForUpdatesAndUpdate(boolean isOnLaunch) {
        WorldBopperUtil.runAsync("update-checker", () -> {
            UpdateInfo updateInfo = UpdateUtil.tryCheckForUpdates();
            if (updateInfo.isSuccess()) {
                int choice = JOptionPane.showConfirmDialog(null, updateInfo.getMessage(), "Update found!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    UpdateUtil.tryUpdateAndRelaunch(updateInfo.getDownloadURL());
                }
            } else {
                if (!isOnLaunch) {
                    JOptionPane.showMessageDialog(null, updateInfo.getMessage());
                }
            }
        });
    }

    public static UpdateInfo tryCheckForUpdates() {
        try {
            return checkForUpdates();
        } catch (Exception e) {
            return new UpdateInfo(false, "Could not check for updates. Github might be rate-limiting you, try again later.", null);
        }
    }

    public synchronized static UpdateInfo checkForUpdates() throws IOException {
        JsonObject meta = GrabUtil.grabJson("https://raw.githubusercontent.com/marin774/Jingle-WorldBopper-Plugin/main/meta.json");

        Jingle.log(Level.DEBUG, "(WorldBopper) Grabbed WorldBopper meta: " + meta.toString());

        VersionUtil.Version latestVersion = VersionUtil.version(meta.get("latest").getAsString());
        String downloadURL = meta.get("latest_download").getAsString();
        boolean isOutdated = VersionUtil.CURRENT_VERSION.isOlderThan(latestVersion);

        if (isOutdated) {
            return new UpdateInfo(true, "New WorldBopper Plugin version found: v" + latestVersion + "! Update now?", downloadURL);
        } else {
            return new UpdateInfo(false, "No new versions found.", null);
        }
    }

    @Data
    public static class UpdateInfo {
        private final boolean success;
        private final String message;
        private final String downloadURL;
    }

    public static void tryUpdateAndRelaunch(String download) {
        try {
            updateAndRelaunch(download);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Unknown error while updating. Try again or update manually.");
            Jingle.log(Level.ERROR, "(WorldBopper) Unknown error while updating:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    private static void updateAndRelaunch(String download) throws IOException, PowerShellExecutionException {
        Path newJarPath = PLUGINS_PATH.resolve(URLDecoder.decode(FilenameUtils.getName(download), StandardCharsets.UTF_8.name()));

        if (!Files.exists(newJarPath)) {
            Jingle.log(Level.DEBUG, "(WorldBopper) Downloading new jar to " + newJarPath);
            downloadWithProgress(download, newJarPath);
            Jingle.log(Level.DEBUG, "(WorldBopper) Downloaded new jar " + newJarPath.getFileName());
        }

        Path javaExe = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("javaw.exe");

        // Release LOCK so updating can go smoothly
        JingleAppLaunch.releaseLock();
        Jingle.options.save();

        // Use powershell's start-process to start it detached
        String powerCommand = String.format("start-process '%s' '-jar \"%s\"'", javaExe, Jingle.getSourcePath());
        Jingle.log(Level.INFO, "(WorldBopper) Exiting and running powershell command: " + powerCommand);
        PowerShellUtil.execute(powerCommand);

        System.exit(0);
    }

    private static void downloadWithProgress(String download, Path newJarPath) throws IOException {
        Point location = JingleGUI.get().getLocation();
        JProgressBar bar = new UpdateProgressFrame(location).getBar();
        bar.setMaximum((int) GrabUtil.getFileSize(download));
        GrabUtil.download(download, newJarPath, bar::setValue, 128);
    }

    /**
     * Imports settings.json, credentials.json and obs-overlay-template from Julti plugin (if these exist).
     */
    public static void importSettingsFromJulti() {
        Path jultiWorldBopperPluginPath = Paths.get(System.getProperty("user.home")).resolve(".Julti").resolve("worldbopper-plugin");
        if (jultiWorldBopperPluginPath.toFile().exists()) {
            // Import existing Julti settings to prevent double setup
            Jingle.log(Level.INFO, "(WorldBopper) Importing Julti settings.");

            boolean success = true;
            Path settingsPath = jultiWorldBopperPluginPath.resolve("settings.json");
            if (settingsPath.toFile().exists()) {
                try {
                    Files.copy(settingsPath, SETTINGS_PATH, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    success = false;
                    Jingle.log(Level.ERROR, "(WorldBopper) Error while trying to copy settings.json from Julti:\n" + ExceptionUtil.toDetailedString(e));
                }
            }

            if (success) {
                Jingle.log(Level.INFO, "(WorldBopper) Imported Julti settings!");
                JOptionPane.showMessageDialog(null, "WorldBopper has imported settings from Julti.", "WorldBopper - Imported from Julti", JOptionPane.INFORMATION_MESSAGE);
            } else {
                Jingle.log(Level.INFO, "(WorldBopper) Couldn't import Julti settings.");
                JOptionPane.showMessageDialog(null, "WorldBopper tried to import settings from Julti, but failed.\nCheck the logs for more information.", "WorldBopper - Failed to import", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}
