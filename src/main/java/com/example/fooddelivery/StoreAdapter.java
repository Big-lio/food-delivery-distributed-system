package com.example.fooddelivery;

import android.content.Context;
import android.view.*;
import android.widget.*;

import com.example.fooddelivery.model.Store;

import java.util.List;

public class StoreAdapter extends ArrayAdapter<Store> {

    public StoreAdapter(Context context, List<Store> stores) {
        super(context, 0, stores);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Store store = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.store_list_item, parent, false);
        }

        TextView nameText = convertView.findViewById(R.id.storeName);
        TextView starsText = convertView.findViewById(R.id.storeStars);

        nameText.setText(store.getStoreName());
        starsText.setText("Stars: " + String.format("%.1f", store.getStars()));

        return convertView;
    }
}
