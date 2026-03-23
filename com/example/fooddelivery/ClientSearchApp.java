package com.example.fooddelivery;

import com.example.fooddelivery.model.Product;
import com.example.fooddelivery.model.Store;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientSearchApp {

    public static void main(String[] args) {
        String masterHost = "localhost";
        int masterPort = 5000;
        Scanner scanner = new Scanner(System.in);

        try {
            while (true) {
                //  Prompt the user for search filters
                System.out.print("Enter your latitude: ");
                double latitude = Double.parseDouble(scanner.nextLine());

                System.out.print("Enter your longitude: ");
                double longitude = Double.parseDouble(scanner.nextLine());

                System.out.print("Enter preferred food categories (comma-separated, e.g., pizzeria,souvlaki): ");
                String[] categoriesInput = scanner.nextLine().split(",");
                List<String> categories = new ArrayList<>();
                for (String cat : categoriesInput) {
                    categories.add(cat.trim());
                }

                System.out.print("Enter minimum star rating (1-5): ");
                int minStars = Integer.parseInt(scanner.nextLine());

                System.out.print("Enter price range ($:for average price up to 5$, $$:for average price up to 15$, $$$:for average price more than 15$): ");
                String priceRange = scanner.nextLine();

                //  Create filter map
                Map<String, Object> filters = new HashMap<>();
                filters.put("Latitude", latitude);
                filters.put("Longitude", longitude);
                filters.put("FoodCategories", categories);
                filters.put("MinStars", minStars);
                filters.put("PriceRange", priceRange);

                Map<String, Object> request = new HashMap<>();
                request.put("type", "SEARCH");
                request.put("filters", filters);

                //  Open a new socket and send request to Master
                try (Socket socket = new Socket(masterHost, masterPort);
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                    out.writeObject(request);
                    out.flush();

                    //  Receive response
                    Object response = in.readObject();

                    if (response instanceof List<?>) {
                        List<?> stores = (List<?>) response;
                        System.out.println("\nMatching stores found:");
                        for (Object store : stores) {
                            Store storeCasted = (Store) store;
                            System.out.println("Store: " + storeCasted.getStoreName());
                            System.out.println("Stars: " + storeCasted.getStars() );

                            List<Product> products = storeCasted.getProducts();
                            if (products != null && !products.isEmpty()) {
                                System.out.println("  Products:");
                                for (Product product : products) {
                                    System.out.printf("    - %s ($%.2f) | Available: %d\n", product.getProductName(), product.getPrice(), product.getAvailableAmount());
                                }
                            } else {
                                System.out.println("  No products available.");
                            }

                            System.out.println();
                        }

                        // Store selection
                        System.out.print("\nEnter the name of the store you'd like to order from: ");
                        String selectedStoreName = scanner.nextLine();

                        Store selectedStore = null;
                        for (Object store : stores) {
                            Store storeCasted = (Store) store;
                            if (storeCasted.getStoreName().equalsIgnoreCase(selectedStoreName.trim())) {
                                selectedStore = storeCasted;
                                break;
                            }
                        }

                        if (selectedStore == null) {
                            System.out.println("Store not found. Please make sure you typed the name correctly.");
                            continue;
                        }

                        // Confirm selection
                        System.out.println("You selected: " + selectedStore.getStoreName());
                        System.out.println("Available products:");
                        for (Product product : selectedStore.getProducts()) {
                            System.out.printf("    - %s ($%.2f) | Available: %d\n", product.getProductName(), product.getPrice(), product.getAvailableAmount());
                        }

                        // buy
                        Map<String, Integer> cart = new HashMap<>();
                        List<Product> availableProducts = selectedStore.getProducts();

                        while (true) {
                            System.out.print("\nEnter product name to add to your cart (or type 'done' to finish): ");
                            String productName = scanner.nextLine().trim();

                            if (productName.equalsIgnoreCase("done")) {
                                break;
                            }

                            Product foundProduct = null;
                            for (Product product : availableProducts) {
                                if (product.getProductName().equalsIgnoreCase(productName)) {
                                    foundProduct = product;
                                    break;
                                }
                            }

                            if (foundProduct == null) {
                                System.out.println("Product not found. Please enter a valid product name.");
                                continue;
                            }

                            System.out.print("Enter quantity: ");
                            int quantity;
                            try {
                                quantity = Integer.parseInt(scanner.nextLine().trim());
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid number. Try again.");
                                continue;
                            }

                            if (quantity <= 0 || quantity > foundProduct.getAvailableAmount()) {
                                System.out.println("Invalid quantity. Must be between 1 and " + foundProduct.getAvailableAmount());
                                continue;
                            }

                            cart.put(foundProduct.getProductName(), quantity);
                            System.out.println("Added " + quantity + foundProduct.getProductName() + " to cart.");
                        }

                        //preview
                        if (cart.isEmpty()) {
                            System.out.println("No products were selected. Exiting.");
                            continue;
                        }

                        System.out.println("\n Your cart:");
                        double total = 0.0;
                        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                            String name = entry.getKey();
                            int quantity = entry.getValue();

                            for (Product p : availableProducts) {
                                if (p.getProductName().equalsIgnoreCase(name)) {
                                    double price = p.getPrice() * quantity;
                                    total += price;
                                    System.out.printf(" - %s x%d = $%.2f\n", name, quantity, price);
                                    break;
                                }
                            }
                        }
                        System.out.printf("Total: $%.2f\n", total);

                        //confirmation
                        while (true) {
                            System.out.print("\nDo you want to confirm the order? (yes/no): ");
                            String confirm = scanner.nextLine().trim().toLowerCase();

                            if (confirm.equals("yes")) {
                                System.out.println(" Order confirmed. Sending to Master...");

                                // proceed to sending order
                                List<Map<String, Object>> cartItems = new ArrayList<>();
                                for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                                    Map<String, Object> item = new HashMap<>();
                                    item.put("productName", entry.getKey());
                                    item.put("quantity", entry.getValue());
                                    cartItems.add(item);
                                }

                                UUID orderId = UUID.randomUUID();
                                Map<String, Object> orderRequest = new HashMap<>();
                                orderRequest.put("type", "ORDER");
                                orderRequest.put("storeName", selectedStore.getStoreName());
                                orderRequest.put("cart", cartItems);
                                orderRequest.put("orderId", orderId);


                                // Open new socket for the order request
                                try (Socket socketOrder = new Socket(masterHost, masterPort);
                                     ObjectOutputStream outOrder = new ObjectOutputStream(socketOrder.getOutputStream());
                                     ObjectInputStream inOrder = new ObjectInputStream(socketOrder.getInputStream())) {

                                    outOrder.writeObject(orderRequest);
                                    outOrder.flush();

                                    Object orderResponse = inOrder.readObject();
                                    if (orderResponse instanceof Map) {
                                        String status = (String) ((Map<?, ?>) orderResponse).get("status");
                                        UUID orderId3 = (UUID) ((Map<?, ?>) orderResponse).get("orderId");

                                        System.out.println("Order " + orderId3 + ": " + status);

                                    
                                        //rate
                                        System.out.print("Do you want to rate the store ? ((1-5) to rate or 0 to skip): ");
                                        int rating = -1;
                                    
                                        while (rating < 0 || rating > 5) {
                                            try {
                                                rating = Integer.parseInt(scanner.nextLine());
                                                if (rating == 0) {
                                                    System.out.println("Skipped rating.");
                                                    break;
                                                } else if (rating >= 1 && rating <= 5) {
                                                    Map<String, Object> ratingRequest = new HashMap<>();
                                                    ratingRequest.put("type", "RATING");
                                                    ratingRequest.put("storeName", selectedStore.getStoreName());
                                                    ratingRequest.put("rating", rating);
                                    
                                                    
                                                    try (Socket ratingSocket = new Socket(masterHost, masterPort);
                                                         ObjectOutputStream outRating = new ObjectOutputStream(ratingSocket.getOutputStream());
                                                         ObjectInputStream inRating = new ObjectInputStream(ratingSocket.getInputStream())) {
                                    
                                                        outRating.writeObject(ratingRequest);
                                                        outRating.flush();
                                    
                                                        Object ratingResponse = inRating.readObject();
                                                        if (ratingResponse instanceof String) {
                                                            System.out.println("Server response: " + ratingResponse);
                                                        }
                                    
                                                    } catch (Exception e) {
                                                        System.out.println("Failed to send rating: " + e.getMessage());
                                                    }
                                    
                                                } else {
                                                    System.out.print("Please enter a number between 1 and 5 (or 0 to skip): ");
                                                }
                                            } catch (NumberFormatException e) {
                                                System.out.print("Invalid input. Please enter a number (1-5 or 0 to skip): ");
                                            }
                                        }
                                    }
                                     else {
                                        System.out.println("Unexpected response from Master after sending order.");
                                    }
                                }


                                break;

                            } else if (confirm.equals("no")) {
                                System.out.println(" Restarting order process...");
                                cart.clear();
                                break;
                            } else {
                                System.out.println("Please answer with 'yes' or 'no'.");
                            }
                        }

                    } else {
                        System.out.println(" Invalid response received from server.");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(" An error occurred:");
            e.printStackTrace();
        }

        scanner.close();
    }
}


