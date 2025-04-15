package command;

import model.StationData;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ScanCommand implements Runnable {
    private final double minTemp;
    private final double maxTemp;
    private final char letter;
    private final String outputFile;
    private final String jobName;
    private final ExecutorService executorService;
    private final String directoryPath;

    public ScanCommand(double minTemp, double maxTemp, char letter, String outputFile, String jobName,
                       ExecutorService executorService, String directoryPath) {
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.letter = letter;
        this.outputFile = outputFile;
        this.jobName = jobName;
        this.executorService = executorService;
        this.directoryPath = directoryPath;
    }

    @Override
    public void run() {
        File dir = new File(directoryPath);
        System.out.println("Tražim fajlove u direktorijumu: " + directoryPath);

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") || name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            System.out.println("Nema dostupnih fajlova za obradu.");
            return;
        }

        List<Future<File>> futures = new ArrayList<>();

        for (File file : files) {
            futures.add(executorService.submit(() -> processFile(file)));
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (Future<File> future : futures) {
                File tempFile = future.get();
                appendToOutputFile(tempFile, writer);
                tempFile.delete();
            }
            System.out.println("SCAN komanda (" + jobName + ") je završena. Rezultati su u " + outputFile);
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.out.println("Greška pri pisanju rezultata: " + e.getMessage());
        }
    }

    private File processFile(File file) throws IOException {
        File tempFile = File.createTempFile("scan_" + jobName + "_", ".tmp");

        boolean isCSV = file.getName().toLowerCase().endsWith(".csv");
        boolean firstLine = true;

        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             BufferedWriter tempWriter = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (isCSV && firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(";");
                if (parts.length < 2) continue;

                String stationName = parts[0].trim();
                try {
                    double temperature = Double.parseDouble(parts[1].trim());

                    if (stationName.charAt(0) == letter && temperature >= minTemp && temperature <= maxTemp) {
                        tempWriter.write(stationName + ";" + temperature);
                        tempWriter.newLine();
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Preskačem neispravnu liniju u " + file.getName() + ": " + line);
                }
            }
        }

        return tempFile;
    }

    private void appendToOutputFile(File tempFile, BufferedWriter writer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}
