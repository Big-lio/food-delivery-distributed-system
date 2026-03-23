package com.example.fooddelivery;

import android.content.Intent;
import android.util.Log;

import android.os.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddelivery.model.Product;
import com.example.fooddelivery.model.Store;

import java.io.*;
import java.util.*;
import java.net.Socket;


public class MainActivity extends AppCompatActivity {

    private EditText latitudeInput, longitudeInput;
    private Spinner categorySpinner;
    private RatingBar ratingBar;
    private CheckBox price1, price2, price3;
    private Button searchButton;
    private ListView resultList;

    private final String masterHost = "10.0.2.2"; // Emulator's loopback to localhost
    private final int masterPort = 5000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeInput = findViewById(R.id.editText_latitude);
        longitudeInput = findViewById(R.id.editText_longitude);
        categorySpinner = findViewById(R.id.spinner_category);
        ratingBar = findViewById(R.id.ratingBar);
        price1 = findViewById(R.id.checkbox_price1);
        price2 = findViewById(R.id.checkbox_price2);
        price3 = findViewById(R.id.checkbox_price3);
        searchButton = findViewById(R.id.button_search);

        // Populate category spinner manually or dynamically from backend later
        String[] categories = {"Select","pizzeria", "burger", "sushi"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);


        searchButton.setOnClickListener(v -> performSearch());
    }

    private void performSearch() {
        double latitude = Double.parseDouble(latitudeInput.getText().toString());
        double longitude = Double.parseDouble(longitudeInput.getText().toString());
        String selectedCategory = categorySpinner.getSelectedItem().toString();
        double minStars = (double) ratingBar.getRating();

        if (selectedCategory.equals("Select")) {
            Toast.makeText(this, "Please select food category", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedPrices = new ArrayList<>();
        if (price1.isChecked()) selectedPrices.add("$");
        if (price2.isChecked()) selectedPrices.add("$$");
        if (price3.isChecked()) selectedPrices.add("$$$");

        // You can support multiple price ranges if needed
        String priceRange = selectedPrices.isEmpty() ? "$" : selectedPrices.get(0);

        Map<String, Object> filters = new HashMap<>();
        filters.put("Latitude", latitude);
        filters.put("Longitude", longitude);
        filters.put("FoodCategories", Collections.singletonList(selectedCategory));
        filters.put("MinStars", (int) minStars);
        filters.put("PriceRange", priceRange);


        Map<String, Object> request = new HashMap<>();
        request.put("type", "SEARCH");
        request.put("filters", filters);

        new SearchTask().execute(request);
    }

    private class SearchTask extends AsyncTask<Map<String, Object>, Void, List<Store>> {
        @Override
        protected List<Store> doInBackground(Map<String, Object>... params) {
            List<Store> stores = new ArrayList<>();
            try (Socket socket = new Socket(masterHost, masterPort);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Send request to MasterServer
                out.writeObject(params[0]);
                out.flush();

                // Recieve response from MasterServer
                try {
                    Object response = in.readObject();
                    if (response instanceof List<?>) {
                        List<?> results = (List<?>) response;
                        for (Object store : results) {
                            Store storeCasted = (Store) store;
                            stores.add(storeCasted);
                        }
                    }
                } catch (Exception e) {
                    Log.e("Error", "Exception while reading object: " + e.getMessage());
                    e.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return stores;
        }

        @Override
        protected void onPostExecute(List<Store> result) {
            super.onPostExecute(result);

            Intent intent = new Intent(MainActivity.this, SearchResultActivity.class);
            intent.putExtra("stores", (Serializable) result);
            startActivity(intent);
        }

    }
}
