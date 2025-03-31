package com.bsuirproject.androidmessenger.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bsuirproject.androidmessenger.R;
import com.bsuirproject.androidmessenger.databinding.ActivityRestorePasswordBinding;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class ResetPasswordActivity extends AppCompatActivity {
    private ActivityRestorePasswordBinding binding;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRestorePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initFirebase();
        setupProgressDialog();
        setupUI();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Обработка запроса");
        progressDialog.setMessage("Проверяем данные...");
        progressDialog.setCancelable(false);
    }

    private void setupUI() {
        binding.imageView10.setOnClickListener(v -> finish());

        binding.restoreBtn.setOnClickListener(v -> {
            String email = getNormalizedEmail();
            if (validateEmail(email)) {
                startPasswordResetProcess(email);
            }
        });
    }

    private String getNormalizedEmail() {
        return binding.emailEd.getText().toString().toLowerCase().trim();
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            showToast("Введите email");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Неверный формат email");
            return false;
        }
        return true;
    }

    private void startPasswordResetProcess(String email) {
        showLoading();
        checkNetworkConnection(email);
    }

    private void checkNetworkConnection(String email) {
        if (!isNetworkAvailable()) {
            hideLoading();
            showToast("Нет интернет-соединения");
            return;
        }
        checkEmailExistence(email);
    }

    private void checkEmailExistence(String email) {
        FirebaseDatabase.getInstance().getReference("Users")
                .orderByChild("email")
                .equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            sendResetEmail(email);
                        } else {
                            hideLoading();
                            showToast("Аккаунт с таким email не существует");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        hideLoading();
                        showToast("Ошибка базы данных: " + error.getMessage());
                    }
                });
    }

    private void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        handleSuccess(email);
                    } else {
                        handleError(task.getException());
                    }
                });
    }

    private void handleSuccess(String email) {
        showCustomToast("Письмо отправлено на " + email, R.drawable.baseline_mark_email_read_24);
        handler.postDelayed(this::finish, 1500);
    }

    private void showCustomToast(String message, int iconRes) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_layout));
        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);

        icon.setImageResource(iconRes);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    private void handleError(Exception exception) {
        String errorMessage = "Ошибка отправки";
        if (exception instanceof FirebaseAuthInvalidUserException) {
            errorMessage = "Аккаунт не найден";
        } else if (exception instanceof FirebaseNetworkException) {
            errorMessage = "Проблемы с подключением";
        } else if (exception instanceof FirebaseAuthException) {
            errorMessage = parseFirebaseError(((FirebaseAuthException) exception).getErrorCode());
        }
        showToast(errorMessage);
    }

    private String parseFirebaseError(String errorCode) {
        switch (errorCode) {
            case "ERROR_INVALID_EMAIL":
                return "Неверный формат email";
            case "ERROR_USER_DISABLED":
                return "Аккаунт заблокирован";
            case "ERROR_TOO_MANY_REQUESTS":
                return "Слишком много запросов";
            default:
                return "Ошибка сервера";
        }
    }

    private boolean isNetworkAvailable() {
        try {
            return Runtime.getRuntime().exec("ping -c 1 google.com").waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void showLoading() {
        progressDialog.show();
        binding.restoreBtn.setEnabled(false);
    }

    private void hideLoading() {
        progressDialog.dismiss();
        binding.restoreBtn.setEnabled(true);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        handler.removeCallbacksAndMessages(null);
    }
}