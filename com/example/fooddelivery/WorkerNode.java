package com.example.fooddelivery;

import com.example.fooddelivery.model.Product;
import com.example.fooddelivery.model.Store;

import java.io.*;
import java.net.*;
import java.util.*;

public class WorkerNode {

    private static Map<String, Store> storeDataMap = new HashMap<>();
    private static final String REDUCER_HOST = "127.0.0.1"; //IPv4 if you use multiple laptops - "127.0.0.1" if you run everything on the same laptop
    private static final int REDUCER_PORT = 7000;


    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java WorkerNode <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Worker node started on port " + port);

            while (true) {
                Socket masterSocket = serverSocket.accept();
                System.out.println("Master connected from " + masterSocket.getInetAddress().getHostAddress());
                new Thread(new WorkerHandler(masterSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class WorkerHandler implements Runnable {
        private Socket masterSocket;

        public WorkerHandler(Socket socket) {
            this.masterSocket = socket;
        }

        @Override
        public void run() {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(masterSocket.getOutputStream());
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(masterSocket.getInputStream());

                Object incoming = ois.readObject();

                if (incoming instanceof Store) {
                    Store store = (Store) incoming;
                    String storeName = store.getStoreName();

                    System.out.println("Received store data for: " + storeName);
                    storeDataMap.put(storeName, store);
                    oos.flush();

                } else if (incoming instanceof Map) {
                    Map<String, Object> request = (Map<String, Object>) incoming;
                    if ("SEARCH".equals(request.get("type"))) {
                        Map<String, Object> filters = (Map<String, Object>) request.get("filters");
                        List<Store> matched = searchStores(filters);

                        // Send to Reducer
                        try (
                            Socket reducerSocket = new Socket(REDUCER_HOST, REDUCER_PORT);
                            ObjectOutputStream outToReducer = new ObjectOutputStream(reducerSocket.getOutputStream());
                            ObjectInputStream inFromReducer = new ObjectInputStream(reducerSocket.getInputStream());
                        ) {
                            Map<String, Object> reducerMessage = new HashMap<>();
                            reducerMessage.put("requestId", request.get("requestId"));
                            reducerMessage.put("results", matched);

                            outToReducer.writeObject(reducerMessage);
                            outToReducer.flush();
                            Object ack = inFromReducer.readObject();
                            if ("ACK".equals(ack)) {
                                System.out.println("Reducer acknowledged receipt of results for request " + request.get("requestId"));
                            }
                            System.out.println("Sent " + matched.size() + " matched stores for request " + request.get("requestId") + " to Reducer.");

                        } catch (Exception e) {
                            System.err.println("Failed to send data to Reducer.");
                            e.printStackTrace();
                        }

                    }else if ("ORDER".equals(request.get("type"))) {
                        String storeName = (String) request.get("storeName");
                        List<Map<String, Object>> cart = (List<Map<String, Object>>) request.get("cart");
                        UUID orderId = (UUID) request.get("orderId");

                        System.out.println("[ORDER] Received ORDER request (order id : " + orderId + ")");
                        System.out.println("[ORDER] Store name: " + storeName);
                        System.out.println("[ORDER] Cart contents: " + cart);

                    
                        Store store = storeDataMap.get(storeName);
                        if (store == null) {
                            oos.writeObject("Store not found.");
                            oos.flush();
                            return;
                        }
                    
                        boolean success = true;
                        StringBuilder statusMessage = new StringBuilder();
                        
                        
                        if (cart == null || cart.isEmpty()) {
                            oos.writeObject("Cart is empty.");
                            oos.flush();
                            return;
                        }
                    
                        synchronized (store) {
                            for (Map<String, Object> item : cart) {
                                String productName = (String) item.get("productName");
                                int quantity = (int) item.get("quantity");

                                boolean updated = store.reduceStock(productName, quantity);
                                if (!updated) {
                                    success = false;
                                    statusMessage.append("Not enough stock for ").append(productName).append(". ");
                                } else {
                                    double price = store.getPrice(productName);
                                    store.addSales(quantity);
                                    store.addRevenue(quantity * price);
                                }
                            }
                        }

                    
                        if (success) {
                            statusMessage.append("Order completed successfully.");
                        }

                        Map<String, Object> response = new HashMap<>();
                        response.put("status", statusMessage.toString());
                        response.put("orderId", orderId);
                    
                        oos.writeObject(response);
                        oos.flush();
                    }

                    else if ("RATING".equals(request.get("type"))) {
                        String storeName = (String) request.get("storeName");
                        int rating = (int) request.get("rating");
                    
                        Store store = storeDataMap.get(storeName);
                        if (store == null) {
                            oos.writeObject("Store not found.");
                            oos.flush();
                            return;
                        }
                    
                        store.addRating(rating);
                    
                        double updatedAverage = store.getStars();
                        oos.writeObject("Rating updated successfully. New average rating: " + updatedAverage);
                        oos.flush();
                    }

                    else if ("GET_STORES".equals(request.get("type"))) {
                        List<Store> storeList = new ArrayList<>(storeDataMap.values());
                        oos.writeObject(storeList);
                        oos.flush();
                    }
                    
                    
                    
                    else {
                        oos.writeObject(Collections.emptyList());
                        oos.flush();
                    }
                }

                ois.close();
                oos.close();
                masterSocket.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static List<Store> searchStores(Map<String, Object> filters) {
        List<Store> result = new ArrayList<>();
    
        double latitude = (double) filters.get("Latitude");
        double longitude = (double) filters.get("Longitude");
        List<String> categories = (List<String>) filters.get("FoodCategories");
        int minStars = (int) filters.get("MinStars");
        String priceRange = (String) filters.get("PriceRange");
    
        for (Store store : storeDataMap.values()) {
            double dist = Math.sqrt(Math.pow(store.getLatitude() - latitude, 2) + Math.pow(store.getLongitude() - longitude, 2));
            if (dist > 10.0) continue;
    
            
            boolean matchesCategory = categories.stream()
                .anyMatch(cat -> cat.equalsIgnoreCase(store.getFoodCategory()));
            if (!matchesCategory) continue;
    
            if (store.getStars() < minStars) continue;
    
            
            if (store.getAccuracyCategory() == null) {
                store.calculateAccuracyCategory();
            }
    
            if (!store.getAccuracyCategory().equals(priceRange)) continue;
    
            result.add(store);
        }
    
        return result;
    }
    
    
}



