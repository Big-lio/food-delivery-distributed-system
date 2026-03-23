package com.example.fooddelivery;

import com.example.fooddelivery.model.Product;
import com.example.fooddelivery.model.Store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonToStoreParser {

    public static Store parseFromFile(String fileName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(new File(fileName));

            String storeName = rootNode.get("StoreName").asText();
            double latitude = rootNode.get("Latitude").asDouble();
            double longitude = rootNode.get("Longitude").asDouble();
            String foodCategory = rootNode.get("FoodCategory").asText();
            int stars = rootNode.get("Stars").asInt();
            int noOfVotes = rootNode.get("NoOfVotes").asInt();
            String storeLogo = rootNode.get("StoreLogo").asText();

            List<Product> products = new ArrayList<>();
            for (JsonNode productNode : rootNode.get("Products")) {
                String productName = productNode.get("ProductName").asText();
                String productType = productNode.get("ProductType").asText();
                int availableAmount = productNode.get("AvailableAmount").asInt();
                double price = productNode.get("Price").asDouble();
                products.add(new Product(productName, productType, availableAmount, price));
            }

            
            double totalPrice = 0;
            for (Product product : products) {
                totalPrice += product.getPrice();
            }
            double averagePrice = totalPrice / products.size();

            
            String accuracyCategory;
            if (averagePrice <= 5) {
                accuracyCategory = "$";
            } else if (averagePrice <= 15) {
                accuracyCategory = "$$";
            } else {
                accuracyCategory = "$$$";
            }

            
            return new Store(storeName, latitude, longitude, foodCategory, stars, noOfVotes, storeLogo, products, accuracyCategory);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

