package cz.svoboda.telecatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by svoboda on 28.8.2014.
 */
public class CallAdapter extends ArrayAdapter<CallItem> {
    public CallAdapter(Context context, ArrayList<CallItem> calls) {
        super(context, R.layout.item_call,calls);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        CallItem call = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_call, parent, false);
        }

        // Lookup view for data population
        TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
        tvName.setText(call.DateTime.toString()+" - "+ call.PhoneNumber);
        // Return the completed view to render on screen
        return convertView;
    }
}
