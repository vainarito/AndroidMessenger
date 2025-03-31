package com.bsuirproject.androidmessenger.bottomnav.chats;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bsuirproject.androidmessenger.R;
import com.bsuirproject.androidmessenger.activity.ChatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

import com.bsuirproject.androidmessenger.chats.Chat;
import com.bsuirproject.androidmessenger.chats.ChatsAdapter;
import com.bsuirproject.androidmessenger    .databinding.FragmentChatsBinding;

public class ChatsFragment extends Fragment {
    private FragmentChatsBinding binding;
    private void statusBarColor() {
        Window window = requireActivity().getWindow();
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white_blue));
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);

        loadChats();

        return binding.getRoot();
    }

    private void loadChats() {
        ArrayList<Chat> chats = new ArrayList<>();

        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Get the current user's ID
                String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                // Check if the user has a "chats" node
                DataSnapshot chatsSnapshot = snapshot.child("Users").child(uid).child("chats");
                if (chatsSnapshot.exists() && chatsSnapshot.getValue() != null) {
                    String chatsStr = chatsSnapshot.getValue().toString();
                    String[] chatsIds = chatsStr.split(",");

                    ArrayList<Chat> chats = new ArrayList<>();

                    for (String chatId : chatsIds) {
                        DataSnapshot chatSnapshot = snapshot.child("Chats").child(chatId);
                        if (chatSnapshot.exists()) {
                            String userId1 = chatSnapshot.child("user1").getValue(String.class);
                            String userId2 = chatSnapshot.child("user2").getValue(String.class);

                            if (userId1 != null && userId2 != null) {
                                String chatUserId = uid.equals(userId1) ? userId2 : userId1;
                                String chatName = snapshot.child("Users").child(chatUserId).child("username").getValue(String.class);

                                if (chatName != null) {
                                    Chat chat = new Chat(chatId, chatName, userId1, userId2);
                                    chats.add(chat);
                                } else {
                                    Log.w("ChatsFragment", "Chat name is null for userId: " + chatUserId);
                                }
                            } else {
                                Log.w("ChatsFragment", "User IDs are null for chatId: " + chatId);
                            }
                        } else {
                            Log.w("ChatsFragment", "Chat does not exist for chatId: " + chatId);
                        }
                    }

                    // Update the RecyclerView
                    binding.chatsRv.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.chatsRv.setAdapter(new ChatsAdapter(chats));
                } else {
                    // Handle case where the user has no chats
                    Toast.makeText(getContext(), "У вас нет чатов", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Не удалось получить чаты", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
