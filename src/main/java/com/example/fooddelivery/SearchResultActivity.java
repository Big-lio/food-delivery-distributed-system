package com.example.fooddelivery;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddelivery.model.Store;

import java.util.List;
public class SearchResultActivity extends AppCompatActivity{
    private ListView resultList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        resultList = findViewById(R.id.resultList);

        List<Store> stores = (List<Store>) getIntent().getSerializableExtra("stores");

        if (stores != null && !stores.isEmpty()) {
            StoreAdapter adapter = new StoreAdapter(this, stores);
            resultList.setAdapter(adapter);

            resultList.setOnItemClickListener((parent, view, position, id) -> {
                Store selectedStore = stores.get(position);
                Intent intent = new Intent(this, StoreDetailActivity.class);
                intent.putExtra("store", selectedStore);
                startActivity(intent);
            });
        } else {
            Toast.makeText(this, "No stores found", Toast.LENGTH_SHORT).show();
        }

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }
}
