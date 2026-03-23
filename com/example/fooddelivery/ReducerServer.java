package com.example.fooddelivery;

import com.example.fooddelivery.model.Product;
import com.example.fooddelivery.model.Store;
import java.io.*;

import java.net.*;
import java.util.*;

public class ReducerServer {

    private static final int PORT = 7000;
    private static final int EXPECTED_WORKERS = 2;

    
    private static final Map<UUID, List<Store>> resultsMap = new HashMap<>();

    
    private static final Map<UUID, Integer> responseCountMap = new HashMap<>();

    
    private static final Object lock = new Object();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));) {
            System.out.println("Reducer Listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ReducerHandler(socket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ReducerHandler implements Runnable {
        private final Socket socket;

        public ReducerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
            ) {
                Object input = ois.readObject();

                if (input instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) input;
                    UUID requestId = (UUID) data.get("requestId");
                    List<Store> partialResults = (List<Store>) data.get("results");

                    synchronized (lock) {
                        resultsMap.putIfAbsent(requestId, new ArrayList<>());
                        responseCountMap.put(requestId, responseCountMap.getOrDefault(requestId, 0) + 1);
                        resultsMap.get(requestId).addAll(partialResults);

                        System.out.println("[Reducer] Received partial result (" +
                            responseCountMap.get(requestId) + "/" + EXPECTED_WORKERS + ") for request: " + requestId);

                        lock.notifyAll();
                    }
                    oos.writeObject("ACK");
                    oos.flush();

                } else if (input instanceof UUID) {
                    UUID requestId = (UUID) input;
                    List<Store> finalResult;

                    synchronized (lock) {
                        while (!responseCountMap.containsKey(requestId) ||
                                responseCountMap.get(requestId) < EXPECTED_WORKERS) {
                            lock.wait();
                        }

                        finalResult = resultsMap.getOrDefault(requestId, new ArrayList<>());
                        resultsMap.remove(requestId);
                        responseCountMap.remove(requestId);
                    }

                    oos.writeObject(finalResult);
                    oos.flush();

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                }

            } catch (Exception e) {
                System.err.println("[Reducer] Handler exception: " + e.getMessage());
            }
        }
    }


}

