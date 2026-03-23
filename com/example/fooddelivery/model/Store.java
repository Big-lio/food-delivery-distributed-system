package com.example.fooddelivery.model;

import java.io.Serializable;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Store implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("StoreName")
    private String storeName;

    @JsonProperty("Latitude")
    private double latitude;

    @JsonProperty("Longitude")
    private double longitude;

    @JsonProperty("FoodCategory")
    private String foodCategory;

    @JsonProperty("Stars")
    private double stars;

    @JsonProperty("NoOfVotes")
    private int noOfVotes;

    @JsonProperty("StoreLogo")
    private String storeLogo;

    @JsonProperty("Products")
    private List<Product> products;

    private String accuracyCategory;

    private int totalSales = 0;

    private double revenue=0;

    public Store(
        @JsonProperty("StoreName") String storeName,
        @JsonProperty("Latitude") double latitude,
        @JsonProperty("Longitude") double longitude,
        @JsonProperty("FoodCategory") String foodCategory,
        @JsonProperty("Stars") double stars,
        @JsonProperty("NoOfVotes") int noOfVotes,
        @JsonProperty("StoreLogo") String storeLogo,
        @JsonProperty("Products") List<Product> products,
        String accuracyCategory
    ) {
        this.storeName = storeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foodCategory = foodCategory;
        this.stars = stars;
        this.noOfVotes = noOfVotes;
        this.storeLogo = storeLogo;
        this.products = products;
        this.accuracyCategory = accuracyCategory;
    }

    // Getters & Setters

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getFoodCategory() { return foodCategory; }
    public void setFoodCategory(String foodCategory) { this.foodCategory = foodCategory; }

    public double getStars() { return stars; }
    public void setStars(double stars) { this.stars = stars; }

    public int getNoOfVotes() { return noOfVotes; }
    public void setNoOfVotes(int noOfVotes) { this.noOfVotes = noOfVotes; }

    public String getStoreLogo() { return storeLogo; }
    public void setStoreLogo(String storeLogo) { this.storeLogo = storeLogo; }

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }

    public String getAccuracyCategory() { return accuracyCategory; }
    
    public void calculateAccuracyCategory() {
        if (products == null || products.isEmpty()) {
            this.accuracyCategory = "$";
            return;
        }
    
        double total = 0;
        for (Product p : products) {
            total += p.getPrice();
        }
        double average = total / products.size();
    
        if (average <= 5) {
            this.accuracyCategory = "$";
        } else if (average <= 15) {
            this.accuracyCategory = "$$";
        } else {
            this.accuracyCategory = "$$$";
        }
    }

    public int getTotalSales() {
        return totalSales;
    }
    
    public void addSales(int amount) {
        this.totalSales += amount;
    }
    
    public boolean reduceStock(String productName, int quantity) {
        for (Product p : products) {
            if (p.getProductName().equalsIgnoreCase(productName)) {
                if (p.getAvailableAmount() >= quantity) {
                    p.setAvailableAmount(p.getAvailableAmount() - quantity);
                    p.addQuantitySold(quantity);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false; 
    }
    
    public double getPrice(String productName) {
        for (Product p : products) {
            if (p.getProductName().equalsIgnoreCase(productName)) {
                return p.getPrice();
            }
        }
        return 0.0; 
    }
    
    public void addRating(int starsGiven) {
        if (starsGiven >= 1 && starsGiven <= 5) {
            double totalRating = (this.stars * this.noOfVotes) + starsGiven;
            this.noOfVotes++;
            this.stars = totalRating / this.noOfVotes;
        }
    }

    public void addRevenue(double rev){
        revenue = this.revenue + rev;
    }

    public double getRevenue(){
        return this.revenue;
    }
    
    
}

                
