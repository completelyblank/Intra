package com.example.intra;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final List<Chat> chatList;
    private OnChatClickListener onChatClickListener;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatAdapter(List<Chat> chatList, OnChatClickListener onChatClickListener) {
        this.chatList = chatList;
        this.onChatClickListener = onChatClickListener;
    }

    public ChatAdapter(List<Chat> chatList) {
        this(chatList, null);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        holder.contactName.setText(chat.getContactName());

        if (chat.isUserOnline()) {
            holder.statusCircle.setImageResource(R.drawable.circle_online);
        } else {
            holder.statusCircle.setImageResource(R.drawable.circle_offline);
        }

        holder.lastMessage.setText(chat.getLastMessage());

        if (chat.getLastMessageTimestamp() > 0) {
            long timestamp = chat.getLastMessageTimestamp();
            Date messageDate = new Date(timestamp);

            // Today's date
            Date today = new Date();
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

            String messageDay = dayFormat.format(messageDate);
            String todayDay = dayFormat.format(today);

            String formattedTime;
            if (messageDay.equals(todayDay)) {
                // Today → show "Today" + time
                formattedTime = "Today, " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(messageDate);
            } else {
                // Not today → show date + time
                formattedTime = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(messageDate);
            }



            holder.timestamp.setText(formattedTime);
        } else {
            holder.timestamp.setText("");
        }


        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && chat.getLastSenderId() != null &&
                chat.getLastSenderId().equals(currentUser.getUid())) {
            holder.doubleTickIcon.setVisibility(View.VISIBLE);
        } else {
            holder.doubleTickIcon.setVisibility(View.GONE);
        }

        // Load profile picture using Glide with ProgressBar
        if (chat.getProfilePicURL() != null && !chat.getProfilePicURL().isEmpty()) {
            holder.profileLoadingSpinner.setVisibility(View.VISIBLE);
            Glide.with(holder.profilePicture.getContext())
                    .load(chat.getProfilePicURL())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            holder.profileLoadingSpinner.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            holder.profileLoadingSpinner.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(holder.profilePicture);
        } else {
            holder.profileLoadingSpinner.setVisibility(View.GONE);
            holder.profilePicture.setImageResource(R.drawable.ic_person);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onChatClickListener != null) {
                onChatClickListener.onChatClick(chat);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView contactName, lastMessage, timestamp;
        ImageView profilePicture, optionsIcon, doubleTickIcon, statusCircle;
        ProgressBar profileLoadingSpinner;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            timestamp = itemView.findViewById(R.id.timestamp);
            profilePicture = itemView.findViewById(R.id.profile_picture);
            optionsIcon = itemView.findViewById(R.id.options_icon);
            doubleTickIcon = itemView.findViewById(R.id.double_tick_icon);
            statusCircle = itemView.findViewById(R.id.status_circle);
            profileLoadingSpinner = itemView.findViewById(R.id.profile_loading_spinner);
        }
    }
}