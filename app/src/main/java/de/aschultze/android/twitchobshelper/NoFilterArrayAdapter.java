package de.aschultze.android.twitchobshelper;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.List;

import androidx.annotation.NonNull;

public class NoFilterArrayAdapter extends ArrayAdapter {
    public NoFilterArrayAdapter(@NonNull Context context, int resource, @NonNull List objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                return new FilterResults();
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

            }
        };
    }
}

