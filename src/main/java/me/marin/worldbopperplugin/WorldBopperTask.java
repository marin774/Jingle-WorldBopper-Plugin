package me.marin.worldbopperplugin;

import me.marin.worldbopperplugin.io.WorldBopperSettings;
import me.marin.worldbopperplugin.util.FileStillEmptyException;
import me.marin.worldbopperplugin.util.SpecialPrefix;
import me.marin.worldbopperplugin.util.WorldBopperUtil;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.marin.worldbopperplugin.WorldBopperPlugin.log;

public class WorldBopperTask implements Runnable {

    private static final int INTERVAL_SECONDS = 5;
    private static final int CLEARED_WORLDS_LOG_INTERVAL = 250;

    private ScheduledFuture<?> service;

    private final File savesDirectory;

    private final AtomicInteger totalBopped = new AtomicInteger(0);

    private final Map<WorldBopperSettings.KeepWorldInfo, Set<String>> worldsToKeepMap = new ConcurrentHashMap<>();

    public WorldBopperTask(Path savesPath) {
       this.savesDirectory = savesPath.toFile();
    }

    public void start() {
        if (this.service != null && !this.service.isCancelled()) {
            return;
        }
        this.service = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        service.cancel(false);
    }


    @Override
    public void run() {
        if (!WorldBopperSettings.getInstance().worldbopperEnabled) {
            return;
        }
        if (!savesDirectory.isDirectory()) return;

        int before = totalBopped.get() / CLEARED_WORLDS_LOG_INTERVAL;

        totalBopped.addAndGet(clearWorlds(false));

        int after = totalBopped.get() / CLEARED_WORLDS_LOG_INTERVAL;
        if (after > before) {
            WorldBopperPlugin.log(Level.DEBUG, "Bopped " + (after * CLEARED_WORLDS_LOG_INTERVAL) + " worlds in this instance.");
        }
    }


    private boolean isValidDirectoryName(String name) {
        return WorldBopperSettings.getInstance().getKeepWorldInfo(name) != null;
    }

    private boolean shouldKeepWorld(File file) {
        String worldName = file.getName();
        WorldBopperSettings.KeepWorldInfo keepWorldInfo = WorldBopperSettings.getInstance().getKeepWorldInfo(worldName);

        if (worldsToKeepMap.containsKey(keepWorldInfo) && worldsToKeepMap.get(keepWorldInfo).contains(worldName)) {
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

    public synchronized int clearWorlds(boolean ignoreKeepLatest) {
        File[] directories = this.savesDirectory.listFiles(File::isDirectory);
        if (directories == null) {
            // IO error, clear next time I guess
            return 0;
        }

        AtomicInteger deleted = new AtomicInteger();

        Map<WorldBopperSettings.KeepWorldInfo, List<File>> grouped = Arrays.stream(directories)
                .parallel()
                .filter(d -> isValidDirectoryName(d.getName()))
                .collect(Collectors.groupingBy(d -> WorldBopperSettings.getInstance().getKeepWorldInfo(d.getName())));

        // Determine which world names to keep per-prefix (keepLatest)
        Set<String> keepLatestWorlds = ConcurrentHashMap.newKeySet();
        if (!ignoreKeepLatest) {
            grouped.entrySet().parallelStream().forEach(entry -> {
                WorldBopperSettings.KeepWorldInfo keepInfo = entry.getKey();
                List<File> list = entry.getValue();
                list.sort(Comparator.comparingLong(File::lastModified).reversed());
                // Keep first 'keepLatest' worlds
                for (int i = 0; i < Math.min(keepInfo.getKeepLatest(), list.size()); i++) {
                    keepLatestWorlds.add(list.get(i).getName());
                }
            });
        }

        Arrays.stream(directories)
                .parallel()
                .filter(d -> !keepLatestWorlds.contains(d.getName()) && isValidDirectoryName(d.getName()))
                .forEach(f -> {
                    if (shouldKeepWorld(f)) {
                        WorldBopperSettings.KeepWorldInfo keepWorldInfo = WorldBopperSettings.getInstance().getKeepWorldInfo(f.getName());
                        boolean added = worldsToKeepMap.computeIfAbsent(keepWorldInfo, k -> ConcurrentHashMap.newKeySet()).add(f.getName());
                        if (added) {
                            WorldBopperPlugin.log(Level.DEBUG, "Keeping world '" + f.getName() + "', from prefix '" + keepWorldInfo.getPrefix() + "': '" + keepWorldInfo.getCondition().getDisplay() + "'");
                        }
                        return;
                    }

                    SpecialPrefix specialPrefix = SpecialPrefix.getSpecialPrefix(f.getName());
                    if (specialPrefix != null && !specialPrefix.canDelete(f.getName())) {
                        return; // skip worlds that can't be deleted yet
                    }

                    deleted.addAndGet(deleteDirectory(f) ? 1 : 0);
                });

        return deleted.get();
    }

    private boolean deleteDirectory(File f) {
        // Delete directory
        try (Stream<Path> stream = Files.walk(f.toPath())) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            return true;
        } catch (NoSuchFileException ignored) {
            log(Level.DEBUG, "File " + f.getName() + " is missing? (NoSuchFileException)");
        } catch (AccessDeniedException e) {
            log(Level.DEBUG, "Access for file " + f.getName() + " denied? (AccessDeniedException)");
        } catch (IOException e) {
            log(Level.ERROR, "Error while deleting worlds:\n" + ExceptionUtil.toDetailedString(e));
        }
        return false;
    }

    public void invalidateCache() {
        worldsToKeepMap.clear();
    }

}
