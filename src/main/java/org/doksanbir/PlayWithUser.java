package org.doksanbir;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


record User(UUID id, String name, String email) {
}

class UserRequest {
    private final String requestJson;

    public UserRequest(String json) {
        this.requestJson = json;
    }

    public String getRequestJson() {
        return requestJson;
    }
}

@Slf4j
class UserService {

    // Generates a new user ID
    public static UUID generateUserId() {
        return UUID.randomUUID();
    }

    // Builds a request for user creation
    public static UserRequest buildUserCreationRequest(UUID userId, String name, String email) {
        String json = String.format("{\"id\":\"%s\", \"name\":\"%s\", \"email\":\"%s\"}", userId, name, email);
        return new UserRequest(json);
    }

    // Simulates the creation of a user on the server
    public static String createUser(UserRequest creationRequest) {
        // Simulate sending the creation request to the server and getting a response
        // For simulation, it just logs and returns a success response
        log.info("Creating user with request: {}", creationRequest.getRequestJson());
        return "User creation successful for request: " + creationRequest.getRequestJson();
    }

    // Builds a request to fetch user details
    public static UserRequest buildUserFetchRequest(UUID userId) {
        String json = String.format("{\"id\":\"%s\"}", userId);
        return new UserRequest(json);
    }

    // Simulates fetching user data from the server
    public static User fetchUser(UserRequest fetchRequest) {
        // Simulate fetching the user details based on the request
        // Normally, you'd parse the fetchRequest.getRequestJson() to extract the ID and fetch the user
        // For the simulation, it just logs and returns a new User object
        log.info("Fetching user with request: {}", fetchRequest.getRequestJson());
        // The details would be set based on the actual data fetched from the server
        return new User(UUID.randomUUID(), "John Doe", "john.doe@example.com");
    }
}


@Slf4j
public class PlayWithUser {

    /*
     * CompletableFuture and Virtual Thread Explanation:
     *
     * CompletableFuture:
     * - CompletableFuture is a class in Java for writing asynchronous, non-blocking code.
     * - It represents the result of an asynchronous operation that will complete in the future.
     * - It is used to handle operations asynchronously, particularly useful for I/O tasks.
     * - CompletableFuture can be completed with a value or an exception, and supports fluent chaining of asynchronous tasks.
     *
     * Asynchronous Execution in the Provided Code:
     * - Creating a CompletableFuture:
     *   CompletableFuture<User> userFuture = new CompletableFuture<>();
     *   - This creates a CompletableFuture instance that will hold the result of the user creation and fetching operation.
     *
     * - Starting a Virtual Thread:
     *   Thread.startVirtualThread(() -> { ... });
     *   - This initiates a virtual thread, part of Project Loom, which are lightweight threads managed by the Java runtime.
     *   - Virtual threads reduce overhead compared to traditional OS threads, allowing more concurrency.
     *   - The virtual thread performs potentially time-consuming operations like network calls.
     *
     * - Completing the CompletableFuture:
     *   - If user creation is successful, fetch the user and complete the CompletableFuture with the user object.
     *   - If user creation fails, complete the CompletableFuture exceptionally to indicate an error.
     *
     * - Handling Exceptions:
     *   - The try-catch block is used to handle any exceptions during the user creation and fetching process.
     *   - If an exception occurs, the CompletableFuture is completed exceptionally with that exception.
     *
     * - Retrieving the Result:
     *   User user3 = userFuture.get();
     *   - The get() method is a blocking call that waits for the CompletableFuture to complete and retrieves its result.
     *   - If the future completed exceptionally, get() will throw an ExecutionException.
     *
     * - Logging the Result:
     *   - After the asynchronous operation completes, the user details are logged.
     */
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // Generate a new user ID
        UUID userId = UserService.generateUserId();

        // Build a user creation request and create the user
        UserRequest creationRequest = UserService.buildUserCreationRequest(userId, "John Doe", "john.doe@example.com");
        String creationResponse = UserService.createUser(creationRequest);
        log.info(creationResponse);

        // Build a user fetch request and fetch the user
        UserRequest fetchRequest = UserService.buildUserFetchRequest(userId);
        User user = UserService.fetchUser(fetchRequest);

