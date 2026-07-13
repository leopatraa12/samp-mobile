package com.aurora.game.ui;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.nvidia.devtech.NvEventQueueActivity;
import com.aurora.game.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.view.WindowManager;

public class Hud {

    public Activity activity;
    public ConstraintLayout main_hud;
    private final NvEventQueueActivity mContext = null;
    private boolean isHudSetPos = false;

    private NvEventQueueActivity nvActivity;

    public ConstraintLayout hud_info_layout, health_layout, armour_layout, eat_layout;
    public TextView health_text, armour_text, eat_text, hud_money, hud_ammo, hud_logo_text;
    public ImageView hud_logo_img;
    public ConstraintLayout samp_buttons_layout;
    public ImageView hud_button_menu, hud_button_star, hud_button_inv, hud_button_shop, hud_button_help;
    public ImageView radar_zone;
    public ConstraintLayout radar_layout, select_weapon;
    public ImageView weapon_button;
    public ImageView star_1, star_2, star_3, star_4, star_5;
    public ConstraintLayout hud_logo_layout;

    private Context context;
    private final Handler handler = new Handler();

    private String playerName = "Egor_Kuzn";
    private int playerId = 0;
    private int currentWeaponId = 0;
    private int currentAmmo = 0;
    private int currentAmmoInClip = 0;
    private boolean isFirstShow = true;

