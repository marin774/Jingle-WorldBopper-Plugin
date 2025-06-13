package me.marin.worldbopperplugin.io;

import me.marin.worldbopperplugin.util.WorldBopperUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.instance.InstanceChecker;
import xyz.duncanruns.jingle.instance.OpenedInstanceInfo;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static me.marin.worldbopperplugin.WorldBopperPlugin.log;

/**
 * Creates new world folder watchers when new instances appear.
 * <p>
 * Ideally this shouldn't even be a thing because SeedQueue is single-instance, but
 * it's used as a safety net if you relaunch instances or have other instances in background etc.
 */
public class InstanceManagerRunnable implements Runnable {

    public static final Map<String, SavesFolderWatcher> instanceWatcherMap = new HashMap<>();

    private final HashSet<String> previousOpenInstancePaths = new HashSet<>();

    @Override
    public void run() {
        try {
            doRun();
        } catch (Exception e) {
            log(Level.DEBUG, "Error while tracking resets & wall time:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    public void doRun() {
        Set<OpenedInstanceInfo> currentOpenInstances = InstanceChecker.getAllOpenedInstances();

        Set<String> currentOpenInstancePaths = currentOpenInstances.stream()
                .map(i -> i.instancePath.toString())
                .collect(Collectors.toSet());

        HashSet<String> closedInstancePaths = new HashSet<>(previousOpenInstancePaths);
        closedInstancePaths.removeAll(currentOpenInstancePaths);

        for (String closedInstancePath : closedInstancePaths) {
            if (instanceWatcherMap.containsKey(closedInstancePath)) {
                // close old watchers (this instance was just closed)
                instanceWatcherMap.get(closedInstancePath).stop();
                instanceWatcherMap.remove(closedInstancePath);
                log(Level.DEBUG, "Closed a FileWatcher for instance: " + closedInstancePath);
            }
        }

        for (OpenedInstanceInfo instance : currentOpenInstances) {
            String path = instance.instancePath.toString();
            if (!instanceWatcherMap.containsKey(path)) {
                Path savesPath = Paths.get(path, "saves");

                log(Level.DEBUG, "Starting a new FileWatcher for instance: " + path);

                SavesFolderWatcher watcher = new SavesFolderWatcher(savesPath);
                WorldBopperUtil.runAsync("saves-folder-watcher", watcher);
                instanceWatcherMap.put(path, watcher);
            }
        }

        previousOpenInstancePaths.clear();
        previousOpenInstancePaths.addAll(currentOpenInstancePaths);
    }

}
