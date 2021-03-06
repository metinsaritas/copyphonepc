package com.metinsaritas.copyphone_pc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ActivityFirst extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ClipboardManager.OnPrimaryClipChangedListener, CompoundButton.OnCheckedChangeListener {

    //public static String SOCKET_URL = "http://"+"10.240.10.88"+":3000/";
    public static String SOCKET_URL = "http://calisma.herokuapp.com/";

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private NavigationView navigationView;

    private ClipboardManager clipboardManager;
    private Socket socket;
    private MyNotification myNotification;
    public SharedPreferences sharedPreferences;
    public SharedPreferences.Editor editor;

    private ToggleButton tbPanelSetRemote;
    private ToggleButton tbPanelGetRemote;


    private String lastCopied = "";
    private String receiveCopied = "";


    private ListAdapterUsers adapterUsers;
    private ListView lvMainUsers;
    private EditText etMainRoom;
    private Button btnMainConnect;
    private EditText etMainArea;
    private LinearLayout llMainHolder;
    private boolean connectedRoom = false;
    private User me = new User();
    private EditText etPanelUserName;

    private ArrayList<User> userList = new ArrayList<User>();
    private RequestRoom requestRoom;
    public static String userId;
    private DrawerLayout drawer;

    private ListView lvAllCopiedCopy;
    private ListAdapterCopy adapterCopy;
    private ArrayList<Copy> copyList = new ArrayList<Copy>();

    private EditText etPanelMaxCopiedCount;
    private AdView adView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        MobileAds.initialize(getApplicationContext(),
                "ca-app-pub-1030319386229981~3757619445");

        adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);


        startService(new Intent(ActivityFirst.this, ServiceRemoveNotification.class));

        initalizeComponent();
        initalizeSocket();


        RemoteViews remoteViews = myNotification.getRemoteViews();
        remoteViews.setTextViewText(R.id.tvNotificationRoomName, "Not connected");

        myNotification.showNotify();

    }

    private void initalizeSocket() {
        try {
            socket = IO.socket(SOCKET_URL);
            socket.on("otherCopied", otherCopied);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            myNotification.getRemoteViews().setImageViewResource(R.id.ivNotificationStatus, R.drawable.status_blue);
                            myNotification.showNotify();
                            JSONObject jsonObject = new JSONObject();
                            String roomName = sharedPreferences.getString("roomName",null);
                            boolean hasRoom = roomName != null;
                            try {
                                jsonObject.put("from","android");
                                jsonObject.put("hasRoom", hasRoom);
                                jsonObject.put("roomName", roomName);
                                jsonObject.put("name", etPanelUserName.getText().toString());
                                jsonObject.put("clickConnect", false);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnMainConnect.setEnabled(false);
                                    btnMainConnect.setText("Connecting");
                                    etMainRoom.setEnabled(false);
                                    llMainHolder.setVisibility(View.INVISIBLE);
                                }
                            });

                            sendToSocket("connectRoom", jsonObject);
                        }
                    });

                }
            });


            socket.on("connectRoom", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject jsonObject;
                    String roomName = "";
                    int member = 4;
                    try {
                        jsonObject = (JSONObject) args[0];
                        roomName = jsonObject.getString("roomName");
                        userId = jsonObject.getJSONObject("user").getString("userId");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    myNotification.getRemoteViews().setImageViewResource(R.id.ivNotificationStatus, R.drawable.status_green);
                    myNotification.getRemoteViews().setTextViewText(R.id.tvNotificationRoomName, roomName);
                    myNotification.showNotify();
                    final String finalRoomName = roomName;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            etMainRoom.setText(finalRoomName);
                            etMainRoom.setEnabled(true);
                            btnMainConnect.setEnabled(true);
                            btnMainConnect.setText("Disconnect");
                            connectedRoom = true;
                            etMainRoom.setEnabled(false);
                            llMainHolder.setVisibility(View.VISIBLE);
                        }
                    });

                    editor.putString("roomName", roomName);
                    editor.commit();
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
                            llMainHolder.setVisibility(View.INVISIBLE);
                            etMainRoom.setEnabled(true);
                            btnMainConnect.setEnabled(true);
                            btnMainConnect.setText("Connect");
                            connectedRoom = false;
                        }
                    });
                }
            });

            socket.on("roomUpdated", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (args.length <= 0) {
                        return;
                    }

                    JSONObject jsonObject;
                    try {
                        jsonObject = (JSONObject) args[0];
                        Gson gson = new Gson();
                        requestRoom = gson.fromJson(jsonObject.toString(), RequestRoom.class);
                        userList = requestRoom.users;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapterUsers = new ListAdapterUsers(getApplicationContext(), userList);
                                lvMainUsers.setAdapter(adapterUsers);
                                myNotification.getRemoteViews().setTextViewText(R.id.tvNotificationMemberCount,""+userList.size());
                                myNotification.showNotify();
                            }
                        });
                    }
                    catch (Exception e) {
                        Log.d("Soket", e.getMessage());
                    }
                }
            });

            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void initalizeComponent() {
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
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        etPanelUserName.setText(sharedPreferences.getString("name","User"));
                    }
                });
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        myNotification = new MyNotification(ActivityFirst.this);

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPreferences.edit();

        tbPanelSetRemote = findViewById(R.id.tbPanelSetRemote);
        tbPanelSetRemote.setOnCheckedChangeListener(this);
        tbPanelSetRemote.setChecked(sharedPreferences.getBoolean("tbPanelSetRemote",true));

        tbPanelGetRemote = findViewById(R.id.tbPanelGetRemote);
        tbPanelGetRemote.setOnCheckedChangeListener(this);
        tbPanelGetRemote.setChecked(sharedPreferences.getBoolean("tbPanelGetRemote",true));

        me.Name = sharedPreferences.getString("name","User");
        etPanelUserName = findViewById(R.id.etPanelUserName);
        etPanelUserName.setText(me.Name);

        adapterCopy = new ListAdapterCopy(getApplicationContext(), copyList);

        etPanelMaxCopiedCount = findViewById(R.id.etPanelMaxCopiedCount);

    }

    Emitter.Listener otherCopied = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (args.length <= 0) {
                return;
            }

            final Object json = args[0];

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                        JSONObject jsonObject;
                        try {
                            jsonObject = (JSONObject) json;
                            String copiedText = jsonObject.getString("copiedText");
                            String sender = jsonObject.getString("sender");

                            Copy copy = new Copy();
                            copy.copiedText = copiedText;
                            MyValidator.Validate(copy);
                            copyList.add(0,copy);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapterCopy.notifyDataSetChanged();
                                }
                            });
                            //listeye ekle listviewi güncelle

                            if (tbPanelGetRemote.isChecked()) {
                                boolean active = RequestRoom.Settings.containsKey(sender);
                                if (!active) {
                                    receiveCopied = copiedText;
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("text", copiedText));
                                }
                            }
                        }
                        catch (Exception e) {
                            Log.d("Soket", e.getMessage());
                        }

                }
            });

        }
    }; @Override
    public void onPrimaryClipChanged() {
        Log.d("Soket","onPrimaryClipChanged");

        if (!tbPanelSetRemote.isChecked()) return;


        String copiedText = clipboardManager.getText().toString();
        if (copiedText.length() <= 0)
            return;

        if (receiveCopied.equals(copiedText)) return;
        receiveCopied = copiedText;

        Copy copy = new Copy();
        copy.copiedText = copiedText;
        MyValidator.Validate(copy);
        copyList.add(0,copy);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapterCopy.notifyDataSetChanged();
            }
        });


        JSONObject json = new JSONObject();
        try {
            json.put("copiedText", copiedText);
            json.put("from","phone");
            sendCopied(json);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void sendCopied(JSONObject json) throws JSONException {
        if (socket == null || json == null || !socket.connected()) return;
        String copiedText = json.getString("copiedText");
        if (lastCopied.equals(copiedText)) return;
        lastCopied = copiedText;

        sendToSocket("dataCopied", json);

    }

    private boolean sendToSocket(String event, JSONObject json) {
        if (socket == null || json == null || !socket.connected()) return false;
        socket.emit(event, json);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_first,menu);
        return true;
    }

    public void clickSetting(MenuItem item) {
        drawer.openDrawer(Gravity.END);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton.getId() == R.id.tbPanelSetRemote) {
            editor.putBoolean("tbPanelSetRemote", b);
            editor.commit();
        } else if (compoundButton.getId() == R.id.tbPanelGetRemote) {
            editor.putBoolean("tbPanelGetRemote", b);
            editor.commit();
        }
    }

    public void clickConnect(View view) {
        JSONObject jsonObject = new JSONObject();
        if (connectedRoom) { /*Log out*/
            try {
                jsonObject.put("from","phone");
                jsonObject.put("roomName", etMainRoom.getText().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendToSocket("disconnectRoom", jsonObject);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    llMainHolder.setVisibility(View.INVISIBLE);
                    btnMainConnect.setText("Connect");
                    connectedRoom = false;
                    etMainRoom.setEnabled(true);
                }
            });
            return;
        }

        if (!socket.connected()) {
            socket.connect();//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
            return;
        }

        try {
            jsonObject.put("from","android");
            jsonObject.put("hasRoom", true);
            jsonObject.put("roomName", etMainRoom.getText().toString());
            jsonObject.put("name", etPanelUserName.getText().toString());
            //jsonObject.put("clickConnect", true);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnMainConnect.setText("Connecting");
                    connectedRoom = true;
                    etMainRoom.setEnabled(false);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendToSocket("connectRoom", jsonObject);
    }

    public void clickCopy(View view) {
        if (etMainArea == null) return;

        clipboardManager.setPrimaryClip(ClipData.newPlainText("text", etMainArea.getText().toString()));
        etMainArea.setText("");
    }

    public void clickPaste(View view) {
        if (etMainArea == null) return;
        etMainArea.setText(clipboardManager.getText());
    }

    public void clickApplyUserName(View view) {
        String name = etPanelUserName.getText().toString();
        editor.putString("name", name);
        editor.commit();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendToSocket("changeName", jsonObject);
    }

    public void clickApplyMaxCopiedCount(View view) {
        int i = 5;
        i++;
        try {
            int count = Integer.parseInt(etPanelMaxCopiedCount.getText().toString());
            if (count >= 1)
                ListAdapterCopy.MAX_COPIED_COUNT = count;
            else
                etPanelMaxCopiedCount.setText(ListAdapterCopy.MAX_COPIED_COUNT + "");
        }
        catch (Exception e) {
            etPanelMaxCopiedCount.setText(ListAdapterCopy.MAX_COPIED_COUNT + "");
        }
    }

    public void clickToMail(View view) {
        // yapımcıya mail gönderme intentini araştır
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{getResources().getString(R.string.mail)});
            i.putExtra(Intent.EXTRA_TEXT   , "");
            try {
                startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(ActivityFirst.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e) {

        }
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
                rootView = inflater.inflate(R.layout.fragment_main, container, false);

                ActivityFirst activityFirst = (ActivityFirst) getActivity();
                activityFirst.lvMainUsers = rootView.findViewById(R.id.lvMainUsers);
                activityFirst.lvMainUsers.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                        return false;
                    }
                });

                activityFirst.lvMainUsers.setAdapter(activityFirst.adapterUsers);

                activityFirst.etMainRoom = rootView.findViewById(R.id.etMainRoom);
                activityFirst.etMainRoom.setText(activityFirst.sharedPreferences.getString("roomName",""));
                activityFirst.btnMainConnect = rootView.findViewById(R.id.btnMainConnect);
                activityFirst.etMainArea = rootView.findViewById(R.id.etMainArea);
                activityFirst.llMainHolder = rootView.findViewById(R.id.llMainHolder);
                activityFirst.llMainHolder.setVisibility(View.INVISIBLE);


            } else {
                rootView = inflater.inflate(R.layout.fragment_allcopied, container, false);
                ActivityFirst activityFirst = (ActivityFirst) getActivity();
                activityFirst.lvAllCopiedCopy = rootView.findViewById(R.id.lvAllCopiedCopy);
                activityFirst.lvAllCopiedCopy.setAdapter(activityFirst.adapterCopy);
                activityFirst.lvAllCopiedCopy.setOnItemLongClickListener(activityFirst.adapterCopy);
                activityFirst.lvAllCopiedCopy.setOnItemClickListener(activityFirst.adapterCopy);
            }
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
