package com.example.fooddelivery;


import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.*;
import android.os.*;
import android.widget.*;
import com.example.fooddelivery.model.Product;


import java.io.*;
import java.util.*;

public class ProductAdapter extends ArrayAdapter<Product> {
    private List<Product> productList;
    private Map<String, Integer> cart;

    public ProductAdapter(Context context, List<Product> products, Map<String, Integer> cart) {
        super(context, 0, products);
        this.productList = products;
        this.cart = cart;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Product product = productList.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.product_list_item, parent, false);
        }

        TextView name = convertView.findViewById(R.id.productName);
        TextView price = convertView.findViewById(R.id.productPrice);
        Button addBtn = convertView.findViewById(R.id.addToCartButton);

        name.setText(product.getProductName());
        price.setText(String.format("€%.2f", product.getPrice()));

        addBtn.setOnClickListener(v -> {
            // Εδώ μπορούμε να βάλουμε ένα popup για ποσότητα
            showQuantityDialog(product);
        });

        return convertView;
    }

    private void showQuantityDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter quantity for " + product.getProductName());

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Quantity (1-" + product.getAvailableAmount() + ")");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String inputText = input.getText().toString().trim();
            int quantity;

            try {
                quantity = Integer.parseInt(inputText);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (quantity <= 0 || quantity > product.getAvailableAmount()) {
                Toast.makeText(getContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            cart.put(product.getProductName(), quantity);
            Toast.makeText(getContext(), quantity + "x " + product.getProductName() + " added to cart", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}

