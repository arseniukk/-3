import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class PairwiseSum {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);

        // Введення розміру та значень масиву
        System.out.println("Введiть розмiр масиву:");
        int size = scanner.nextInt();
        System.out.println("Введiть мiнiмальне значенння:");
        int minValue = scanner.nextInt();
        System.out.println("Введiть максимальне значенння:");
        int maxValue = scanner.nextInt();

        // Генерація масиву
        int[] array = new Random().ints(size, minValue, maxValue + 1).toArray();
        System.out.println("Згенерований масив: " + Arrays.toString(array));

        // Версія Work Stealing
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        long startTimeStealing = System.nanoTime();

        PairwiseSumTask task = new PairwiseSumTask(array, 0, array.length - 1);
        int resultStealing = forkJoinPool.invoke(task);

        long endTimeStealing = System.nanoTime();
        System.out.println("Work Stealing результат: " + resultStealing);
        System.out.println("Час виконання (Work Stealing): " + (endTimeStealing - startTimeStealing) / 1_000_000 + " ms");

        // Версія Work Dealing з використанням ExecutorService
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        long startTimeDealing = System.nanoTime();

        int resultDealing = calculateWithExecutorService(array, executorService);
        
        long endTimeDealing = System.nanoTime();
        System.out.println("Work Dealing результат: " + resultDealing);
        System.out.println("Час виконанння (Work Dealing): " + (endTimeDealing - startTimeDealing) / 1_000_000 + " ms");

        executorService.shutdown();
    }

    // Версія Work Dealing
    public static int calculateWithExecutorService(int[] array, ExecutorService executorService) throws InterruptedException, ExecutionException {
        int threshold = 100;
        int tasksCount = (array.length + threshold - 1) / threshold;
        Future<Integer>[] futures = new Future[tasksCount];

        for (int i = 0; i < tasksCount; i++) {
            final int start = i * threshold;
            final int end = Math.min(start + threshold, array.length - 1);
            futures[i] = executorService.submit(() -> {
                int sum = 0;
                for (int j = start; j < end; j++) {
                    sum += array[j] + array[j + 1];
                }
                return sum;
            });
        }

        int totalSum = 0;
        for (Future<Integer> future : futures) {
            totalSum += future.get();
        }
        return totalSum;
    }

    // Версія Work Stealing через ForkJoinPool
    static class PairwiseSumTask extends RecursiveTask<Integer> {
        private final int[] array;
        private final int start;
        private final int end;
        private static final int THRESHOLD = 100;

        PairwiseSumTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Integer compute() {
            if ((end - start) <= THRESHOLD) {
                int sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i] + array[i + 1];
                }
                return sum;
            } else {
                int mid = (start + end) / 2;
                PairwiseSumTask leftTask = new PairwiseSumTask(array, start, mid);
                PairwiseSumTask rightTask = new PairwiseSumTask(array, mid + 1, end);
                leftTask.fork(); // Запуск підзадачі
                return rightTask.compute() + leftTask.join();
            }
        }
    }
}
