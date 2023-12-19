package org.doksanbir;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VirtualThreadExample {

    public static void main(String[] args) throws InterruptedException {
        Set<String> poolNames = ConcurrentHashMap.newKeySet();
        Set<String> threadNames = ConcurrentHashMap.newKeySet();

        var threads = IntStream.range(0, 1_000_000)
                .mapToObj(i -> Thread.startVirtualThread(() -> {
                    String poolName = readPoolName();
                    poolNames.add(poolName);
                    String workerName = readWorkerName();
                    threadNames.add(workerName);
                }))
                .collect(Collectors.toList());

        Instant begin = Instant.now();

        // Join all threads to ensure they all complete
        for (var thread : threads) {
            thread.join();
        }

        Instant end = Instant.now();
        System.out.println("Duration: " + java.time.Duration.between(begin, end));

        // Additional code to work with poolNames and threadNames
    }

    private static String readPoolName() {

        return "pool-" + Thread.currentThread().getId();
    }

    private static String readWorkerName() {

        return "worker-" + Thread.currentThread().getId();
    }
}

