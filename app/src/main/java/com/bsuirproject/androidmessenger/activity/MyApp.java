package com.bsuirproject.androidmessenger.activity;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Включаем персистентность до первого использования FirebaseDatabase
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}