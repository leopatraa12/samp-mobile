package com.aurora.launcher.activities;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.aurora.game.R;

import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.firebase.analytics.FirebaseAnalytics;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;

public class LoadGameActivity extends AppCompatActivity {

    private ProgressBar downloadProgressBar;
    private TextView currentFileText;
    private TextView downloadProgressText;
    private VideoView introVideoView;
    private FirebaseAnalytics mFirebaseAnalytics;

    private static final String CACHE_URL = "https://www.dropbox.com/scl/fi/u8y9e01rnrngvpl6bw0s4/cache.zip?rlkey=kz6qoesrulpwsr8jmegqkim9r&st=hps2dq1o&dl=1";
    private static final String CACHE_ZIP_PATH = Environment.getExternalStorageDirectory() + "/Android/data/com.aurora.game/cache.zip";
    private static final String EXTRACT_DIR = Environment.getExternalStorageDirectory() + "/Android/data/com.aurora.game/";
    private static final String VERSION_DIR = Environment.getExternalStorageDirectory() + "/Android/data/com.aurora.game/files/";
    private static final String VERSION_FILE = VERSION_DIR + "version.txt";
    private static final String VERSION = "ver: 1.0.0";

    private static final long MEGABYTE = 1024 * 1024;
    private static final long GIGABYTE = 1024 * 1024 * 1024;
    private long totalDownloadSize = 0;
    private long downloadedBytes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenMode();

        setContentView(R.layout.launcher_load_game_activity);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        trackScreenView("LoadGameActivity");

        boolean isReinstall = getIntent().getBooleanExtra("is_reinstall", false);
        trackInstallStarted(isReinstall);

        initViews();
 
        playIntroVideo();

