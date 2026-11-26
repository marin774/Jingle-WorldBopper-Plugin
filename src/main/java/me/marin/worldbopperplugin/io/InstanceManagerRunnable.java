package me.marin.worldbopperplugin.io;

import me.marin.worldbopperplugin.WorldBopperTask;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.instance.InstanceChecker;
import xyz.duncanruns.jingle.instance.InstanceInfo;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static me.marin.worldbopperplugin.WorldBopperPlugin.log;

public class InstanceManagerRunnable implements Runnable {

    public static final Map<String, WorldBopperTask> instanceWatchersMap = new HashMap<>();

    private final HashSet<String> previousOpenInstancePaths = new HashSet<>();

    @Override
    public void run() {
        try {
            doRun();
        } catch (Exception e) {
            log(Level.DEBUG, "Error while tracking active instance:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    public void doRun() {
        Set<InstanceInfo> currentOpenInstances = new HashSet<>(InstanceChecker.getAllOpenedInstances());

        Set<String> currentOpenInstancePaths = currentOpenInstances.stream()
                .map(i -> i.instancePath.toString())
                .collect(Collectors.toSet());

        HashSet<String> closedInstancePaths = new HashSet<>(previousOpenInstancePaths);
        closedInstancePaths.removeAll(currentOpenInstancePaths);

        for (String closedInstancePath : closedInstancePaths) {
            if (instanceWatchersMap.containsKey(closedInstancePath)) {
                // close old watchers (this instance was just closed)
                instanceWatchersMap.remove(closedInstancePath).stop();

                log(Level.DEBUG, "Stopped WorldBopper for instance: " + closedInstancePath);
            }
        }

        for (InstanceInfo instance : currentOpenInstances) {
            String path = instance.instancePath.toString();
            if (!instanceWatchersMap.containsKey(path)) {
                Path savesPath = Paths.get(path, "saves");

                log(Level.DEBUG, "Starting WorldBopper for instance: " + path);

                WorldBopperTask worldBopperTask = new WorldBopperTask(savesPath);
                worldBopperTask.start();

                instanceWatchersMap.put(path, worldBopperTask);
            }
        }

        previousOpenInstancePaths.clear();
        previousOpenInstancePaths.addAll(currentOpenInstancePaths);
    }

}
