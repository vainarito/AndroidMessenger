package com.bsuirproject.androidmessenger.bottomnav.profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.bsuirproject.androidmessenger.R;
import com.bsuirproject.androidmessenger.activity.LoginActivity;
import com.bsuirproject.androidmessenger.databinding.FragmentProfileBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.UUID;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private Uri filePath;
    private ActivityResultLauncher<Intent> pickImageActivityResultLauncher;
    private boolean isUploading = false;
    private SharedPreferences prefs;
    private ValueEventListener userListener;
    private String currentUserId;

    private void statusBarColor() {
        Window window = requireActivity().getWindow();
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white_blue));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        statusBarColor();

        prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupFirebasePersistence();
        initImagePicker();
        setupListeners();
        loadCachedData();
        setupUserListener();

        return binding.getRoot();
    }

    private void setupFirebasePersistence() {
        FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId)
                .keepSynced(true);
    }

    private void initImagePicker() {
        pickImageActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {
                        handleImageSelection(result.getData().getData());
                    }
                });
    }

    private void handleImageSelection(Uri uri) {
        filePath = uri;
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                    requireContext().getContentResolver(),
                    filePath
            );
            if (isAdded() && binding != null) {
                binding.profileImageView.setImageBitmap(bitmap);
                isUploading = true;
                uploadImage();
            }
        } catch (IOException e) {
            Log.e("ProfileFragment", "Ошибка загрузки изображения", e);
            showToast("Ошибка выбора изображения");
        }
    }

    private void setupListeners() {
        binding.profileImageView.setOnClickListener(v -> selectImage());
        binding.logoutBtn.setOnClickListener(v -> performLogout());
    }

    private void loadCachedData() {
        String cachedUsername = prefs.getString("username", "");
        String cachedProfileImage = prefs.getString("profileImage", "");

        if (!cachedUsername.isEmpty()) {
            binding.usernameTv.setText(cachedUsername);
        }

        if (!cachedProfileImage.isEmpty()) {
            loadImageWithGlide(cachedProfileImage);
        }
    }

    private void setupUserListener() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                String username = snapshot.child("username").getValue(String.class);
                String profileImage = snapshot.child("profileImage").getValue(String.class);

                prefs.edit()
                        .putString("username", username)
                        .putString("profileImage", profileImage)
                        .apply();

                updateUI(username, profileImage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Ошибка загрузки: " + error.getMessage());
            }
        };

        FirebaseDatabase.getInstance().getReference()
                .child("Users").child(currentUserId)
                .addValueEventListener(userListener);
    }

    private void updateUI(String username, String profileImage) {
        if (binding == null) return;

        binding.usernameTv.setText(username);
        if (profileImage != null && !profileImage.isEmpty()) {
            loadImageWithGlide(profileImage);
        }
    }

    private void loadImageWithGlide(String url) {
        Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .into(binding.profileImageView);
    }

    private void selectImage() {
        if (!isAdded() || isUploading) {
            showToast(isUploading
                    ? "Дождитесь завершения загрузки"
                    : "Невозможно выбрать изображение");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageActivityResultLauncher.launch(intent);
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        clearEncryptedPrefs();
        clearUserCache();
        navigateToLogin();
    }

    private void clearEncryptedPrefs() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences loginPrefs = EncryptedSharedPreferences.create(
                    "LoginPrefs",
                    masterKeyAlias,
                    requireContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            loginPrefs.edit().clear().apply();
        } catch (Exception e) {
            Log.e("ProfileFragment", "Ошибка очистки настроек", e);
        }
    }

    private void clearUserCache() {
        prefs.edit().clear().apply();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void uploadImage() {
        if (filePath == null || !isAdded()) return;

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images/" + currentUserId + "_" + UUID.randomUUID());

        storageRef.putFile(filePath)
                .addOnSuccessListener(taskSnapshot -> {
                    isUploading = false;
                    showToast("Фото загружено");
                    updateProfileImageUrl(storageRef);
                })
                .addOnFailureListener(e -> {
                    isUploading = false;
                    showToast("Ошибка загрузки: " + e.getMessage());
                });
    }

    private void updateProfileImageUrl(StorageReference storageRef) {
        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            FirebaseDatabase.getInstance().getReference()
                    .child("Users").child(currentUserId)
                    .child("profileImage")
                    .setValue(uri.toString());

            prefs.edit().putString("profileImage", uri.toString()).apply();
            loadImageWithGlide(uri.toString());
        }).addOnFailureListener(e ->
                showToast("Ошибка получения URL: " + e.getMessage()));
    }

    private void showToast(String message) {
        if (isAdded()) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child("Users").child(currentUserId)
                    .removeEventListener(userListener);
        }
        binding = null;
    }
}