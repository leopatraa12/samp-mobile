package com.aurora.launcher.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.aurora.game.R;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.aurora.game.SAMP;
import com.aurora.launcher.util.SampQueryAPI;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private Handler handler = new Handler(Looper.getMainLooper());

    private TextView serverName;
    private TextView serverInfo;

    private FirebaseAnalytics analytics;

    private FrameLayout rootContainer;

    private View homeView;
    private View currentScreen;

    private EditText nickname;

    private ConstraintLayout playBtn;

    private Wini wini;

    // ---------------- ON CREATE ----------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullscreen();
        setContentView(R.layout.launcher_home_activity);

        analytics = FirebaseAnalytics.getInstance(this);

        rootContainer = findViewById(android.R.id.content);

        initHomeView();
        com.aurora.launcher.util.ConfigValidator.validateConfigFiles(this);
        loadNickname();
        setupClicks();
        loadServerInfo();
    }

    // ---------------- HOME INIT ----------------

    private void initHomeView() {

        nickname = findViewById(R.id.name);

        playBtn = findViewById(R.id.play_btn);

        serverName = findViewById(R.id.server_name);

        serverInfo = findViewById(R.id.server_info_text);
    }

    // ---------------- CLICK SYSTEM ----------------

    private void setupClicks() {

        if (playBtn != null) {
            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handlePlay();
                }
            });
        }
    }

    // ---------------- PLAY ----------------

    private void handlePlay() {

        String nick = nickname.getText().toString().trim();

        if (!nick.contains("_")) {
            Toast.makeText(this,
                    "Use nome_sobrenome",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        hideKeyboard(this);

        if (isDataUpdateAvailable()) {
            Intent i = new Intent(this, LoadGameActivity.class);
            startActivity(i);
        } else {
            Intent i = new Intent(this, SAMP.class);
            startActivity(i);
        }
    }

    // ---------------- SERVER INFO ----------------

    private void loadServerInfo() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    String ip = "135.148.164.122";
                    int port = 21450;

                    File file = new File(getExternalFilesDir(null), "SAMP/settings.ini");
                    if (file.exists()) {
                        try {
                            Wini w = new Wini(file);
                            String h = w.get("client", "host");
                            String p = w.get("client", "port");
                            if (h != null && !h.trim().isEmpty()) {
                                ip = h.trim();
                            }
                            if (p != null && !p.trim().isEmpty()) {
                                try {
                                    port = Integer.parseInt(p.trim());
                                } catch (NumberFormatException ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }

                    SampQueryAPI query = new SampQueryAPI(ip, port);
                    String[] info = query.mo7164b();

                    if (info == null) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                serverName.setText("Servidor Offline");
                                serverInfo.setText("0/0");
                            }
                        });

                        return;
                    }

                    final String hostname = info[3];
                    final String players = info[1];
                    final String maxPlayers = info[2];

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            serverName.setText(hostname);
                            serverInfo.setText(players + "/" + maxPlayers);

                        }
                    });

                } catch (Exception e) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            serverName.setText("Servidor Offline");
                            serverInfo.setText("0/0");
                        }
                    });

                    Log.e(TAG, "query error", e);
                }
            }
        }).start();
    }

    // ---------------- NICK ----------------

    private void loadNickname() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    File file = new File(getExternalFilesDir(null),
                            "SAMP/settings.ini");

                    if (file.exists()) {

                        wini = new Wini(file);
                        final String nick = wini.get("client", "name");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (nick != null) {
                                    nickname.setText(nick);
                                }
                            }
                        });
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            nickname.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                                @Override
                                public void afterTextChanged(Editable s) {
                                    saveNickname(s.toString());
                                }
                            });

                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "nick error", e);
                }
            }
        }).start();
    }

    private void saveNickname(final String nick) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    File file = new File(getExternalFilesDir(null),
                            "SAMP/settings.ini");

                    if (!file.exists()) {
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }

                    Wini wini = new Wini(file);
                    String host = wini.get("client", "host");
                    String portStr = wini.get("client", "port");
                    if (host == null || host.isEmpty() || portStr == null || portStr.isEmpty()) {
                        wini.put("client", "host", "135.148.164.122");
                        wini.put("client", "port", "21450");
                    }
                    wini.put("client", "name", nick);
                    wini.store();

                } catch (IOException e) {
                    Log.e(TAG, "save nick error", e);
                }
            }
        }).start();
    }

    // ---------------- FULLSCREEN ----------------

    private void setFullscreen() {

        Window w = getWindow();

        w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            w.setDecorFitsSystemWindows(false);
        }
    }

    // ---------------- KEYBOARD ----------------

    private boolean isDataUpdateAvailable() {

        try {

            File versionFile = new File(getExternalFilesDir(null),
                    "version.txt");

            if (!versionFile.exists()) {
                return true; // nunca instalou data
            }

            // versão local
            int localVersion = Integer.parseInt(
                    new java.util.Scanner(versionFile).nextLine()
            );

            // versão do servidor (hardcoded por enquanto)
            int serverVersion = 2;

            return serverVersion > localVersion;

        } catch (Exception e) {
            return true;
        }
    }

    public static void hideKeyboard(Activity a) {

        InputMethodManager imm =
                (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);

        View v = a.getCurrentFocus();

        if (v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}