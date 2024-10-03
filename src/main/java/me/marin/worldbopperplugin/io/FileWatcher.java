package me.marin.worldbopperplugin.io;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static me.marin.worldbopperplugin.WorldBopperPlugin.log;

/**
 * Works with single files and directories.
 * Directories will receive updates when a file in the directory has been modified.
 */
public abstract class FileWatcher implements Runnable {

    /**
     * Some programs will fire two ENTRY_MODIFY events without the file actually changing, and
     * <a href="https://stackoverflow.com/questions/16777869/java-7-watchservice-ignoring-multiple-occurrences-of-the-same-event/25221600#25221600">this</a>
     * post explains it and addresses it, even though it's not the perfect solution.
     */
    private static final int DUPLICATE_UPDATE_PREVENTION_MS = 5;

    protected final String name;
    protected final File file;

    private WatchService watchService;

    public FileWatcher(String name, File file) {
        this.name = name;
        this.file = file;
    }

    @Override
    public void run() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            log(Level.ERROR, "Could not start WatchService:\n" + ExceptionUtil.toDetailedString(e));
            return;
        }

        try {
            this.file.toPath().register(watchService, ENTRY_MODIFY, ENTRY_CREATE);

            WatchKey watchKey;
            do {
                watchKey = watchService.take();

                Thread.sleep(DUPLICATE_UPDATE_PREVENTION_MS); // explained above

                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path path = ev.context();
                    if (path == null) {
                        // sometimes event context is null? not sure how, but it happened.
                        continue;
                    }
                    File updatedFile = new File(this.file, path.toString());

                    if (event.kind() == ENTRY_MODIFY) {
                        if (updatedFile.length() > 0) {
                            try {
                                handleFileUpdated(updatedFile);
                            } catch (Exception e) {
                                log(Level.ERROR, "Unhandled exception in '" + this.name + "':\n" + ExceptionUtil.toDetailedString(e));
                            }
                        }
                    }
                    if (event.kind() == ENTRY_CREATE) {
                        try {
                            handleFileCreated(updatedFile);
                        } catch (Exception e) {
                            log(Level.ERROR, "Unhandled exception in '" + this.name + "':\n" + ExceptionUtil.toDetailedString(e));
                        }
                    }
                }
            } while (watchKey.reset());
        } catch (ClosedWatchServiceException e) {
            // when stop method gets called, ClosedWatchServiceException is thrown, and file watcher should stop.
        } catch (IOException | InterruptedException e) {
            log(Level.ERROR, "Error while reading:\n" + ExceptionUtil.toDetailedString(e));
        } catch (Exception e) {
            log(Level.ERROR, "Unknown exception while reading:\n" + ExceptionUtil.toDetailedString(e));
        }
        log(Level.DEBUG, "FileWatcher was closed " + (name));
    }

    protected abstract void handleFileUpdated(File file);
    protected abstract void handleFileCreated(File file);
    protected void stop() {
        try {
            watchService.close();
        } catch (IOException e) {
            log(Level.ERROR, "Could not stop WatchService:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

}
