package com.example.fooddelivery;

import com.example.fooddelivery.model.Product;
import com.example.fooddelivery.model.Store;

import java.io.*;
import java.net.*;
import java.util.*;

public class Manager {

    private static Map<String, Store> storeMap = new HashMap<>();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        System.out.println("====== Manager Console ======");
        System.out.print("Enter Master IP (e.g. 127.0.0.1): ");
        String masterIP = scanner.nextLine();

        System.out.print("Enter Master Port (e.g. 5000): ");
        int masterPort = Integer.parseInt(scanner.nextLine());

        boolean running = true;
        while (running) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Add Store (from JSON)");
            System.out.println("2. Add Product to Store");
            System.out.println("3. Remove Product from Store");
            System.out.println("4. Update Product Availability");
            System.out.println("5. View Sales by Store Category");
            System.out.println("6. View Sales by Product Category");
            System.out.println("7. Exit");
            System.out.print("Select option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    addStoreFromJson();
                    sendStoreToMaster(masterIP, masterPort);
                    break;
                case "2":
                    addProductToStore();
                    break;
                case "3":
                    removeProductFromStore();
                    break; 
                case "4":
                    System.out.print("Enter Store Name: ");
                    String storeName = scanner.nextLine();
            
            
                    List<Store> stores = fetchAllStoresFromMaster(masterIP, masterPort);
                    Store store = null;
            
                    for (Store s : stores) {
                        if (s.getStoreName().equalsIgnoreCase(storeName)) {
                            store = s;
                            break;
                        }
                    }
                
                    
                    if (store == null) {
                        System.out.println("Store not found.");
                        return;
                    }
                
                    List<Product> products = store.getProducts();
                    if (products == null || products.isEmpty()) {
                        System.out.println("This store has no products.");
                        return;
                    }
                
                    
                    System.out.println("Available Products in " + storeName + ":");
                    for (Product p : products) {
                        System.out.println("- " + p.getProductName() + " (" + p.getAvailableAmount() + " available, " + p.getPrice() + "$)");
                    }
                
                    
                    System.out.print("Enter Product Name to update availability: ");
                    String productName = scanner.nextLine();
                
                    Product productToUpdate = null;
                    for (Product p : products) {
                        if (p.getProductName().equalsIgnoreCase(productName)) {
                            productToUpdate = p;
                            break;
                        }
                    }
                
                    if (productToUpdate == null) {
                        System.out.println("Product not found in this store.");
                        return;
                    }
                
                    System.out.println("Current availability: " + productToUpdate.getAvailableAmount());
                    System.out.print("Enter amount to increase/decrease (e.g. +5 or -3): ");
                    String input = scanner.nextLine();
                
                    try {
                        int change = Integer.parseInt(input);
                        int newAmount = productToUpdate.getAvailableAmount() + change;
                        if (newAmount < 0) {
                            System.out.println("Resulting availability would be negative. Operation cancelled.");
                            return;
                        }
                        productToUpdate.setAvailableAmount(newAmount);
                        System.out.println("Updated availability: " + productToUpdate.getAvailableAmount());
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid number.");
                    }
                    break;  
                case "5":
                    System.out.print("Enter Store Food Category (e.g., pizzeria, burger): ");
                    String storeCategory = scanner.nextLine();

                    List<Store> storesByFood = fetchAllStoresFromMaster(masterIP, masterPort);

                    int totalSalesForCategory = 0;
                    for (Store s : storesByFood) {
                        if (s.getFoodCategory().equalsIgnoreCase(storeCategory)) {
                            totalSalesForCategory += s.getTotalSales();
                            System.out.println("\"" + s.getStoreName() + "\": " + s.getTotalSales());
                        }
                    }

                    System.out.println("Total sales for food category '" + storeCategory + "': " + totalSalesForCategory);
                    break;
                case "6":
                    System.out.print("Enter Product Category (e.g., salad, pizza, side, drink): ");
                    String productCategory = scanner.nextLine();
                
                    List<Store> storesByProduct = fetchAllStoresFromMaster(masterIP, masterPort);
                
                    int totalSalesForProductCategory = 0;
                    for (Store s : storesByProduct) {
                        int storeTotal = 0;
                        for (Product p : s.getProducts()) {
                            if (p.getProductType().equalsIgnoreCase(productCategory)) {
                                totalSalesForProductCategory += p.getQuantitySold(); 
                                storeTotal += p.getQuantitySold();
                            }
                        }
                        if (storeTotal > 0) {
                            System.out.println("\"" + s.getStoreName() + "\": " + storeTotal);
                        }
                    }
                
                    System.out.println("Total sales for product category '" + productCategory + "': " + totalSalesForProductCategory + " units sold.");
                    break;                            
                case "7":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }

