package com.aurora.launcher.update;

import android.content.Context;
import android.os.Environment;

import org.json.JSONObject;

import java.io.*;
import java.net.URL;

public class UpdateChecker {

    private Context context;

    public interface Callback {
        void onUpdateRequired(String remoteVersion);
        void onUpToDate();
    }

    public UpdateChecker(Context context) {
        this.context = context;
    }

    public File getGameDir() {
        return new File(
                Environment.getExternalStorageDirectory(),
                "Android/data/com.aurora.game/files"
        );
    }

    public String getLocalVersion() {
        try {
            File file = new File(getGameDir(), "version.txt");

            if (!file.exists()) return "0.0.0";

            BufferedReader br = new BufferedReader(new FileReader(file));
            return br.readLine();

        } catch (Exception e) {
            return "0.0.0";
        }
    }

    public void check(String url, Callback callback) {

        new Thread(() -> {

            try {

                String json = load(url);

                JSONObject obj = new JSONObject(json);

                String remoteVersion = obj.getString("version");

                String localVersion = getLocalVersion();

                if (!remoteVersion.equals(localVersion)) {
                    callback.onUpdateRequired(remoteVersion);
                } else {
                    callback.onUpToDate();
                }

            } catch (Exception e) {
                callback.onUpToDate();
            }

        }).start();
    }

    private String load(String url) throws Exception {

        BufferedReader br = new BufferedReader(
                new InputStreamReader(new URL(url).openStream())
        );

        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }
}