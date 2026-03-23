package com.example.fooddelivery;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.fooddelivery.model.Product;
import com.example.fooddelivery.model.Store;
import java.util.Map;

public class StoreDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_detail);

        Store store = (Store) getIntent().getSerializableExtra("store");

        TextView nameView = findViewById(R.id.storeName);
        TextView categoryView = findViewById(R.id.storeCategory);
        TextView ratingView = findViewById(R.id.storeRating);
        TextView priceView = findViewById(R.id.storePrice);

        nameView.setText(store.getStoreName());
        categoryView.setText("Category: " + store.getFoodCategory());
        ratingView.setText("Rating: " + store.getStars() + " ★");
        priceView.setText("Price Range: " + store.getAccuracyCategory());

        ListView productListView = findViewById(R.id.productListView);
        List<Product> products = store.getProducts();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                convertProductsToStrings(products)
        );
        productListView.setAdapter(adapter);

        Map<String, Integer> cart = new HashMap<>();
        ProductAdapter p_adapter = new ProductAdapter(this, products, cart);
        productListView.setAdapter(p_adapter);

        Button viewCartBtn = findViewById(R.id.viewCartButton);
        viewCartBtn.setOnClickListener(v -> {
            Intent intent = new Intent(StoreDetailActivity.this, CartActivity.class);
            intent.putExtra("cart", new HashMap<>(cart)); // μεταβιβάζεις αντίγραφο
            intent.putExtra("allProducts", new ArrayList<>(products));
            intent.putExtra("storeName", store.getStoreName());
            startActivity(intent);
        });

    }

    private ArrayList<String> convertProductsToStrings(List<Product> products) {
        ArrayList<String> list = new ArrayList<>();
        for (Product p : products) {
            String line = p.getProductName() + " - " + p.getPrice() + "$ | Available: " + p.getAvailableAmount();
            list.add(line);
        }
        return list;
    }
}
