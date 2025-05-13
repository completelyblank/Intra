package com.example.intra;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<MessageItem> messageItems;

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_DATE_HEADER = 3;

    public MessageAdapter(List<MessageItem> messageItems) {
        this.messageItems = messageItems;
    }

    @Override
    public int getItemViewType(int position) {
        MessageItem item = messageItems.get(position);
        if (item instanceof MessageItem.DateHeader) {
            return VIEW_TYPE_DATE_HEADER;
        } else if (item instanceof MessageItem.MessageWrapper) {
            MessageItem.MessageWrapper messageWrapper = (MessageItem.MessageWrapper) item;
            return messageWrapper.isSentByCurrentUser() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
        }
        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageItem item = messageItems.get(position);
        if (holder instanceof SentMessageViewHolder && item instanceof MessageItem.MessageWrapper) {
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            MessageItem.MessageWrapper messageWrapper = (MessageItem.MessageWrapper) item;
            sentHolder.messageText.setText(messageWrapper.getText());
            sentHolder.timeText.setText(messageWrapper.getFormattedTime());
        } else if (holder instanceof ReceivedMessageViewHolder && item instanceof MessageItem.MessageWrapper) {
            ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
            MessageItem.MessageWrapper messageWrapper = (MessageItem.MessageWrapper) item;
            receivedHolder.messageText.setText(messageWrapper.getText());
            receivedHolder.timeText.setText(messageWrapper.getFormattedTime());
        } else if (holder instanceof DateHeaderViewHolder && item instanceof MessageItem.DateHeader) {
            DateHeaderViewHolder dateHolder = (DateHeaderViewHolder) holder;
            MessageItem.DateHeader dateHeader = (MessageItem.DateHeader) item;
            dateHolder.dateText.setText(dateHeader.getDate());
        }
    }

    @Override
    public int getItemCount() {
        return messageItems.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
        }
    }

    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;

        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.text_date_header);
        }
    }
}