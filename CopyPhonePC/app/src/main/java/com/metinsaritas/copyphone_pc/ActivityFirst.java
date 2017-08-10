package com.metinsaritas.copyphone_pc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.CompoundButton;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class ActivityFirst extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ClipboardManager.OnPrimaryClipChangedListener, CompoundButton.OnCheckedChangeListener {
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private NavigationView navigationView;

    private ClipboardManager clipboardManager;
    private Socket socket;
    String lastCopied = "";
    boolean otherCopying = false;
    private MyNotification myNotification;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    ToggleButton tbPanelSetRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        myNotification = new MyNotification(ActivityFirst.this);
        RemoteViews remoteViews = myNotification.getRemoteViews();

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPreferences.edit();

        tbPanelSetRemote = findViewById(R.id.tbPanelSetRemote);
        tbPanelSetRemote.setOnCheckedChangeListener(this);

        try {
            socket = IO.socket("http://calisma.herokuapp.com/");
            socket.on("otherCopied", otherCopied);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ActivityFirst.this, "Connected", Toast.LENGTH_SHORT).show();
                            myNotification.getRemoteViews().setImageViewResource(R.id.ivNotificationStatus, R.drawable.status_green);
                            myNotification.showNotify();
                        }
                    });

                }
            });

            socket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ActivityFirst.this, "Error", Toast.LENGTH_SHORT).show();
                            myNotification.getRemoteViews().setImageViewResource(R.id.ivNotificationStatus, R.drawable.status_red);
                            myNotification.showNotify();
                        }
                    });
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ActivityFirst.this, "Disconnected", Toast.LENGTH_SHORT).show();
                            myNotification.getRemoteViews().setImageViewResource(R.id.ivNotificationStatus, R.drawable.status_red);
                            myNotification.showNotify();
                        }
                    });
                }
            });

            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        remoteViews.setTextViewText(R.id.tvNotificationRoomId, "Not connected");

        myNotification.showNotify();

    }

    Emitter.Listener otherCopied = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (args.length <= 0) {
                return;
            }

            JSONObject jsonObject;
            try {
                jsonObject = (JSONObject) args[0];
                String copiedText = jsonObject.getString("copiedText");
                otherCopying = true;
                clipboardManager.setPrimaryClip(ClipData.newPlainText("text", copiedText));
            }
            catch (Exception e) {
                Log.d("Soket", e.getMessage());
            }

        }
    }; @Override
    public void onPrimaryClipChanged() {
        Log.d("Soket","onPrimaryClipChanged");
        if (otherCopying) {
            otherCopying = false;
            return;
        }

        String copiedText = clipboardManager.getText().toString();
        if (copiedText.length() <= 0)
            return;

        JSONObject json = new JSONObject();
        try {
            json.put("copiedText", copiedText);
            json.put("from","phone");
            sendToSocket(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void sendToSocket(JSONObject json) throws JSONException {
        if (socket == null || json == null || !socket.connected()) return;
        String copiedText = json.getString("copiedText");
        if (lastCopied.equals(copiedText)) return;
        lastCopied = copiedText;

        // ben kopyaladıysm emit et
        socket.emit("dataCopied", json);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myNotification.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_first,menu);
        return true;
    }

    public void clickSetting(MenuItem item) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.openDrawer(Gravity.END);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        Toast.makeText(this, "Tıklanıldı", Toast.LENGTH_SHORT).show();
    }

    public static class PlaceholderFragment extends Fragment {

        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView;
            int section = getArguments().getInt(ARG_SECTION_NUMBER);
            if (section == 1) {
                rootView = inflater.inflate(R.layout.fragment_activity_first, container, false);
            } else {
                rootView = inflater.inflate(R.layout.fragment_allcopied, container, false);
            }
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            //textview null geldifalan ife koyarak al
            return rootView;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
