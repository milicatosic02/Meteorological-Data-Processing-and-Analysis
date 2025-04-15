import model.StationData;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

public class MonitoringDirectoryThread implements Runnable {
    private final String directoryPath;
    private final Map<String, Long> fileLastModifiedMap;
    private final ExecutorService executorService;
    private final Map<Character, StationData> stationsMap;
    private final ReentrantLock fileLock;
    private volatile boolean running = true;

    public MonitoringDirectoryThread(String directoryPath,
                                     ExecutorService executorService,
                                     Map<Character, StationData> stationsMap,
                                     ReentrantLock fileLock) {
        this.directoryPath = directoryPath;
        this.executorService = executorService;
        this.stationsMap = stationsMap;
        this.fileLock = fileLock;
        this.fileLastModifiedMap = new ConcurrentHashMap<>();
    }


    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        WatchService watchService = null;
        try {
            Path dir = Paths.get(directoryPath);
            watchService = FileSystems.getDefault().newWatchService();
            dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

            // Prva obrada svih postojećih fajlova u direktorijumu
            File dirFile = new File(directoryPath);
            File[] initialFiles = dirFile.listFiles((f) ->
                    f.isFile() && (f.getName().endsWith(".txt") || f.getName().endsWith(".csv")));

            if (initialFiles != null) {
                for (File file : initialFiles) {
                    long lastModified = file.lastModified();
                    String absolutePath = file.getAbsolutePath();
                    fileLastModifiedMap.put(absolutePath, lastModified);
                    System.out.println("Inicijalna obrada: " + file.getName());
                    executorService.submit(new FileProcessingTask(absolutePath, stationsMap, fileLock));
                }
            }

            while (running) {
                WatchKey key = watchService.take(); // Blokira dok se nešto ne desi

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path changedFile = dir.resolve((Path) event.context());

                    if (!changedFile.toString().endsWith(".txt") && !changedFile.toString().endsWith(".csv")) {
                        continue;
                    }

                    File file = changedFile.toFile();
                    long lastModified = file.lastModified();
                    String absolutePath = file.getAbsolutePath();

                    System.out.println("Fajl: " + file.getName() + " je poslednji put modifikovan: " + new Date(lastModified));

                    if (!fileLastModifiedMap.containsKey(absolutePath) ||
                            fileLastModifiedMap.get(absolutePath) != lastModified) {

                        fileLastModifiedMap.put(absolutePath, lastModified);
                        System.out.println("Detektovana promena: " + changedFile + " [" + kind.name() + "]");
                        executorService.submit(new FileProcessingTask(absolutePath, stationsMap, fileLock));
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    System.err.println("Ključ direktorijuma više nije važeći.");
                    break;
                }
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Greška pri praćenju direktorijuma: " + e.getMessage());
        } finally {
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (IOException e) {
                    System.err.println("Greška pri zatvaranju WatchService: " + e.getMessage());
                }
            }
        }
    }
}