    private int[] getScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            activity.getDisplay().getRealMetrics(metrics);
        } else {
            WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
        }
        return new int[]{metrics.widthPixels, metrics.heightPixels};
    }

    private class ConvertViewCoordsToGta {
        class Data {
            float x, y, width, height;

            Data(float x, float y, float width, float height) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }
        }

        Data convertCoordsToGta(Data data) {
            int[] screenSize = Hud.this.getScreenSize();
            float screenWidth = (float) screenSize[0];
            float screenHeight = (float) screenSize[1];

            Data returnData = new Data(0, 0, 0, 0);

            float viewPercentX = data.x / screenWidth;
            float viewPercentY = data.y / screenHeight;

            returnData.x = 640f * viewPercentX;
            returnData.y = 480f * viewPercentY;

            float viewPercentWidth = data.width / screenWidth;
            float viewPercentHeight = data.height / screenHeight;

            returnData.width = 640f * viewPercentWidth * 0.895f;
            returnData.height = 480f * viewPercentHeight;

            return returnData;
        }
    }

    native void SetRadarBgPos(float x1, float y1, float x2, float y2);
    native void nativeSetRadarPos(float x, float y, float width, float height);

    public Hud(Activity aactivity) {
        activity = aactivity;
        main_hud = aactivity.findViewById(R.id.main_hud);

        try {
            nvActivity = NvEventQueueActivity.getInstance();
        } catch (Exception e) {
            Log.e("HUD", "Error getting NvEventQueueActivity instance: " + e.getMessage());
        }

        if (main_hud == null) {
            Log.e("Hud", "Main HUD layout is null! Check setContentView.");
            return;
        }

        initializeViews();

        loadPlayerNameFromSettings();

        setupRadarPosition();

        HideHud();

        setInitialValues();

        setupButtonClickListeners();
    }

    private void setupButtonClickListeners() {
        setMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));

                byte[] commandBytes = "/mm".getBytes();

                if (nvActivity != null) {
                    nvActivity.sendCommand(commandBytes);
                } else {
                    NvEventQueueActivity.getInstance().sendCommand(commandBytes);
                }
            }
        });

        setStarButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));

                byte[] commandBytes = "/mm".getBytes();

                if (nvActivity != null) {
                    nvActivity.sendCommand(commandBytes);
                } else {
                    NvEventQueueActivity.getInstance().sendCommand(commandBytes);
                }
            }
        });

        setInvButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));

                byte[] commandBytes = "/mm".getBytes();

                if (nvActivity != null) {
                    nvActivity.sendCommand(commandBytes);
                } else {
                    NvEventQueueActivity.getInstance().sendCommand(commandBytes);
                }
            }
        });

        setShopButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));

                byte[] commandBytes = "/mm".getBytes();

                if (nvActivity != null) {
                    nvActivity.sendCommand(commandBytes);
                } else {
                    NvEventQueueActivity.getInstance().sendCommand(commandBytes);
                }
            }
        });

        setHelpButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.button_click));

                byte[] commandBytes = "/mm".getBytes();

                if (nvActivity != null) {
                    nvActivity.sendCommand(commandBytes);
                } else {
                    NvEventQueueActivity.getInstance().sendCommand(commandBytes);
                }
            }
        });
    }

    private void loadPlayerNameFromSettings() {
        try {
            File file = new File(activity.getExternalFilesDir(null) + "/SAMP/settings.ini");

            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("name")) {
                        String[] parts = line.split("=");
                        if (parts.length >= 2) {
                            playerName = parts[1].trim();
                            Log.d("HUD", "Loaded player name from settings: " + playerName);
                            break;
                        }
                    }
                }
                reader.close();

                if (playerName.equals("Egor_Kuzn")) {
                    reader = new BufferedReader(new FileReader(file));
                    boolean inClientSection = false;

                    while ((line = reader.readLine()) != null) {
                        line = line.trim();

                        if (line.startsWith("[") && line.endsWith("]")) {
                            inClientSection = line.equalsIgnoreCase("[client]");
                            continue;
                        }

                        if (inClientSection && line.startsWith("name")) {
                            String[] parts = line.split("=");
                            if (parts.length >= 2) {
                                playerName = parts[1].trim();
                                Log.d("HUD", "Loaded player name from [client] section: " + playerName);
                                break;
                            }
                        }
                    }
                    reader.close();
                }
            } else {
                Log.e("HUD", "SAMP settings file not found: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e("HUD", "Error reading SAMP settings: " + e.getMessage());
            playerName = "Egor_Kuzn";
        }

        if (playerName.equals("Egor_Kuzn")) {
            try {
                File file = new File(activity.getExternalFilesDir(null) + "/SAMP/settings.ini");
                if (file.exists()) {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.toLowerCase().contains("name") && line.contains("=")) {
                            String[] parts = line.split("=", 2);
                            if (parts.length == 2) {
                                playerName = parts[1].trim();
                                Log.d("HUD", "Fallback loaded player name: " + playerName);
                                break;
                            }
                        }
                    }
                    br.close();
                }
            } catch (Exception e) {
                Log.e("HUD", "Fallback error: " + e.getMessage());
            }
        }
    }

    private void initializeViews() {
        hud_info_layout = main_hud.findViewById(R.id.hud_info_layout);
        health_layout = main_hud.findViewById(R.id.health_layout);
        armour_layout = main_hud.findViewById(R.id.armour_layout);
        eat_layout = main_hud.findViewById(R.id.eat_layout);
        health_text = main_hud.findViewById(R.id.health_text);
        armour_text = main_hud.findViewById(R.id.armour_text);
        eat_text = main_hud.findViewById(R.id.eat_text);
        hud_money = main_hud.findViewById(R.id.hud_money);
        samp_buttons_layout = main_hud.findViewById(R.id.samp_buttons_layout);
        hud_button_menu = main_hud.findViewById(R.id.hud_button_menu);
        radar_zone = main_hud.findViewById(R.id.radar_zone);
        radar_layout = main_hud.findViewById(R.id.radar_layout);
        select_weapon = main_hud.findViewById(R.id.select_weapon);
        weapon_button = main_hud.findViewById(R.id.weapon_button);
        hud_ammo = main_hud.findViewById(R.id.hud_ammo);
        star_1 = main_hud.findViewById(R.id.star_1);
        star_2 = main_hud.findViewById(R.id.star_2);
        star_3 = main_hud.findViewById(R.id.star_3);
        star_4 = main_hud.findViewById(R.id.star_4);
        star_5 = main_hud.findViewById(R.id.star_5);
        hud_logo_layout = main_hud.findViewById(R.id.hud_logo_layout);
    }

    private void setInitialValues() {
        if (health_text != null) health_text.setText("0%");
        if (armour_text != null) armour_text.setText("0%");
        if (eat_text != null) eat_text.setText("100%");
        if (hud_money != null) hud_money.setText("R$ 0");
        if (hud_ammo != null) hud_ammo.setText("0/0");

        updateWantedStars(0);

        updateLogoInfo();
    }

    private void setupRadarPosition() {
        radar_zone.post(() -> {
            float x = radar_zone.getX();
            float y = radar_zone.getY();
            float w = radar_zone.getWidth();
            float h = radar_zone.getHeight();

            ConvertViewCoordsToGta convert = new ConvertViewCoordsToGta();
            ConvertViewCoordsToGta.Data data = convert.new Data(x, y, w, h);
            ConvertViewCoordsToGta.Data gtaData = convert.convertCoordsToGta(data);

            nativeSetRadarPos(gtaData.x, gtaData.y, gtaData.width * 0.96f, gtaData.height * 0.96f);
            SetRadarBgPos(x, y, x + w, y + h);

            Log.d("HUD", "Radar Pos: X=" + gtaData.x + " Y=" + gtaData.y +
                    " W=" + gtaData.width + " H=" + gtaData.height);
        });
    }

    public void updatePlayerInfo(String name, int id) {
        this.playerName = name;
        this.playerId = id;
        updateLogoInfo();
    }

    public void UpdateHudInfo(int health, int armor, int weaponid, int ammo, int ammoinclip,
                              int money, int wanted, int eat, int drink, int bankMoney, int playerid) {
        if (health_text != null) {
            health_text.setText(String.format(Locale.getDefault(), "%d%%", health));
        }

        if (armour_text != null) {
            armour_text.setText(String.format(Locale.getDefault(), "%d%%", armor));
        }

        if (eat_text != null) {
            eat_text.setText("100%");
        }

        if (hud_money != null) {
            DecimalFormat formatter = new DecimalFormat("#,###");
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator(',');
            formatter.setDecimalFormatSymbols(symbols);

            String sMoney = formatter.format(money);
            hud_money.setText(String.format("R$ %s", sMoney));
        }

        currentWeaponId = weaponid;
        currentAmmo = ammo;
        currentAmmoInClip = ammoinclip;
        updateWeaponAndAmmo(weaponid, ammo, ammoinclip);

        updateWantedStars(wanted);

        if (playerid != this.playerId) {
            this.playerId = playerid;
            updateLogoInfo();
        }
    }

    private void updateWeaponAndAmmo(int weaponid, int ammo, int ammoinclip) {
        if (weapon_button != null) {
            weapon_button.setVisibility(View.VISIBLE);
            try {
                String drawableName = String.format("weapon_%d", weaponid);
                int resId = activity.getResources().getIdentifier(
                        drawableName,
                        "drawable",
                        activity.getPackageName());

                if (resId != 0) {
                    weapon_button.setImageResource(resId);
                } else {
                    weapon_button.setImageResource(R.drawable.weapon_0);
                    Log.w("HUD", "Weapon drawable not found: " + drawableName);
                }
            } catch (Exception e) {
                Log.e("HUD", "Error setting weapon image: " + e.getMessage());
                weapon_button.setImageResource(R.drawable.weapon_0);
            }
        }

        if (weaponid == 0) {
            if (hud_ammo != null) {
                hud_ammo.setVisibility(View.INVISIBLE);
            }
        } else {
            if (hud_ammo != null) {
                hud_ammo.setVisibility(View.VISIBLE);
                hud_ammo.setText(String.format(Locale.getDefault(), "%d/%d", ammoinclip, ammo));
            }
        }
    }

    public void updateAmmoOnFire() {
        if (currentWeaponId != 0 && currentAmmoInClip > 0) {
            currentAmmoInClip--;
            if (hud_ammo != null && hud_ammo.getVisibility() == View.VISIBLE) {
                hud_ammo.setText(String.format(Locale.getDefault(), "%d/%d", currentAmmoInClip, currentAmmo));
            }
        }
    }

    public void updateAmmoOnReload(int newAmmoInClip, int totalAmmo) {
        currentAmmoInClip = newAmmoInClip;
        currentAmmo = totalAmmo;
        if (currentWeaponId != 0 && hud_ammo != null && hud_ammo.getVisibility() == View.VISIBLE) {
            hud_ammo.setText(String.format(Locale.getDefault(), "%d/%d", currentAmmoInClip, currentAmmo));
        }
    }

    private void updateLogoInfo() {
        if (hud_logo_text != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String currentDate = sdf.format(new Date());

            String infoText = String.format(Locale.getDefault(),
                    "ID %d: %s\n%s",
                    playerId,
                    playerName,
                    currentDate);

            hud_logo_text.setText(infoText);
        }
    }

    private void updateWantedStars(int wantedLevel) {

        if (wantedLevel <= 0) {
            setWantedStarState(star_1, false);
            setWantedStarState(star_2, false);
            setWantedStarState(star_3, false);
            setWantedStarState(star_4, false);
            setWantedStarState(star_5, false);

            if (star_1 != null) star_1.setVisibility(View.VISIBLE);
            if (star_2 != null) star_2.setVisibility(View.VISIBLE);
            if (star_3 != null) star_3.setVisibility(View.VISIBLE);
            if (star_4 != null) star_4.setVisibility(View.VISIBLE);
            if (star_5 != null) star_5.setVisibility(View.VISIBLE);
        } else {
            setWantedStarState(star_1, wantedLevel >= 1);
            setWantedStarState(star_2, wantedLevel >= 2);
            setWantedStarState(star_3, wantedLevel >= 3);
            setWantedStarState(star_4, wantedLevel >= 4);
            setWantedStarState(star_5, wantedLevel >= 5);

            if (star_1 != null) star_1.setVisibility(View.VISIBLE);
            if (star_2 != null) star_2.setVisibility(View.VISIBLE);
            if (star_3 != null) star_3.setVisibility(View.VISIBLE);
            if (star_4 != null) star_4.setVisibility(View.VISIBLE);
            if (star_5 != null) star_5.setVisibility(View.VISIBLE);
        }
    }

    private void setWantedStarState(ImageView star, boolean active) {
        if (star != null) {
            if (active) {
                star.setImageResource(R.drawable.ic_starnew_off);
            } else {
                star.setImageResource(R.drawable.ic_starnew_on);
            }
        }
    }

    public void toggleWeaponView() {
        if (select_weapon != null) {
            if (select_weapon.getVisibility() == View.VISIBLE) {
                select_weapon.setVisibility(View.INVISIBLE);
            } else {
                select_weapon.setVisibility(View.VISIBLE);
            }
        }
    }

    public void ShowHud() {
        if (main_hud != null) {
            main_hud.setVisibility(View.VISIBLE);
            Log.d("HUD", "HUD shown");

            if (isFirstShow) {
                isFirstShow = false;
                sendHudShownBroadcast();
            }
        }
    }

    public void HideHud() {
        if (main_hud != null) {
            main_hud.setVisibility(View.INVISIBLE);
            Log.d("HUD", "HUD hidden");
        }
    }

    private void sendHudShownBroadcast() {
        try {
            Intent hudShownIntent = new Intent("HUD_SHOWN");
            activity.sendBroadcast(hudShownIntent);
            Log.d("HUD", "Broadcast sent: HUD_SHOWN");
        } catch (Exception e) {
            Log.e("HUD", "Error sending broadcast: " + e.getMessage());
        }
    }

    public void updateRadarVisibility(boolean visible) {
        if (radar_zone != null) {
            radar_zone.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (radar_layout != null) {
            radar_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setWeaponVisibility(boolean visible) {
        if (weapon_button != null) {
            weapon_button.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (select_weapon != null) {
            select_weapon.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        if (hud_ammo != null) {
            hud_ammo.setVisibility(visible && currentWeaponId != 0 ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setHealthVisibility(boolean visible) {
        if (health_layout != null) {
            health_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setArmourVisibility(boolean visible) {
        if (armour_layout != null) {
            armour_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setEatVisibility(boolean visible) {
        if (eat_layout != null) {
            eat_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setMoneyVisibility(boolean visible) {
        if (hud_money != null) {
            hud_money.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setButtonsVisibility(boolean visible) {
        if (samp_buttons_layout != null) {
            samp_buttons_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setLogoVisibility(boolean visible) {
        if (hud_logo_layout != null) {
            hud_logo_layout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setMenuButtonClickListener(View.OnClickListener listener) {
        if (hud_button_menu != null) {
            hud_button_menu.setOnClickListener(listener);
        }
    }

    public void setStarButtonClickListener(View.OnClickListener listener) {
        if (hud_button_star != null) {
            hud_button_star.setOnClickListener(listener);
        }
    }

    public void setInvButtonClickListener(View.OnClickListener listener) {
        if (hud_button_inv != null) {
            hud_button_inv.setOnClickListener(listener);
        }
    }

    public void setShopButtonClickListener(View.OnClickListener listener) {
        if (hud_button_shop != null) {
            hud_button_shop.setOnClickListener(listener);
        }
    }

    public void setHelpButtonClickListener(View.OnClickListener listener) {
        if (hud_button_help != null) {
            hud_button_help.setOnClickListener(listener);
        }
    }
}
