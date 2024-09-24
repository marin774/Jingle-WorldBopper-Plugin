package me.marin.worldbopperplugin.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorldBopperUtil {

    public static void runAsync(String threadName, Runnable runnable) {
        new Thread(runnable, threadName).start();
    }

    public static void runTimerAsync(Runnable runnable, int delayMs) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(runnable, 0, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Attempts to read a file 50 times with 10ms delay in between.
     * This is done to prevent some rare race conditions where file is being modified while it's trying to be read, which returns an empty string.
     * @param path  Path to the file
     * @return File contents in a string, UTF_8 charset; null if the file is no longer available
     * @throws FileStillEmptyException If the file is still empty after 50 attempts.
     */
    public static String readFile(Path path) throws FileStillEmptyException {
        int attempts = 0;
        while (attempts < 50) {
            try {
                if (!Files.exists(path)) {
                    return null;
                }
                byte[] text = Files.readAllBytes(path);
                if (text.length > 0) {
                    return new String(text, StandardCharsets.UTF_8);
                }
                Thread.sleep(10);
            } catch (IOException | InterruptedException e) {
                Jingle.log(Level.DEBUG, "(WorldBopper) Could not read " + path.getFileName() + ":\n" + ExceptionUtil.toDetailedString(e));
            }
            attempts++;
        }
        throw new FileStillEmptyException("file is still empty after 50 attempts.");
    }

}
