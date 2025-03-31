package com.bsuirproject.androidmessenger.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bsuirproject.androidmessenger.R;
import com.bsuirproject.androidmessenger.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import com.bsuirproject.androidmessenger.bottomnav.chats.ChatsFragment;
import com.bsuirproject.androidmessenger.bottomnav.new_chat.NewChatFragment;
import com.bsuirproject.androidmessenger.bottomnav.profile.ProfileFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    private void statusBarColor() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white_blue));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        statusBarColor();
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Проверка авторизации
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Предварительная загрузка данных пользователя в SharedPreferences
        preloadUserData();

        // Настраиваем навигацию после загрузки данных
        setupNavigation();
    }

    private void preloadUserData() {
        FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        String username = snapshot.child("username").getValue(String.class);
                        String profileImage = snapshot.child("profileImage").getValue(String.class);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("username", username);
                        editor.putString("profileImage", profileImage);
                        editor.apply();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Ошибка загрузки данных: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupNavigation() {
        Map<Integer, Fragment> fragmentMap = new HashMap<>();
        fragmentMap.put(R.id.chats, new ChatsFragment());
        fragmentMap.put(R.id.new_chat, new NewChatFragment());
        fragmentMap.put(R.id.profile, new ProfileFragment());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(binding.fragmentContainer.getId(), fragmentMap.get(item.getItemId()))
                    .commit();
            return true;
        });

        binding.bottomNav.setSelectedItemId(R.id.chats);
    }
}