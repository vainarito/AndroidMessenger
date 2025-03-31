    package com.bsuirproject.androidmessenger.activity;

    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.content.ContextCompat;
    import androidx.recyclerview.widget.LinearLayoutManager;

    import android.content.Intent;
    import android.os.Bundle;
    import android.view.View;
    import android.view.Window;
    import android.widget.Toast;

    import com.bsuirproject.androidmessenger.R;
    import com.bsuirproject.androidmessenger.databinding.ActivityChatBinding;
    import com.bsuirproject.androidmessenger.message.Message;
    import com.bsuirproject.androidmessenger.message.MessagesAdapter;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;
    import com.bumptech.glide.Glide;

    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Date;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Objects;

    public class ChatActivity extends AppCompatActivity {

        private ActivityChatBinding binding;
        private MessagesAdapter messagesAdapter;
        private List<Message> messages = new ArrayList<>();
        private LinearLayoutManager linearLayoutManager; // Для управления прокруткой

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            binding = ActivityChatBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            statusBarColor();

            String chatId = getIntent().getStringExtra("chatId");

            // Инициализация RecyclerView
            linearLayoutManager = new LinearLayoutManager(this);
            linearLayoutManager.setStackFromEnd(true); // Для автоматической прокрутки вниз
            binding.messagesRv.setLayoutManager(linearLayoutManager);
            messagesAdapter = new MessagesAdapter(messages);
            binding.messagesRv.setAdapter(messagesAdapter);

            loadProfileImage(chatId); // Загрузка изображения профиля и имени пользователя
            loadMessages(chatId); // Загрузка сообщений

            // Обработчик для кнопки возврата
            binding.arrowBtn2.setOnClickListener(v ->
                    startActivity(new Intent(ChatActivity.this, MainActivity.class))
            );

            // Обработчик отправки сообщения
            binding.sendMessageBtn.setOnClickListener(v -> {
                String message = binding.messageEt.getText().toString();
                if (message.isEmpty()) {
                    Toast.makeText(this, "Message field cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm dd.MM.yyyy");
                String date = simpleDateFormat.format(new Date());

                binding.messageEt.setText(""); // Очистка поля ввода
                sendMessage(chatId, message, date);
            });
        }

        // Установка цвета статус-бара
        private void statusBarColor() {
            Window window = ChatActivity.this.getWindow();
            window.setStatusBarColor(ContextCompat.getColor(ChatActivity.this, R.color.white_blue));
        }

        // Отправка сообщения
        private void sendMessage(String chatId, String message, String date) {
            if (chatId == null) return;

            HashMap<String, String> messageInfo = new HashMap<>();
            messageInfo.put("text", message);
            messageInfo.put("ownerId", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
            messageInfo.put("date", date);

            FirebaseDatabase.getInstance().getReference()
                    .child("Chats")
                    .child(chatId)
                    .child("messages")
                    .push()
                    .setValue(messageInfo);
        }

        // Загрузка сообщений
        private void loadMessages(String chatId) {
            if (chatId == null) return;

            FirebaseDatabase.getInstance().getReference()
                    .child("Chats")
                    .child(chatId)
                    .child("messages")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) return;

                            messages.clear(); // Очистка списка перед загрузкой новых сообщений
                            for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                                String messageId = messageSnapshot.getKey();
                                String ownerId = messageSnapshot.child("ownerId").getValue(String.class);
                                String text = messageSnapshot.child("text").getValue(String.class);
                                String date = messageSnapshot.child("date").getValue(String.class);

                                if (ownerId != null && text != null && date != null) {
                                    messages.add(new Message(messageId, ownerId, text, date, chatId));
                                }
                            }

                            messagesAdapter.notifyDataSetChanged(); // Обновление адаптера
                            binding.messagesRv.scrollToPosition(messages.size() - 1); // Прокрутка вниз
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Обработка ошибок
                        }
                    });
        }

        // Загрузка изображения профиля и имени user2
        private void loadProfileImage(String chatId) {
            if (chatId == null) return;

            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

            // Получение данных чата
            database.child("Chats").child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) return;

                    String user1Id = snapshot.child("user1").getValue(String.class);
                    String user2Id = snapshot.child("user2").getValue(String.class);

                    if (user1Id == null || user2Id == null) {
                        Toast.makeText(ChatActivity.this, "Chat data incomplete", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Определяем ID собеседника
                    String interlocutorId = currentUserId.equals(user1Id) ? user2Id : user1Id;

                    // Загружаем данные собеседника
                    database.child("Users").child(interlocutorId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) return;

                            String profileImageUrl = snapshot.child("profileImage").getValue(String.class);
                            String username = snapshot.child("username").getValue(String.class);

                            // Загрузка фото профиля
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(ChatActivity.this)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.baseline_face_24)
                                        .error(R.drawable.baseline_face_24)
                                        .circleCrop()
                                        .into(binding.profileIv3);
                            } else {
                                binding.profileIv3.setImageResource(R.drawable.baseline_face_24);
                            }

                            // Установка имени пользователя
                            if (username != null) {
                                binding.usernameTv2.setText(username);
                            } else {
                                binding.usernameTv2.setText("Unknown User");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(ChatActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ChatActivity.this, "Failed to load chat data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
