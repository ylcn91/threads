package org.doksanbir;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class PlayWithThread {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        Set<Contract> contracts = ConcurrentHashMap.newKeySet();

        //Instant start = Instant.now();

        long id = 12;
        for(int i = 0; i < 1_000_000; i++) {
            Json request = ContractService.buildContractRequest(id); // 10 ns
            String contractJson = ContractService.fetchContract(request); // 100 ms - 50 ms
            Contract contract = Json.unmarshal(contractJson); // 10 ns
            contracts.add(contract);
            log.info("Contract details: {},  {}", contract,i);

        }

        Json request = ContractService.buildContractRequest(id); // 10 ns
        String contractJson = ContractService.fetchContract(request); // 100 ms - 50 ms
        Contract contract = Json.unmarshal(contractJson); // 10 ns
        contracts.add(contract);

        //Instant end = Instant.now();

        //log.info("Contract details: {}", contracts.iterator().next());
        //log.info("Duration: {} in ms", java.time.Duration.between(start, end).toMillis());

        var future = CompletableFuture.supplyAsync(
                        () -> ContractService.buildContractRequest(id))
                .thenApply(
                        ContractService::fetchContract)
                .exceptionally(ex -> {
                    log.error("Error during contract processing", ex);
                    return "{}";
                })
                .thenApply(
                        Json::unmarshal);


        Contract contract2 = future.get();
        log.info("Contract details: {}", contract2);


        // Asynchronous operation using a virtual thread for contract3
        CompletableFuture<Contract> contract3Future = new CompletableFuture<>();

        Thread.startVirtualThread(() -> {
            try {
                Json request3 = ContractService.buildContractRequest(id);
                String contractJson3 = ContractService.fetchContract(request3);
                Contract contract3 = Json.unmarshal(contractJson3);
                contract3Future.complete(contract3);
            } catch (Exception ex) {
                contract3Future.completeExceptionally(ex);
            }
        });

        // Wait for the virtual thread to complete and get the contract
        Contract contract3 = contract3Future.get();
        log.info("Contract details: {}", contract3);


        AtomicInteger count = new AtomicInteger();

        // Contract operations 1,000,000 times
        var contractThreads = IntStream.range(0, 1_000_000)
                .mapToObj(i -> Thread.startVirtualThread(() -> {
                    Json request4 = ContractService.buildContractRequest(id);
                    String contractJson4 = ContractService.fetchContract(request4);
                    Contract contract4 = Json.unmarshal(contractJson4);
                    contracts.add(contract4);
                    count.incrementAndGet();
                }))
                .collect(Collectors.toList());

        Instant begin = Instant.now();

        // Join all contract threads
        for (var thread : contractThreads) {
            thread.join();
        }

        Instant end = Instant.now();
        log.info("Duration for contracts: {} in ms", java.time.Duration.between(begin, end).toMillis());
        log.info("Total contracts processed: {}", count.get());
    }

}

class ContractService {

    public static Json buildContractRequest(long id) {
        // Simply return a new Json object containing the id
        return new Json("{\"id\":" + id + "}");
    }

    public static String fetchContract(Json request) {
        // Pretend to fetch contract data, return a JSON string representation of a contract
        return "{\"id\":12, \"contractType\":\"Service Agreement\", \"details\":\"This is a mock contract details.\"}";
    }
}


class Json {
    private final String jsonString;

    public Json(String json) {
        this.jsonString = json;
    }

    // This method would convert a JSON string into a Contract object
    // For simplicity, we're not parsing the string but directly creating a new contract
    public static Contract unmarshal(String contractJson) {
        // In a real scenario, you'd parse the JSON to extract these values
        return new Contract(12, "Service Agreement", "This is a mock contract details.");
    }
}

@Data
class Contract {
    private long id;
    private String contractType;
    private String details;

    public Contract(long id, String contractType, String details) {
        this.id = id;
        this.contractType = contractType;
        this.details = details;
    }
}

