package com.aurora.game;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.bytedance.shadowhook.ShadowHook;
import com.joom.paranoid.Obfuscate;
import com.wardrumstudios.utils.WarMedia;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Obfuscate
public class GTASA extends WarMedia {
    // public static GTASA gtasaSelf = null;
    static String vmVersion;
    private boolean once = false;

    static {
        ShadowHook.init(new ShadowHook.ConfigBuilder()
                .setMode(ShadowHook.Mode.UNIQUE)
                .build());

        vmVersion = null;
        System.out.println("**** Loading SO's");
        try {
            vmVersion = System.getProperty("java.vm.version");
            System.out.println("vmVersion " + vmVersion);
            System.loadLibrary("ImmEmulatorJ");
        }
        catch (ExceptionInInitializerError | UnsatisfiedLinkError ignored) {
        }
        System.loadLibrary("GTASA");
        System.out.println("**** loading libGTASA.so");
        System.loadLibrary("bass");
        System.out.println("**** loading libbass.so");
        System.loadLibrary("multiplayer");
        System.out.println("**** loading libmultiplayer.so");

    }

    public static void staticEnterSocialClub()
    {
        //  gtasaSelf.EnterSocialClub();
    }

    public static void staticExitSocialClub() {
        //gtasaSelf.ExitSocialClub();
    }

    public void AfterDownloadFunction() {

    }

    public void EnterSocialClub() {

    }

    public void ExitSocialClub() {

    }

    public boolean ServiceAppCommand(String str, String str2)
    {
        return false;
    }

    public int ServiceAppCommandValue(String str, String str2)
    {
        return 0;
    }

    public native void main();

    public void onActivityResult(int i, int i2, Intent intent)
    {
        super.onActivityResult(i, i2, intent);
    }

    public void onConfigurationChanged(Configuration configuration)
    {
        super.onConfigurationChanged(configuration);
    }

    public void onCreate(Bundle bundle)
    {
        if(!once)
        {
            once = true;
        }

        System.out.println("GTASA onCreate");

        // Extrai a pasta Text/ dos assets para o armazenamento externo
        extractTextAssets();

        super.onCreate(bundle);

        /*if (new SharedPreferenceCore().getBoolean(this, "MLOADER")) {
            try {
                System.loadLibrary("monetloader");
            } catch (ExceptionInInitializerError | UnsatisfiedLinkError e) {
                Log.e("AXL", e.getMessage());
            }
        }*/
    }

    public void onDestroy()
    {
        System.out.println("GTASA onDestroy");
        super.onDestroy();
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent)
    {
        return super.onKeyDown(i, keyEvent);
    }

    public void onPause()
    {
        System.out.println("GTASA onPause");
        super.onPause();
    }

    public void onRestart()
    {
        System.out.println("GTASA onRestart");
        super.onRestart();
    }

    public void onResume()
    {
        System.out.println("GTASA onResume");
        super.onResume();
    }

    public void onStart()
    {
        System.out.println("GTASA onStart");
        super.onStart();
    }

    public void onStop()
    {
        System.out.println("GTASA onStop");
        super.onStop();
    }

    public native void setCurrentScreenSize(int i, int i2);

    /**
     * Copia os arquivos GXT da pasta Text/ dos assets para o diretório de dados externo.
     * O motor do jogo (libGTASA.so) procura AMERICAN.GXT em files/text/.
     */
    private void extractTextAssets() {
        try {
            File storageRoot = getExternalFilesDir(null);
            if (storageRoot == null) {
                Log.e("GTASA", "extractTextAssets: storageRoot is null");
                return;
            }

            File textDir = new File(storageRoot, "text");
            if (!textDir.exists()) {
                textDir.mkdirs();
            }

            String[] gxtFiles = getAssets().list("Text");
            if (gxtFiles == null) return;

            for (String fileName : gxtFiles) {
                File destFile = new File(textDir, fileName.toUpperCase());
                if (!destFile.exists() || destFile.length() == 0) {
                    Log.i("GTASA", "Extraindo Text/" + fileName + " -> " + destFile.getAbsolutePath());
                    try (InputStream is = getAssets().open("Text/" + fileName);
                         FileOutputStream fos = new FileOutputStream(destFile)) {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = is.read(buf)) != -1) {
                            fos.write(buf, 0, n);
                        }
                    }
                }
            }
            Log.i("GTASA", "extractTextAssets: concluído");
        } catch (IOException e) {
            Log.e("GTASA", "extractTextAssets falhou: " + e.getMessage());
        }
    }
}