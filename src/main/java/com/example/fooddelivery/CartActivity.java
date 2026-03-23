package com.example.fooddelivery;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.example.fooddelivery.model.Product;
import java.util.Map;
import android.os.*;
import android.widget.*;
import java.net.Socket;
import android.app.AlertDialog;
import android.widget.RatingBar;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;


public class CartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        ListView cartListView = findViewById(R.id.cartListView);
        TextView totalTextView = findViewById(R.id.totalTextView);

        // Πάρε τα στοιχεία του καλαθιού από το Intent
        HashMap<String, Integer> cart = (HashMap<String, Integer>) getIntent().getSerializableExtra("cart");
        ArrayList<Product> allProducts = (ArrayList<Product>) getIntent().getSerializableExtra("allProducts");

        List<String> cartDisplay = new ArrayList<>();
        double total = 0.0;

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String name = entry.getKey();
            int quantity = entry.getValue();

            for (Product p : allProducts) {
                if (p.getProductName().equalsIgnoreCase(name)) {
                    double price = p.getPrice() * quantity;
                    total += price;
                    cartDisplay.add(name + " x" + quantity + " = $" + String.format("%.2f", price));
                    break;
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cartDisplay);
        cartListView.setAdapter(adapter);
        totalTextView.setText("Total: $" + String.format("%.2f", total));

        Button confirmOrderButton = findViewById(R.id.confirmOrderButton);
        String storeName = getIntent().getStringExtra("storeName");

        confirmOrderButton.setOnClickListener(v -> {
            List<Map<String, Object>> cartItems = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("productName", entry.getKey());
                item.put("quantity", entry.getValue());
                cartItems.add(item);
            }

            Map<String, Object> orderRequest = new HashMap<>();
            UUID orderId = UUID.randomUUID();

            orderRequest.put("type", "ORDER");
            orderRequest.put("storeName", storeName);
            orderRequest.put("cart", cartItems);
            orderRequest.put("orderId", orderId);

            new OrderTask().execute(orderRequest);
            showRatingDialog(storeName);
        });
    }

    private class OrderTask extends AsyncTask<Map<String, Object>, Void, String> {
        @Override
        protected String doInBackground(Map<String, Object>... params) {
            try (Socket socket = new Socket("10.0.2.2", 5000);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject(params[0]);
                out.flush();

                Object response = in.readObject();

                if (response instanceof Map) {
                    Map<?, ?> responseMap = (Map<?, ?>) response;
                    String status = (String) responseMap.get("status");
                    UUID orderId = (UUID) responseMap.get("orderId");

                    return "Order " + orderId + ": " + status;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "Error sending order: " + e.getMessage();
            }
            return "Unexpected response from server.";
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(CartActivity.this, result, Toast.LENGTH_LONG).show();
        }
    }

    private void showRatingDialog(String storeName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rate the store");

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.activity_rating, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBarD);

        builder.setView(dialogView);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            int rating = (int) ratingBar.getRating();
            if (rating > 0) {
                sendRatingToMaster(storeName, rating);
            } else {
                Toast.makeText(this, "Skipped rating", Toast.LENGTH_SHORT).show();
            }

            // Επιστροφή στην αρχική οθόνη
            Intent intent = new Intent(CartActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("Skip", (dialog, which) -> {
            Toast.makeText(this, "Skipped rating", Toast.LENGTH_SHORT).show();

            // Επιστροφή στην αρχική οθόνη
            Intent intent = new Intent(CartActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        builder.show();
    }



    private void sendRatingToMaster(String storeName, int rating) {
        new Thread(() -> {
            try (Socket socket = new Socket("10.0.2.2", 5000);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                Map<String, Object> ratingRequest = new HashMap<>();
                ratingRequest.put("type", "RATING");
                ratingRequest.put("storeName", storeName);
                ratingRequest.put("rating", rating);

                out.writeObject(ratingRequest);
                out.flush();

                Object response = in.readObject();
                if (response instanceof String) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Server response: " + response, Toast.LENGTH_LONG).show()
                    );
                }

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error sending rating: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }




}

