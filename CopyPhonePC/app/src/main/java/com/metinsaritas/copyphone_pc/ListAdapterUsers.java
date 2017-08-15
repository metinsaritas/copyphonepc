package com.metinsaritas.copyphone_pc;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

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
        return users.size();
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

        TextView tvLvUserName = row.findViewById(R.id.tvLvUserName);
        ImageView ivLvFrom = row.findViewById(R.id.ivLvFrom);
        ToggleButton tbUsersActive = row.findViewById(R.id.tbUsersActive);


        final User user = getItem(i);
        boolean activeStatus = !Room.Settings.containsKey(user.id);
        tbUsersActive.setChecked(activeStatus);

        if (user.id.equals(ActivityFirst.userId)) {
            tbUsersActive.setVisibility(View.INVISIBLE);
            tvLvUserName.setTextColor(context.getResources().getColor(R.color.colorAccent));
        }

        tbUsersActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!b) {/*off*/
                    Room.Settings.put(user.id,false);
                } else {/*on*/
                    Room.Settings.remove(user.id);
                }
            }
        });
        tvLvUserName.setText(user.Name);

        int ua_icon = R.drawable.ua_other;
        if (user.from.equals("android")) ua_icon = R.drawable.ua_android;
        else if (user.from.equals("chrome")) ua_icon = R.drawable.ua_chrome;
        else if (user.from.equals("edge")) ua_icon = R.drawable.ua_edge;
        else if (user.from.equals("firefox")) ua_icon = R.drawable.ua_firefox;
        else if (user.from.equals("ios")) ua_icon = R.drawable.ua_ios;
        else if (user.from.equals("mac")) ua_icon = R.drawable.ua_mac;
        else if (user.from.equals("safari")) ua_icon = R.drawable.ua_safari;

        ivLvFrom.setImageResource(ua_icon);
        return row;
    }
}
