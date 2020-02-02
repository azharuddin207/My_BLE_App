package com.example.mybleapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private static final String TAG = "MyAdapter";
    private ArrayList<BluetoothDevice> mDataset;
    private onDeviceListener mOnDeviceListener;

    public  MyAdapter(ArrayList<BluetoothDevice> mDataset, onDeviceListener onDeviceListener){
        this.mDataset = mDataset;
        this.mOnDeviceListener = onDeviceListener;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder  implements  View.OnClickListener{
        // each data item is just a string in this case
//        public TextView deviceAddress;
        public  TextView deviceName;
        onDeviceListener onDeviceListener;
        public MyViewHolder(View v, onDeviceListener onDeviceListener) {
            super(v);
//            deviceAddress= v.findViewById(R.id.deviceAddress);
            deviceName = v.findViewById(R.id.deviceName);
            this.onDeviceListener = onDeviceListener;

            v.setOnClickListener(this);
        }


        @Override
        public void onClick(View v) {
                onDeviceListener.onDeviceClick(getAdapterPosition());
        }
    }

    public interface  onDeviceListener {
        void onDeviceClick(int position);

    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(ArrayList<BluetoothDevice> myDataset, Runnable scanCallback) {
        this.mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent,
                                           int viewType) {
        // create a new view
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.my_text_view, parent, false);
        MyViewHolder viewHolder = new MyViewHolder(listItem, mOnDeviceListener);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
//        holder.deviceAddress.setText(mDataset.get(position).getAddress());
        holder.deviceName.setText(mDataset.get(position).getName());

        holder.deviceName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    final Intent intent = new Intent(String.valueOf(DeviceControlActivity.class));
            }
        });

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }



}