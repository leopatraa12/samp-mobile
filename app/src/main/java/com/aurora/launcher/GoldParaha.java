package com.aurora.launcher;
import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

public class GoldParaha extends Application {
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e("CRASH", "Uncaught exception in thread " + thread.getName(), throwable);
                throwable.printStackTrace();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });

        try {
            FirebaseApp.initializeApp(this);

            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

            Log.d("GoldParaha", "Firebase initialized successfully");
			
            if (mFirebaseAnalytics != null) {
                setUserProperties();
            } else {
                Log.w("GoldParaha", "FirebaseAnalytics instance is null");
            }

        } catch (Exception e) {
            Log.e("GoldParaha", "Error initializing Firebase: " + e.getMessage(), e);
        }
    }

    private void setUserProperties() {
        try {
            mFirebaseAnalytics.setUserProperty("app_version", "0.8.2.1");
            mFirebaseAnalytics.setUserProperty("device_architecture", "ARMx64");
            mFirebaseAnalytics.setUserProperty("build_type", "release");
            Log.d("GoldParaha", "User properties set successfully");
        } catch (Exception e) {
            Log.e("GoldParaha", "Error setting user properties: " + e.getMessage(), e);
        }
    }

    public FirebaseAnalytics getFirebaseAnalytics() {
        return mFirebaseAnalytics;
    }
}
