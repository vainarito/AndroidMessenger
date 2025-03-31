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
import com.bsuirproject.androidmessenger.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";

    private void statusBarColor() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white_blue));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
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
            goToMainActivity();
            return;
        }

        // Загрузка кэшированных данных
        loadCachedCredentials();

        binding.goToRegisterActivityTv.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class)));

        binding.restoreAccess.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class)));

        binding.loginBtn.setOnClickListener(view -> {
            String email = binding.emailEd.getText().toString().trim();
            String password = binding.passwordEd.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Поля не могут быть пустыми", Toast.LENGTH_SHORT).show();
                return;
            }

            // Отключаем кнопку перед началом авторизации
            binding.loginBtn.setEnabled(false);
            performLogin(email, password);
        });
    }

    // Загрузка кэшированных данных
    private void loadCachedCredentials() {
        String cachedEmail = prefs.getString(KEY_EMAIL, "");
        String cachedPassword = prefs.getString(KEY_PASSWORD, "");

        if (!cachedEmail.isEmpty() && !cachedPassword.isEmpty()) {
            binding.emailEd.setText(cachedEmail);
            binding.passwordEd.setText(cachedPassword);
            // Можно раскомментировать для автологина:
            // performLogin(cachedEmail, cachedPassword);
        }
    }

    // Выполнение авторизации
    private void performLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveCredentials(email, password); // Сохраняем зашифрованные данные
                        goToMainActivity();
                    } else {
                        Toast.makeText(this, "Ошибка авторизации: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        // Включаем кнопку обратно в случае ошибки
                        binding.loginBtn.setEnabled(true);
                    }
                });
    }

    // Сохранение учетных данных
    private void saveCredentials(String email, String password) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PASSWORD, password); // Пароль шифруется автоматически
        editor.apply();
    }

    // Переход в MainActivity
    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}