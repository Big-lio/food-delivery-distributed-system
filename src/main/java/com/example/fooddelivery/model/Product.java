package com.example.fooddelivery.model;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("ProductName")
    private String productName;

    @JsonProperty("ProductType")
    private String productType;

    @JsonProperty("AvailableAmount")
    private int availableAmount;

    @JsonProperty("Price")
    private double price;

    private int quantitySold = 0;

    public Product(
            @JsonProperty("ProductName") String productName,
            @JsonProperty("ProductType") String productType,
            @JsonProperty("AvailableAmount") int availableAmount,
            @JsonProperty("Price") double price
    ) {
        this.productName = productName;
        this.productType = productType;
        this.availableAmount = availableAmount;
        this.price = price;
    }



    // Getters & Setters

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public int getAvailableAmount() { return availableAmount; }
    public void setAvailableAmount(int availableAmount) { this.availableAmount = availableAmount; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantitySold() {return quantitySold;}
    public void addQuantitySold(int quantity) {
        this.quantitySold += quantity;
    }

}