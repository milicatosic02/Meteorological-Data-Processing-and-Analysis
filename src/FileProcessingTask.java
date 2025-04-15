import model.StationData;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class FileProcessingTask implements Runnable {
    private final String filePath;
    private final Map<Character, StationData> stationsMap;
    private final ReentrantLock lock;


    public FileProcessingTask(String filePath,
                              Map<Character, StationData> stationsMap,
                              ReentrantLock lock) {
        this.filePath = filePath;
        this.stationsMap = stationsMap;
        this.lock = lock;
    }

    @Override
    public void run() {
        boolean isCSV = filePath.toLowerCase().endsWith(".csv");
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isCSV && firstLine) {
                    firstLine = false;
                    continue; // Preskoči zaglavlje u CSV fajlu
                }

                String[] parts = line.split(";");
                if (parts.length != 2) {
                    System.err.println("Preskačem neispravnu liniju u fajlu " + filePath + ": " + line);
                    continue;
                }

                String stationName = parts[0].trim();
                double temperature;

                try {
                    temperature = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException e) {
                    System.err.println("Preskačem neispravnu temperaturu u fajlu " + filePath + ": " + parts[1]);
                    continue;
                }

                if (stationName.isEmpty()) continue;
                char firstLetter = Character.toLowerCase(stationName.charAt(0));

                lock.lock();
                try {
                    stationsMap.compute(firstLetter, (key, data) -> {
                        if (data == null) {
                            return new StationData(1, temperature);
                        } else {
                            data.addData(temperature);
                            return data;
                        }
                    });
                } finally {
                    lock.unlock();
                }
            }
        } catch (IOException e) {
            System.err.println("Greška pri čitanju fajla '" + filePath + "': " + e.getMessage());
        }
    }


}
