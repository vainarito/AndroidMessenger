package com.bsuirproject.androidmessenger.bottomnav.new_chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bsuirproject.androidmessenger.R;
import com.bsuirproject.androidmessenger.databinding.FragmentNewChatBinding;
import com.bsuirproject.androidmessenger.users.User;
import com.bsuirproject.androidmessenger.users.UsersAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class NewChatFragment extends Fragment {
    private FragmentNewChatBinding binding;
    private ArrayList<User> allUsers = new ArrayList<>(); // Список всех пользователей
    private ArrayList<User> filteredUsers = new ArrayList<>(); // Отфильтрованный список пользователей
    private UsersAdapter adapter;

    /**
     * Изменение цвета строки состояния
     */
    private void statusBarColor() {
        Window window = requireActivity().getWindow();
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white_blue));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewChatBinding.inflate(inflater, container, false);

        statusBarColor(); // Установка цвета строки состояния
        setupRecyclerView(); // Настройка RecyclerView
        setupSearchUser(); // Подключение поисковой строки
        loadUsers(); // Загрузка пользователей

        return binding.getRoot();
    }

    /**
     * Настройка RecyclerView и адаптера
     */
    private void setupRecyclerView() {
        adapter = new UsersAdapter(filteredUsers); // Подключение адаптера
        binding.usersRv.setLayoutManager(new LinearLayoutManager(getContext())); // Линейная разметка
        binding.usersRv.setAdapter(adapter);
    }

    /**
     * Настройка строки поиска (EditText)
     */
    private void setupSearchUser() {
        binding.searchuser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString()); // Фильтрация при каждом изменении текста
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Фильтрация пользователей по введённой строке
     *
     * @param query строка, введённая пользователем
     */
    private void filterUsers(String query) {
        filteredUsers.clear(); // Очистка списка перед обновлением
        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers); // Если строка пустая, показываем всех пользователей
        } else {
            for (User user : allUsers) {
                if (user.getUsername().toLowerCase().contains(query.toLowerCase())) {
                    filteredUsers.add(user); // Добавляем только тех, чьи имена соответствуют запросу
                }
            }
        }
        adapter.notifyDataSetChanged(); // Уведомляем адаптер об изменении данных
    }

    /**
     * Загрузка всех пользователей из Firebase
     */
    private void loadUsers() {
        FirebaseDatabase.getInstance().getReference().child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allUsers.clear(); // Очистка списка перед обновлением
                filteredUsers.clear(); // Очистка списка фильтрованных пользователей

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    if (userSnapshot.getKey().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        continue; // Пропускаем текущего пользователя
                    }

                    String uid = userSnapshot.getKey();
                    String username = userSnapshot.child("username").getValue(String.class);
                    String profileImage = userSnapshot.child("profileImage").getValue(String.class);

                    if (username != null) {
                        allUsers.add(new User(uid, username, profileImage)); // Добавление пользователя в список
                    }
                }

                // Копируем данные из allUsers в filteredUsers
                filteredUsers.addAll(allUsers);

                // Обновляем адаптер
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Обработка ошибок при чтении из базы
            }
        });
    }
}
