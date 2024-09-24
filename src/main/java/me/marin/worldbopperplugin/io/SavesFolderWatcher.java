package me.marin.worldbopperplugin.io;

import me.marin.worldbopperplugin.util.FileStillEmptyException;
import me.marin.worldbopperplugin.util.WorldBopperUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class SavesFolderWatcher extends FileWatcher {

    private static final Map<WorldBopperSettings.KeepWorldInfo, Set<String>> worldsToKeepMap = new ConcurrentHashMap<>();

    public SavesFolderWatcher(Path path) {
        super("saves-folder-watcher", path.toFile());
        Jingle.log(Level.DEBUG, "(WorldBopper) Saves folder watcher is running...");
    }

    @Override
    protected void handleFileUpdated(File file) {
        // ignored
    }

    @Override
    protected void handleFileCreated(File file) {
        if (!WorldBopperSettings.getInstance().worldbopperEnabled) {
            return;
        }
        if (!file.isDirectory()) return;

        String dirName = file.getName();
        if (!isValidDirectoryName(dirName)) return;

        long worldsToKeep = WorldBopperSettings.getInstance().savesBuffer;
        // Count how many worlds should be kept.
        // These world names are in worldsToKeep map, which is populated at the end of this method every time WorldBopper runs.
        // Known "issue": if user manually deletes those worlds at some point, savesBuffer will essentially be larger until Jingle is restarted.
        for (WorldBopperSettings.KeepWorldInfo keepWorldInfo : WorldBopperSettings.getInstance().worldsToKeep) {
            if (keepWorldInfo.getCondition() != WorldBopperSettings.KeepCondition.ALWAYS_DELETE) {
                worldsToKeep += this.worldsToKeepMap.getOrDefault(keepWorldInfo, new HashSet<>()).size();
            }
        }

        File[] directories = this.file.listFiles(File::isDirectory);
        if (directories == null) {
            // IO error, clear next time I guess
            return;
        }
        File[] validDirectories = Arrays.stream(directories)
                .filter(d -> isValidDirectoryName(d.getName()))
                .toArray(File[]::new);

        if (validDirectories.length <= worldsToKeep) {
            Jingle.log(Level.DEBUG, "(WorldBopper) Not deleting any worlds (" + validDirectories.length + " <= " + worldsToKeep + ")");
            return;
        }

        // Sort valid worlds by time
        try {
            Arrays.sort(validDirectories, Comparator.comparingLong(File::lastModified));
        } catch (IllegalArgumentException ignored) {
            // rare JDK bug (https://stackoverflow.com/questions/13575224/comparison-method-violates-its-general-contract-timsort-and-gridlayout)
            // probably not fixable because Arrays class might be loaded before the plugin
            return;
        }

        for (int i = 0; i < validDirectories.length - worldsToKeep; i++) {
            File oldestDir = validDirectories[i];

            // Keep worlds
            if (shouldKeepWorld(oldestDir)) {
                WorldBopperSettings.KeepWorldInfo keepWorldInfo = WorldBopperSettings.getInstance().getKeepWorldInfo(oldestDir.getName());

                if (!worldsToKeepMap.containsKey(keepWorldInfo)) {
                    worldsToKeepMap.put(keepWorldInfo, new HashSet<>());
                }
                worldsToKeepMap.get(keepWorldInfo).add(oldestDir.getName());

                Jingle.log(Level.DEBUG, "(WorldBopper) Not deleting " + oldestDir.getName() + " because it has nether enter!");
                continue;
            }

            // Delete
            Jingle.log(Level.DEBUG, "(WorldBopper) Deleting " + oldestDir.getName());
            try (Stream<Path> stream = Files.walk(oldestDir.toPath())) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (NoSuchFileException ignored) {
                Jingle.log(Level.DEBUG, "(WorldBopper) File " + oldestDir.getName() + " is missing? (NoSuchFileException)");
            } catch (AccessDeniedException e) {
                Jingle.log(Level.DEBUG, "(WorldBopper) Access for file " + oldestDir.getName() + " denied? (AccessDeniedException)");
            } catch (IOException e) {
                Jingle.log(Level.ERROR, "(WorldBopper) Unknown error while deleting worlds:\n" + ExceptionUtil.toDetailedString(e));
            }
        }
    }

    private boolean isValidDirectoryName(String name) {
        return WorldBopperSettings.getInstance().getKeepWorldInfo(name) != null;
    }

    private boolean shouldKeepWorld(File file) {
        String worldName = file.getName();
        WorldBopperSettings.KeepWorldInfo keepWorldInfo = WorldBopperSettings.getInstance().getKeepWorldInfo(worldName);

        if (worldsToKeepMap.getOrDefault(keepWorldInfo, new HashSet<>()).contains(worldName)) {
            return true;
        }

        WorldBopperSettings.KeepCondition keepCondition = keepWorldInfo.getCondition();
        if (keepCondition == WorldBopperSettings.KeepCondition.ALWAYS_DELETE) {
            return false;
        }

        File speedrunIGTDir = new File(file, "speedrunigt");
        if (!speedrunIGTDir.exists()) {
            return false;
        }
        File eventsLog = new File(speedrunIGTDir, "events.log");
        if (!eventsLog.exists()) {
            return false;
        }

        try {
            String eventsLogText = WorldBopperUtil.readFile(eventsLog.toPath());
            boolean hasBastion = false;
            boolean hasFortress = false;
            for (String line : eventsLogText.split("[\\r\\n]+")) {
                if (keepCondition.getEventName() != null) {
                    if (line.startsWith(keepCondition.getEventName())) {
                        return true;
                    }
                } else {
                    if (line.startsWith("rsg.enter_bastion")) {
                        hasBastion = true;
                    }
                    if (line.startsWith("rsg.enter_fortress")) {
                        hasFortress = true;
                    }

                    switch (keepCondition) {
                        case STRUCTURE_1: {
                            if (hasBastion || hasFortress) {
                                return true;
                            }
                            break;
                        }
                        case STRUCTURE_2: {
                            if (hasBastion && hasFortress) {
                                return true;
                            }
                            break;
                        }
                    }
                }
            }
            return false;
        } catch (FileStillEmptyException e) {
            // file is probably actually empty, this should never happen, clear this world
            return false;
        }
    }

    public static void clearWorldsToKeepCache() {
        worldsToKeepMap.clear();
    }

}
