package command;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

public class StartCommand implements Runnable {
    private final ExecutorService executorService;
    private final BlockingQueue<String> commandQueue;
    private final boolean loadJobs;

    public StartCommand(ExecutorService executorService, BlockingQueue<String> commandQueue, boolean loadJobs) {
        this.executorService = executorService;
        this.commandQueue = commandQueue;
        this.loadJobs = loadJobs;
    }

    @Override
    public void run() {
        if (loadJobs) {
            loadSavedJobs();
        }
        System.out.println("Sistem je pokrenut.");
    }

    private void loadSavedJobs() {
        File file = new File("load_config.txt");
        if (!file.exists()) {
            System.out.println("Nema sačuvanih poslova za učitavanje.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    commandQueue.offer(line); // Dodajemo komandu u red
                    System.out.println("Učitana komanda: " + line);
                    count++;
                }
            }

            if (count == 0) {
                System.out.println("Fajl za učitavanje je prazan.");
            } else {
                System.out.println("Učitano " + count + " sačuvanih poslova.");
            }

        } catch (IOException e) {
            System.out.println("Greška pri učitavanju poslova: " + e.getMessage());
        }
    }
}
