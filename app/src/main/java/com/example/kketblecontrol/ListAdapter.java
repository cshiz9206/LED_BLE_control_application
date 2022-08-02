package com.example.kketblecontrol;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ListAdapter extends BaseAdapter {

    Context mContext = null;
    LayoutInflater mLayoutInflater = null;
    ArrayList<DeviceData> devices;

    public ListAdapter(Context context, ArrayList<DeviceData> data) {
        mContext = context;
        devices = data;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public DeviceData getItem(int position) {
        return devices.get(position);
    }

    @Override
    public View getView(int position, View converView, ViewGroup parent) {
        View view = mLayoutInflater.inflate(R.layout.activity_scan, null);

        //ImageView imageView = (ImageView)view.findViewById(R.id.poster);
        TextView deviceName = (TextView)view.findViewById(R.id.device_name);
        TextView deviceAddr = (TextView)view.findViewById(R.id.device_address);

        //imageView.setImageResource(sample.get(position).getPoster());
        deviceName.setText(devices.get(position).getName());
        deviceAddr.setText(devices.get(position).getAddr());

        return view;
    }
}