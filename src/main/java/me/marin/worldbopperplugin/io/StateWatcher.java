package me.marin.worldbopperplugin.io;

import me.marin.worldbopperplugin.WorldBopperPlugin;
import me.marin.worldbopperplugin.util.FileStillEmptyException;
import me.marin.worldbopperplugin.util.SpecialPrefix;
import me.marin.worldbopperplugin.util.WorldBopperUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.io.File;
import java.nio.file.Path;

import static me.marin.worldbopperplugin.WorldBopperPlugin.log;

public class StateWatcher extends FileWatcher {

    private static final String WALL = "wall";

    private final Path atumDirectory;
    private final Path wpStateoutPath;
    private String previousState;

    public StateWatcher(Path atumDirectory, Path wpStateoutPath) {
        super("state-watcher", wpStateoutPath.getParent().toFile(), wpStateoutPath.getFileName().toString());

        this.atumDirectory = atumDirectory;
        this.wpStateoutPath = wpStateoutPath;
        this.previousState = readWpStateout();
    }

    @Override
    protected void handleFileUpdated(File file) {
        String state = readWpStateout();

        if (!WALL.equals(previousState) && WALL.equals(state)) {
            // wall was entered, update special prefix
            SpecialPrefix.ALL_PREFIXES.forEach((p, sp) -> {
                int resets = readResets(atumDirectory.resolve(sp.getFileName()));
                sp.setSafeDeleteUpTo(resets - 50); // buffer of 50 wall worlds
                WorldBopperPlugin.log(Level.DEBUG, "Now deleting worlds up to '" + p + sp.getSafeDeleteUpTo() + "'.");
            });
        }

        previousState = state;
    }

    @Override
    protected void handleFileCreated(File file) {
        // ignored
    }

    private String readWpStateout() {
        try {
            return WorldBopperUtil.readFile(this.wpStateoutPath);
        } catch (FileStillEmptyException e) {
            log(Level.ERROR, "wpstateout.txt is empty?\n" + ExceptionUtil.toDetailedString(e));
            return null;
        }
    }

    private int readResets(Path path) {
        String resetString;
        try {
            resetString = WorldBopperUtil.readFile(path);
        } catch (FileStillEmptyException e) {
            log(Level.ERROR, path.getFileName() + " is empty?\n" + ExceptionUtil.toDetailedString(e));
            return -1;
        }
        try {
            return Integer.parseInt(resetString);
        } catch (NumberFormatException e) {
            log(Level.ERROR, "invalid number in " + path.getFileName() + " '" + resetString + "':\n" + ExceptionUtil.toDetailedString(e));
            return -1;
        }
    }

}
