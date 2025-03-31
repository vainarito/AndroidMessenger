package com.bsuirproject.androidmessenger.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.bsuirproject.androidmessenger.R;
import com.bsuirproject.androidmessenger.databinding.ActivityRegistrationBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class RegistrationActivity extends AppCompatActivity {
    private ActivityRegistrationBinding binding;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "RegistrationPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_USERNAME = "username";

    private void statusBarColor() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white_lavender));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        statusBarColor();
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Инициализация EncryptedSharedPreferences
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка инициализации шифрования", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Проверка текущего пользователя
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mAuth.signOut(); // Разлогиниваем, так как это регистрация
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding.arrowBtn.setOnClickListener(v -> finish());

        binding.signUpBtn.setOnClickListener(v -> {
            String email = binding.emailEd.getText().toString().trim();
            String password = binding.passwordEd.getText().toString().trim();
            String username = binding.usernameEd.getText().toString().trim();

            if (validateInput(email, password, username)) {
                // Отключаем кнопку перед началом регистрации
                binding.signUpBtn.setEnabled(false);
                startRegistrationProcess(email, password, username);
            }
        });
    }

    // Загрузка кэшированных данных (метод оставлен, но не вызывается)
    private void loadCachedCredentials() {
        String cachedEmail = prefs.getString(KEY_EMAIL, "");
        String cachedPassword = prefs.getString(KEY_PASSWORD, "");
        String cachedUsername = prefs.getString(KEY_USERNAME, "");

        if (!cachedEmail.isEmpty()) binding.emailEd.setText(cachedEmail);
        if (!cachedPassword.isEmpty()) binding.passwordEd.setText(cachedPassword);
        if (!cachedUsername.isEmpty()) binding.usernameEd.setText(cachedUsername);
    }

    // Сохранение данных в кэш
    private void saveCredentials(String email, String password, String username) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PASSWORD, password); // Пароль шифруется автоматически
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    private boolean validateInput(String email, String password, String username) {
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (username.length() > 12) {
            Toast.makeText(this, "Имя не должно превышать 12 символов", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void startRegistrationProcess(String email, String password, String username) {
        Toast.makeText(this, "Регистрация...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveCredentials(email, password, username); // Сохраняем зашифрованные данные
                            saveUserData(user, username, email);
                        }
                    } else {
                        handleRegistrationError(Objects.requireNonNull(task.getException()));
                        // Включаем кнопку обратно в случае ошибки
                        binding.signUpBtn.setEnabled(true);
                    }
                });
    }

    private void saveUserData(FirebaseUser user, String username, String email) {
        HashMap<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", email);
        userInfo.put("username", username);
        userInfo.put("profileImage", "");
        userInfo.put("chats", "");
        userInfo.put("isVerified", false);

        FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(user.getUid())
                .setValue(userInfo)
                .addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        mAuth.signOut();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        deleteUnverifiedUser(user);
                        // Включаем кнопку обратно в случае ошибки
                        binding.signUpBtn.setEnabled(true);
                    }
                });
    }

    private void deleteUnverifiedUser(FirebaseUser user) {
        user.delete().addOnCompleteListener(task -> {
            binding.signUpBtn.setEnabled(true);
            Toast.makeText(this, "Ошибка сохранения данных, попробуйте снова", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleRegistrationError(Exception exception) {
        binding.signUpBtn.setEnabled(true);
        Toast.makeText(this, "Ошибка регистрации: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
    }
}