        new Handler().postDelayed(() -> {

            if (isDataInstalled()) {

                startActivity(
                        new Intent(
                                LoadGameActivity.this,
                                HomeActivity.class
                        )
                );

                finish();

            } else {

                startCacheInstallation();

            }

        }, 100);
    }

    private void setFullScreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private boolean isDataInstalled() {

        try {

            File versionFile =
                    new File(VERSION_FILE);

            if (!versionFile.exists()) {
                return false;
            }

            BufferedReader reader =
                    new BufferedReader(
                            new FileReader(versionFile)
                    );

            String version =
                    reader.readLine();

            reader.close();

            return version != null
                    && version.trim().equals("2");

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    private void trackScreenView(String screenName) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
            bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
        } catch (Exception e) {
            Log.e("FirebaseAnalytics", "Error logging screen view: " + e.getMessage());
        }
    }

    private void trackInstallStarted(boolean isReinstall) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString("install_type", isReinstall ? "reinstall" : "first_install");
            bundle.putString("cache_url", CACHE_URL);
            bundle.putBoolean("is_reinstall", isReinstall);
            mFirebaseAnalytics.logEvent("install_started", bundle);
            Log.d("FirebaseAnalytics", "Install started, reinstall: " + isReinstall);
        } catch (Exception e) {
            Log.e("FirebaseAnalytics", "Error logging install start: " + e.getMessage());
        }
    }

    private void trackInstallProgress(int progress, String stage) {
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("progress", progress);
            bundle.putString("stage", stage);
            mFirebaseAnalytics.logEvent("install_progress", bundle);
        } catch (Exception e) {
            Log.e("FirebaseAnalytics", "Error logging install progress: " + e.getMessage());
        }
    }

    private void trackInstallCompleted(boolean success, String errorMessage, boolean isReinstall) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString("status", success ? "success" : "failed");
            bundle.putBoolean("is_reinstall", isReinstall);
            if (!success) {
                bundle.putString("error_message", errorMessage);
            }
            bundle.putString("install_type", isReinstall ? "reinstall" : "first_install");
            mFirebaseAnalytics.logEvent("install_completed", bundle);
            Log.d("FirebaseAnalytics", "Install completed: " + (success ? "success" : "failed") + ", reinstall: " + isReinstall);
        } catch (Exception e) {
            Log.e("FirebaseAnalytics", "Error logging install completion: " + e.getMessage());
        }
    }

    private void initViews() {
        downloadProgressBar = findViewById(R.id.download_progress_bar);
        currentFileText = findViewById(R.id.current_file);
        downloadProgressText = findViewById(R.id.download_progress_text);
        introVideoView = findViewById(R.id.intro_video_view);

        downloadProgressBar.setProgress(0);
        downloadProgressText.setText("0.00 GB из 0.00 GB / 0%");

        boolean isReinstall = getIntent().getBooleanExtra("is_reinstall", false);
        if (isReinstall) {
            currentFileText.setText("MENGINSTAL ULANG...");
        } else {
            currentFileText.setText("MENGUNDUH FILE: ");
        }
    }

    private void playIntroVideo() {
        try { 
            String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.intro;
            Uri videoUri = Uri.parse(videoPath);

            introVideoView.setVideoURI(videoUri);
            introVideoView.setOnPreparedListener(mp -> { 
                introVideoView.setVisibility(View.VISIBLE);
                introVideoView.start();
 
                mp.setLooping(true);
 
                introVideoView.setScaleX(1.1f);
                introVideoView.setScaleY(1.1f);
            });

            introVideoView.setOnErrorListener((mp, what, extra) -> {
                Log.e("VideoView", "Error playing video: " + what + ", " + extra);
 
                introVideoView.setVisibility(View.GONE);
                return true;
            });

        } catch (Exception e) {
            Log.e("VideoView", "Error setting up video: " + e.getMessage());
            introVideoView.setVisibility(View.GONE);
        }
    }

    private void startCacheInstallation() {
        new CacheInstallTask().execute();
    }

    private String formatFileSize(long bytes) {
        if (bytes >= GIGABYTE) {
            return String.format("%.2f GB", (double) bytes / GIGABYTE);
        } else if (bytes >= MEGABYTE) {
            return String.format("%.2f MB", (double) bytes / MEGABYTE);
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", (double) bytes / 1024);
        } else {
            return bytes + " B";
        }
    }

    private void updateProgress(int progress, long downloadedBytes, long totalBytes, String statusMsg) {
        runOnUiThread(() -> {
            if (progress >= 0) {
                downloadProgressBar.setProgress(progress);
            }

            String downloadedFormatted = formatFileSize(downloadedBytes);
            String totalFormatted = formatFileSize(totalBytes);
            String percentText = String.format("%s %s / %d%%", downloadedFormatted, totalBytes > 0 ? "de " + totalFormatted : "", progress >= 0 ? progress : 0);
            downloadProgressText.setText(percentText);

            if (statusMsg != null && !statusMsg.isEmpty()) {
                currentFileText.setText(statusMsg);
            } else {
                if (progress < 70) {
                    boolean isReinstall = getIntent().getBooleanExtra("is_reinstall", false);
                    if (isReinstall) {
                        currentFileText.setText("MENGUNDUH FILE PEMBARUAN...");
                    } else {
                        currentFileText.setText("MENGUNDUH FILE GAME...");
                    }
                } else if (progress < 100) {
                    currentFileText.setText("MENYELESAIKAN INSTALASI...");
                } else {
                    boolean isReinstall = getIntent().getBooleanExtra("is_reinstall", false);
                    if (isReinstall) {
                        currentFileText.setText("INSTAL ULANG SELESAI!");
                    } else {
                        currentFileText.setText("INSTALASI SELESAI!");
                    }
                }
            }

            if (progress >= 100) {
                if (introVideoView != null && introVideoView.isPlaying()) {
                    introVideoView.stopPlayback();
                    introVideoView.setVisibility(View.GONE);
                }
            }
        });
    }

    private class CacheInstallTask extends AsyncTask<Void, Object, Boolean> {
        private String errorMessage = "";
        private long totalBytes = 0;
        private long downloadedBytes = 0;

        private String parseNextLink(String linkHeader) {
            if (linkHeader == null || linkHeader.trim().isEmpty()) {
                return "";
            }

            String[] parts = linkHeader.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.contains("rel=\"next\"")) {
                    continue;
                }

                int start = trimmed.indexOf('<');
                int end = trimmed.indexOf('>');
                if (start >= 0 && end > start) {
                    return trimmed.substring(start + 1, end);
                }
            }

            return "";
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                publishProgress(0, 0L, 0L, "Carregando configurações...");

                // Read update.json from assets
                String jsonStr = "";
                try {
                    InputStream is = getAssets().open("update.json");
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    jsonStr = new String(buffer, "UTF-8");
                } catch (Exception e) {
                    errorMessage = "Error reading update.json: " + e.getMessage();
                    return false;
                }

                JSONObject obj = new JSONObject(jsonStr);
                JSONObject hf = obj.getJSONObject("huggingface");
                String repoId = hf.getString("repo_id");
                String repoType = hf.optString("repo_type", "dataset");
                String revision = hf.optString("revision", "main");
                String prefix = hf.optString("files_path_prefix", "data-lite/files");

                String repoTypePath = "datasets/";
                if ("space".equalsIgnoreCase(repoType) || "spaces".equalsIgnoreCase(repoType)) {
                    repoTypePath = "spaces/";
                } else if ("model".equalsIgnoreCase(repoType) || "models".equalsIgnoreCase(repoType)) {
                    repoTypePath = "models/";
                }

                String treeUrl = "https://huggingface.co/api/" + repoTypePath + repoId + "/tree/" + revision + "?recursive=true&expand=true";
                String resolveBaseUrl = "https://huggingface.co/" + repoTypePath + repoId + "/resolve/" + revision;

                publishProgress(0, 0L, 0L, "Obtendo lista de arquivos do servidor...");

                ArrayList<JSONObject> fileEntries = new ArrayList<>();
                String nextUrl = treeUrl;

                while (nextUrl != null && !nextUrl.trim().isEmpty()) {
                    HttpURLConnection connection = null;
                    BufferedReader reader = null;
                    try {
                        connection = (HttpURLConnection) new URL(nextUrl).openConnection();
                        connection.setConnectTimeout(30000);
                        connection.setReadTimeout(30000);
                        connection.connect();

                        int responseCode = connection.getResponseCode();
                        if (responseCode < 200 || responseCode >= 300) {
                            throw new IOException("Unexpected response code: " + responseCode);
                        }

                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder buffer = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line);
                        }

                        JSONArray entries = new JSONArray(buffer.toString());
                        for (int i = 0; i < entries.length(); i++) {
                            JSONObject item = entries.optJSONObject(i);
                            if (item == null || !"file".equalsIgnoreCase(item.optString("type"))) {
                                continue;
                            }

                            String remotePath = item.optString("path", "").trim();
                            if (remotePath.isEmpty()) {
                                continue;
                            }

                            fileEntries.add(item);
                        }

                        nextUrl = parseNextLink(connection.getHeaderField("Link"));
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException ignored) {}
                        }
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }

                ArrayList<JSONObject> targetFiles = new ArrayList<>();
                long totalFilesSize = 0;
                String prefixWithSlash = prefix.endsWith("/") ? prefix : prefix + "/";

                for (JSONObject entry : fileEntries) {
                    String remotePath = entry.optString("path", "").trim();
                    if (remotePath.isEmpty() || remotePath.startsWith(".")) {
                        continue;
                    }

                    if (remotePath.startsWith(prefixWithSlash)) {
                        targetFiles.add(entry);
                        totalFilesSize += entry.optLong("size", 0);
                    }
                }

                if (targetFiles.isEmpty()) {
                    errorMessage = "Nenhum arquivo encontrado para baixar.";
                    return false;
                }

                totalBytes = totalFilesSize;

                long alreadyDownloaded = 0;

                for (JSONObject entry : targetFiles) {

                    String remotePath =
                            entry.optString("path", "").trim();

                    String localPath =
                            remotePath.substring(
                                    prefixWithSlash.length()
                            );

                    File file =
                            new File(
                                    getExternalFilesDir(null),
                                    localPath
                            );

                    long remoteSize =
                            entry.optLong("size", 0);

                    if (file.exists()
                            && file.length() == remoteSize) {

                        alreadyDownloaded += remoteSize;
                    }
                }

                long overallDownloaded = alreadyDownloaded;
                byte[] dataBuffer = new byte[8192];

                for (int fileIndex = 0; fileIndex < targetFiles.size(); fileIndex++) {

                    JSONObject entry = targetFiles.get(fileIndex);

                    String remotePath =
                            entry.optString("path", "").trim();

                    String localPath =
                            remotePath.substring(
                                    prefixWithSlash.length()
                            );

                    File outFile =
                            new File(
                                    getExternalFilesDir(null),
                                    localPath
                            );

                    long remoteSize =
                            entry.optLong("size", 0);

                    if (outFile.exists()
                            && outFile.length() == remoteSize) {


                        int progress =
                                (int)(
                                        overallDownloaded
                                                * 95
                                                / totalBytes
                                );

                        publishProgress(
                                progress,
                                overallDownloaded,
                                totalBytes,
                                "Verificando arquivos..."
                        );

                        continue;
                    }

                    File parent = outFile.getParentFile();

                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    String downloadUrl =
                            resolveBaseUrl + "/" + remotePath;
                    publishProgress(-1, overallDownloaded, totalBytes, "Baixando: " + localPath);

                    HttpURLConnection conn = null;
                    InputStream input = null;
                    OutputStream output = null;
                    try {
                        conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                        conn.setConnectTimeout(30000);
                        conn.setReadTimeout(30000);
                        conn.connect();

                        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            errorMessage = "Erro HTTP " + conn.getResponseCode() + " ao baixar: " + localPath;
                            return false;
                        }

                        input = conn.getInputStream();
                        output = new FileOutputStream(outFile);

                        int count;
                        while ((count = input.read(dataBuffer)) != -1) {
                            output.write(dataBuffer, 0, count);
                            overallDownloaded += count;

                            int progress = 0;
                            if (totalBytes > 0) {
                                progress = (int) (overallDownloaded * 95 / totalBytes);
                            }
                            publishProgress(progress, overallDownloaded, totalBytes, "Baixando: " + localPath);
                        }
                    } catch (IOException e) {
                        errorMessage = "Erro ao baixar " + localPath + ": " + e.getMessage();
                        return false;
                    } finally {
                        try {
                            if (output != null) output.close();
                            if (input != null) input.close();
                        } catch (IOException ignored) {}
                        if (conn != null) conn.disconnect();
                    }
                }

                publishProgress(98, totalBytes, totalBytes, "Salvando versão...");
                if (!createVersionFile()) {
                    errorMessage = "Falha ao criar arquivo de versão.";
                    return false;
                }

                publishProgress(100, totalBytes, totalBytes, "Instalação concluída!");
                return true;

            } catch (Exception e) {
                errorMessage = e.getMessage();
                e.printStackTrace();
                return false;
            }
        }

        private boolean createVersionFile() {
            try {
                File versionFile = new File(VERSION_FILE);
                File parent = versionFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                FileWriter writer = new FileWriter(versionFile);
                writer.write("2");
                writer.flush();
                writer.close();
                return true;
            } catch (Exception e) {
                errorMessage = "Exceção do arquivo de versão: " + e.getMessage();
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            if (values.length >= 4) {
                int progress = (Integer) values[0];
                long downloaded = (Long) values[1];
                long total = (Long) values[2];
                String msg = (String) values[3];
                updateProgress(progress, downloaded, total, msg);
            } else if (values.length >= 3 && values[0] instanceof Integer) {
                int progress = (Integer) values[0];
                long downloaded = values[1] instanceof Long ? (Long) values[1] :
                        values[1] instanceof Integer ? ((Integer) values[1]).longValue() : 0;
                long total = values[2] instanceof Long ? (Long) values[2] :
                        values[2] instanceof Integer ? ((Integer) values[2]).longValue() : 0;

                updateProgress(progress, downloaded, total, "");
            } else if (values.length == 1 && values[0] instanceof Integer) {
                updateProgress((Integer) values[0], downloadedBytes, totalBytes, "");
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            boolean isReinstall = getIntent().getBooleanExtra("is_reinstall", false);

            if (success) {
                String totalFormatted = formatFileSize(totalBytes);
                downloadProgressText.setText(String.format("%s de %s / 100%%", totalFormatted, totalFormatted));
                trackInstallCompleted(true, "", isReinstall);
                new Handler().postDelayed(() -> {
                    startActivity(new Intent(LoadGameActivity.this, HomeActivity.class));
                    finish();
                }, 1500);
            } else {
                if (isReinstall) {
                    currentFileText.setText("KESALAHAN SAAT INSTAL ULANG");
                } else {
                    currentFileText.setText("KESALAHAN SAAT INSTALASI");
                }
                downloadProgressText.setText("GAGAL: " + errorMessage);
                downloadProgressBar.setVisibility(View.VISIBLE);
                downloadProgressText.setVisibility(View.VISIBLE);

                if (introVideoView != null && introVideoView.isPlaying()) {
                    introVideoView.stopPlayback();
                    introVideoView.setVisibility(View.GONE);
                }

                trackInstallCompleted(false, errorMessage, isReinstall);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause(); 
        if (introVideoView != null && introVideoView.isPlaying()) {
            introVideoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume(); 
        if (introVideoView != null && !introVideoView.isPlaying()) {
            introVideoView.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy(); 
        if (introVideoView != null) {
            introVideoView.stopPlayback();
        }
    }
}
