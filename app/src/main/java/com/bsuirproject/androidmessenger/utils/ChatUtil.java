package com.bsuirproject.androidmessenger.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import com.bsuirproject.androidmessenger.users.User;

public class ChatUtil {

    public static void createChat(User user) {
        // Получаем UID текущего пользователя
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Генерируем уникальный chatId
        String chatId = generateChatId(uid, user.uid);

        // Проверяем, существует ли уже чат между текущим пользователем и собеседником
        FirebaseDatabase.getInstance().getReference().child("Chats").child(chatId)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            // Чат уже существует, выводим сообщение
                            Log.w("ChatUtil", "Чат уже существует между этими пользователями");
                            return; // Не создаем чат
                        } else {
                            // Чат не существует, создаем новый
                            createNewChat(uid, user, chatId);
                        }
                    } else {
                        Log.w("ChatUtil", "Ошибка при проверке существования чата");
                    }
                });
    }

    // Метод для генерации уникального идентификатора чата, всегда в одном и том же порядке
    private static String generateChatId(String userId1, String userId2) {
        // Сначала соединяем UID текущего пользователя и собеседника
        String sumUser1User2 = userId1 + userId2;
        char[] charArray = sumUser1User2.toCharArray();
        Arrays.sort(charArray); // Сортируем для гарантии одинакового chatId для двух пользователей

        return new String(charArray);
    }

    // Метод для создания нового чата
    private static void createNewChat(String uid, User user, String chatId) {
        // Создаем HashMap для хранения информации о чате
        HashMap<String, String> chatInfo = new HashMap<>();
        // Записываем текущего пользователя в поле user1
        chatInfo.put("user1", uid);
        // Записываем собеседника в поле user2
        chatInfo.put("user2", user.uid);

        // Сохраняем информацию о чате в Firebase
        FirebaseDatabase.getInstance().getReference().child("Chats").child(chatId)
                .setValue(chatInfo);

        // Добавляем chatId в список чатов для текущего пользователя и собеседника
        addChatIdToUser(uid, chatId);
        addChatIdToUser(user.uid, chatId);
    }

    // Метод для добавления chatId в список чатов пользователя
    private static void addChatIdToUser(String uid, String chatId) {
        FirebaseDatabase.getInstance().getReference().child("Users").child(uid)
                .child("chats").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Получаем текущий список чатов пользователя
                        String chats = task.getResult().getValue() != null ? task.getResult().getValue().toString() : "";

                        // Обновляем список чатов, добавляя новый chatId
                        String chatsUpd = addIdToStr(chats, chatId);

                        // Сохраняем обновленный список чатов
                        FirebaseDatabase.getInstance().getReference().child("Users").child(uid)
                                .child("chats").setValue(chatsUpd);
                    }
                });
    }

    // Метод для добавления нового chatId в строку, представляющую список чатов
    private static String addIdToStr(String str, String chatId) {
        // Если строка пуста, добавляем первый chatId, если нет — добавляем через запятую
        str += (str.isEmpty()) ? chatId : ("," + chatId);
        return str;
    }
}
