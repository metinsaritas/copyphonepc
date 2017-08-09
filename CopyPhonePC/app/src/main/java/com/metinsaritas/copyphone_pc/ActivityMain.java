package com.metinsaritas.copyphone_pc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class ActivityMain extends AppCompatActivity implements ClipboardManager.OnPrimaryClipChangedListener {

    private ClipboardManager clipboardManager;
    private Socket socket;
    String lastCopied = "";
    boolean otherCopying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(this);

        try {
            socket = IO.socket("http://calisma.herokuapp.com/");
            socket.on("otherCopied", otherCopied);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ActivityMain.this, "Connected", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(ActivityMain.this, "Error", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(ActivityMain.this, "Disconnected", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

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
    };

    @Override
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
        socket.emit("phoneCopied", json);

    }

}