        System.out.println("Exiting Manager.");
    }

    private static void addStoreFromJson() {
        System.out.print("Enter JSON file name (fileName.json): ");
        String jsonPath = scanner.nextLine();
        try {
            Store store = JsonToStoreParser.parseFromFile(jsonPath);
            store.calculateAccuracyCategory();
            storeMap.put(store.getStoreName(), store);
            System.out.println("Loaded store: " + store.getStoreName());
        } catch (Exception e) {
            System.err.println("Failed to load store: " + e.getMessage());
        }
    }

    private static void addProductToStore() {
        System.out.print("Enter Store Name to add product: ");
        String storeName = scanner.nextLine();

        Store store = storeMap.get(storeName);
        if (store == null) {
            System.out.println("Store not found.");
            return;
        }

        List<Product> products = store.getProducts();
        if (products == null || products.isEmpty()) {
            System.out.println("No products available in " + storeName);
            return;
        }
    
        System.out.println("Products in " + storeName + ":");
        for (Product p : products) {
            System.out.println("- " + p.getProductName() +
                    " | Type: " + p.getProductType() +
                    " | Amount: " + p.getAvailableAmount() +
                    " | Price: $" + p.getPrice());
        }
        System.out.println("Price Category: " + store.getAccuracyCategory());

        System.out.print("Enter product name: ");
        String name = scanner.nextLine();
        System.out.print("Enter product type: ");
        String type = scanner.nextLine();
        System.out.print("Enter available amount: ");
        int amount = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter price: ");
        double price = Double.parseDouble(scanner.nextLine());

        Product newProduct = new Product(name, type, amount, price);
        store.getProducts().add(newProduct);
        System.out.println("Product added to " + storeName);
        store.calculateAccuracyCategory();
    }

    private static void removeProductFromStore() {
        System.out.print("Enter Store Name to remove product from: ");
        String storeName = scanner.nextLine();

        Store store = storeMap.get(storeName);
        if (store == null) {
            System.out.println("Store not found.");
            return;
        }

        List<Product> products = store.getProducts();
        if (products == null || products.isEmpty()) {
            System.out.println("No products available in " + storeName);
            return;
        }
    
        System.out.println("Products in " + storeName + ":");
        for (Product p : products) {
            System.out.println("- " + p.getProductName() +
                    " | Type: " + p.getProductType() +
                    " | Amount: " + p.getAvailableAmount() +
                    " | Price: $" + p.getPrice());
        }
        System.out.println("Price Category: " + store.getAccuracyCategory());

        System.out.print("Enter product name to remove: ");
        String productName = scanner.nextLine();

        boolean removed = store.getProducts().removeIf(p -> p.getProductName().equalsIgnoreCase(productName));
        if (removed) {
            System.out.println("Product removed from " + storeName);
            store.calculateAccuracyCategory();
        } else {
            System.out.println("Product not found.");
        }
    }

    private static void sendStoreToMaster(String masterIP, int masterPort) {
        System.out.print("Enter Store Name to send: ");
        String storeName = scanner.nextLine();

        Store store = storeMap.get(storeName);
        if (store == null) {
            System.out.println("Store not found.");
            return;
        }

        try (Socket socket = new Socket(masterIP, masterPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(store);
            out.flush();
            System.out.println("Successfully sent store data to Master.");

            Object response = in.readObject();
            System.out.println("Response from Master: " + response.toString());

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error communicating with Master: " + e.getMessage());
        }
    }


    private static List<Store> fetchAllStoresFromMaster(String masterHost, int masterPort) {
        List<Store> allStores = new ArrayList<>();
        Map<String, Object> request = new HashMap<>();
        request.put("type", "GET_ALL_STORES");
    
        try (Socket socket = new Socket(masterHost, masterPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
    
            out.writeObject(request);
            out.flush();
    
            Object response = in.readObject();
            if (response instanceof List<?>) {
                allStores = (List<Store>) response;
            }
    
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error fetching stores from Master: " + e.getMessage());
        }
    
        return allStores;
    }
    
    
    
}