        // Log the fetched user details
        log.info("Fetched User ID: {}", user.id());
        log.info("Fetched User Name: {}", user.name());


        UUID userId2 = UserService.generateUserId();

        // Chain CompletableFuture methods to asynchronously create a user and then fetch the user
        CompletableFuture<User> future = CompletableFuture.supplyAsync(
                        () -> UserService.buildUserCreationRequest(userId2, "John Doe", "john.doe@example.com"))
                .thenApply(UserService::createUser) // Assuming createUser now returns UserRequest
                .exceptionally(ex -> {
                    log.error("Error during user creation", ex);
                    return "{}"; // Return an empty JSON or some error JSON structure
                })
                .thenApply(json -> {
                    if (json.equals("{}")) {
                        return new UserRequest("{}"); // Return an empty UserRequest in case of error
                    }
                    // Build fetch request from the creation response (which would normally include user details in a real application)
                    return UserService.buildUserFetchRequest(userId2);
                })
                .thenApply(UserService::fetchUser); // Assuming fetchUser now takes UserRequest and returns User

        try {
            User user2 = future.get(); // This can throw checked exceptions
            // Log the fetched user details
            log.info("Fetched User ID: {}", user2.id());
            log.info("Fetched User Name: {}", user2.name());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving the user", e);
        }

        // Asynchronous operation using a virtual thread - Generate a unique identifier for a new user.
        UUID userId3 = UserService.generateUserId();

        CompletableFuture<User> userFuture = new CompletableFuture<>(); // Create a CompletableFuture. This will be used to handle the result of the asynchronous operation.

        Thread.startVirtualThread(() -> { // Start a new virtual thread. Virtual threads are lightweight threads managed by the Java runtime rather than the operating system.
            try {
                // Build a request to create a new user with specified details.
                UserRequest creationRequest2 = UserService.buildUserCreationRequest(userId3, "Jane Doe", "jane.doe@example.com");
                // Attempt to create a new user on the server and get the response.
                String creationResponse2 = UserService.createUser(creationRequest2);
                if (!creationResponse2.equals("{}")) { // Check if the user creation was successful.

                    // Build a request to fetch the details of the newly created user.
                    UserRequest fetchRequest2 = UserService.buildUserFetchRequest(userId3);

                    // Fetch the details of the newly created user.
                    User user3 = UserService.fetchUser(fetchRequest2);

                    // Successfully complete the CompletableFuture with the fetched user.
                    userFuture.complete(user3);
                } else {
                    // If user creation was not successful, complete the CompletableFuture exceptionally.
                    userFuture.completeExceptionally(new RuntimeException("User creation failed"));
                }
            } catch (Exception ex) {
                // If an exception occurs during the process, complete the CompletableFuture exceptionally.
                userFuture.completeExceptionally(ex);
            }
        }); // End of the virtual thread's task.

        // Wait for the virtual thread to complete and get the user
        User user3 = userFuture.get();
        log.info("Fetched User ID: {}", user3.id());
        log.info("Fetched User Name: {}", user3.name());



        Set<User> users = ConcurrentHashMap.newKeySet();
        AtomicInteger count = new AtomicInteger();

        // User operations 1,000,000 times
        var userThreads = IntStream.range(0, 1_000_000)
                .mapToObj(i -> Thread.startVirtualThread(() -> {
                    UUID userId5 = UserService.generateUserId();
                    UserRequest creationRequest5 = UserService.buildUserCreationRequest(userId5, "John Doe", "john.doe@example.com");
                    UserService.createUser(creationRequest5);
                    UserRequest fetchRequest5 = UserService.buildUserFetchRequest(userId5);
                    User user5 = UserService.fetchUser(fetchRequest5);
                    users.add(user5);
                    count.incrementAndGet();
                }))
                .collect(Collectors.toList());

        Instant begin = Instant.now();

        // Join all user threads
        for (var thread : userThreads) {
            thread.join();
        }

        Instant end = Instant.now();
        log.info("Duration for users: {} ms", java.time.Duration.between(begin, end).toMillis());
        log.info("Total users processed: {}", count.get());
    }
}


