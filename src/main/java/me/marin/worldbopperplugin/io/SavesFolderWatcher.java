package me.marin.worldbopperplugin.io;

import me.marin.worldbopperplugin.WorldBopperPlugin;
import me.marin.worldbopperplugin.util.FileStillEmptyException;
import me.marin.worldbopperplugin.util.WorldBopperUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static me.marin.worldbopperplugin.WorldBopperPlugin.log;

public class SavesFolderWatcher extends FileWatcher {

    private static final int LOG_INTERVAL = 250;

    private final Map<WorldBopperSettings.KeepWorldInfo, Set<String>> worldsToKeepMap = new ConcurrentHashMap<>();

    private int totalDeleted = 0;

    public SavesFolderWatcher(Path path) {
        super("saves-folder-watcher", path.toFile());
        log(Level.DEBUG, "Saves folder watcher is running...");
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

        int before = totalDeleted / LOG_INTERVAL;

        totalDeleted += clearWorlds();

        int after = totalDeleted / LOG_INTERVAL;
        if (after > before) {
            WorldBopperPlugin.log(Level.DEBUG, "Cleared " + (after * LOG_INTERVAL) + " worlds in this instance.");
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
            if (eventsLogText == null) {
                return false;
            }
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

    public synchronized int clearWorlds() {
        long numKeep = 100;
        // Count how many worlds should be kept.
        // These world names are in worldsToKeep map, which is populated at the end of this method every time WorldBopper runs.
        for (WorldBopperSettings.KeepWorldInfo keepWorldInfo : WorldBopperSettings.getInstance().worldsToKeep) {
            if (keepWorldInfo.getCondition() != WorldBopperSettings.KeepCondition.ALWAYS_DELETE) {
                numKeep += worldsToKeepMap.getOrDefault(keepWorldInfo, new HashSet<>()).size();
            }
        }

        File[] directories = this.file.listFiles(File::isDirectory);
        if (directories == null) {
            // IO error, clear next time I guess
            return 0;
        }
        AtomicInteger deleted = new AtomicInteger();
        Arrays.stream(directories)
                .parallel()
                .filter(d -> isValidDirectoryName(d.getName()))
                .map(f -> Pair.of(f, f.lastModified())) // more efficient than only sorting by lastModified, shoutout duncan
                .sorted(Comparator.comparingLong(p -> -p.getRight())) // most recent first
                .map(Pair::getLeft)
                .skip(numKeep)
                .filter(f -> {
                    if (shouldKeepWorld(f)) {
                        WorldBopperSettings.KeepWorldInfo keepWorldInfo = WorldBopperSettings.getInstance().getKeepWorldInfo(f.getName());

                        worldsToKeepMap.computeIfAbsent(keepWorldInfo, k -> new HashSet<>()).add(f.getName());
                        return false;
                    }
                    return true;
                })
                .forEach(f -> {
                    // Delete
                    try (Stream<Path> stream = Files.walk(f.toPath())) {
                        stream.sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                        deleted.addAndGet(1);
                    } catch (NoSuchFileException ignored) {
                        log(Level.DEBUG, "File " + f.getName() + " is missing? (NoSuchFileException)");
                    } catch (AccessDeniedException e) {
                        log(Level.DEBUG, "Access for file " + f.getName() + " denied? (AccessDeniedException)");
                    } catch (IOException e) {
                        log(Level.ERROR, "Error while deleting worlds:\n" + ExceptionUtil.toDetailedString(e));
                    }
                });
        return deleted.get();
    }

    public void invalidateCache() {
        worldsToKeepMap.clear();
    }

}
