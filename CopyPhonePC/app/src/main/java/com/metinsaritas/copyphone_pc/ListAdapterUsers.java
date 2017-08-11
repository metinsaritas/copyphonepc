package com.metinsaritas.copyphone_pc;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 11-Aug-17.
 */

public class ListAdapterUsers extends BaseAdapter {

    private ArrayList<User> users;

    private Context context;
    private LayoutInflater inflater;
    public ListAdapterUsers(Context context, ArrayList<User> users) {
        this.context = context;
        this.users = users;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return users.size()+10;
    }

    @Override
    public User getItem(int i) {
        return users.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View row = inflater.inflate(R.layout.listview_users, null);

        TextView tvLvUsersDeneme = row.findViewById(R.id.tvLvUsersDeneme);
        tvLvUsersDeneme.setText("Deneme alani");
        return row;
    }
}
