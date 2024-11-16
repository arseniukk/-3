import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class FileCounter {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);

        // Введення шляху до директорії і формату файлів
        System.out.println("Введiть шлях директорiї:");
        String directoryPath = scanner.nextLine();
        System.out.println("Введiть розширення файлiв (наприклад, .pdf):");
        String extension = scanner.nextLine();

        // Перевірка чи директорія існує
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Вказана директорія не існує або це не директорія.");
            return;
        }

        // Ініціалізація ExecutorService
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Integer>> results = new ArrayList<>();
        long startTime = System.nanoTime();

        // Перевірка наявності файлів у директорії
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("Директорія порожня.");
            executorService.shutdown();
            return;
        }

        // Додавання завдань у пул потоків
        for (File file : files) {
            if (file.isDirectory()) {
                results.add(executorService.submit(new FileCountCallable(file, extension)));
            } else if (file.getName().endsWith(extension)) {
                results.add(executorService.submit(() -> 1));
            } else {
                results.add(executorService.submit(() -> 0));
            }
        }

        // Обчислення результатів
        int count = 0;
        for (Future<Integer> future : results) {
            count += future.get();
        }
        executorService.shutdown();

        long endTime = System.nanoTime();
        System.out.println("Work Dealing результат: " + count);
        System.out.println("Час виконання (Work Dealing): " + (endTime - startTime) / 1_000_000 + " ms");
    }

    // Callable для рекурсивного підрахунку файлів у директорії
    static class FileCountCallable implements Callable<Integer> {
        private final File directory;
        private final String extension;

        FileCountCallable(File directory, String extension) {
            this.directory = directory;
            this.extension = extension;
        }

        @Override
        public Integer call() {
            int count = 0;
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    count += new FileCountCallable(file, extension).call();
                } else if (file.getName().endsWith(extension)) {
                    count++;
                }
            }
            return count;
        }
    }
}
