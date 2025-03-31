package com.bsuirproject.androidmessenger.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import com.bsuirproject.androidmessenger.R;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private List<Message> messages;

    public MessagesAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        // Установка текста сообщения и даты
        holder.messageTv.setText(message.getText());
        holder.dateTv.setText(message.getDate());

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (!message.getOwnerId().equals(currentUserId) && holder.profileImageView != null) {
            // Загружаем фото отправителя сообщения
            loadUserProfileImage(message.getOwnerId(), holder.profileImageView);
        } else if (holder.profileImageView != null) {
            // Для сообщений от текущего пользователя скрываем фото
            holder.profileImageView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getOwnerId().equals(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()))
            return R.layout.message_from_curr_user_rv_item;
        else
            return R.layout.message_rv_item;
    }

    private void loadUserProfileImage(String userId, CircleImageView profileImageView) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        usersRef.child("profileImage").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String profileImageUrl = snapshot.getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(profileImageView.getContext())
                                .load(profileImageUrl)
                                .placeholder(R.drawable.baseline_face_24)
                                .error(R.drawable.baseline_face_24)
                                .into(profileImageView);
                    } else {
                        profileImageView.setImageResource(R.drawable.baseline_face_24);
                    }
                } else {
                    profileImageView.setImageResource(R.drawable.baseline_face_24);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                profileImageView.setImageResource(R.drawable.baseline_face_24);
            }
        });
    }

    // ViewHolder для элементов RecyclerView
    static class MessageViewHolder extends RecyclerView.ViewHolder {

        TextView messageTv, dateTv;
        CircleImageView profileImageView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            messageTv = itemView.findViewById(R.id.message_tv);
            dateTv = itemView.findViewById(R.id.message_date_tv);
            profileImageView = itemView.findViewById(R.id.profile_iv4); // Убедитесь, что ID совпадает
        }
    }
}