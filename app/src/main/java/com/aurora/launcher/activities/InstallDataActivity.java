package com.aurora.launcher.activities;

import android.os.*;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.aurora.game.R;

import java.io.File;
import java.io.FileOutputStream;

public class InstallDataActivity extends AppCompatActivity {

    private ProgressBar progress;
    private TextView status, percent;

    private final int SERVER_VERSION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_data);

        progress = findViewById(R.id.progress);
        status = findViewById(R.id.status);
        percent = findViewById(R.id.percent);

        startInstall();
    }

    private void startInstall() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                downloadFake();

                extractData();

                saveVersion();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        status.setText("Selesai!");
                        finish();
                    }
                });
            }
        }).start();
    }

    // ---------------- DOWNLOAD SIMULADO ----------------

    private void downloadFake() {

        for (int i = 0; i <= 100; i++) {

            final int p = i;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progress.setProgress(p);
                    percent.setText(p + "%");
                    status.setText("Mengunduh data...");
                }
            });

            try {
                Thread.sleep(20);
            } catch (Exception ignored) {}
        }
    }

    // ---------------- EXTRAI PARA DATA ----------------

    private void extractData() {

        try {

            File baseDir =
                    getExternalFilesDir(null); // Android/data/com.aurora.game/files

            File dataFolder = new File(baseDir, "files");

            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            // EXEMPLO: criando arquivo de teste
            File test = new File(dataFolder, "data_installed.txt");

            FileOutputStream fos =
                    new FileOutputStream(test);

            fos.write("DATA OK".getBytes());
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- SALVA VERSÃO ----------------

    private void saveVersion() {

        try {

            File file = new File(getExternalFilesDir(null),
                    "version.txt");

            FileOutputStream fos =
                    new FileOutputStream(file);

            fos.write(String.valueOf(SERVER_VERSION).getBytes());
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}