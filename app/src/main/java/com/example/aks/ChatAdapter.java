package com.example.aks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private ArrayList<Message> messages;
    private String currentUserId;

    public ChatAdapter(ArrayList<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).senderId.equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String time = sdf.format(new Date(message.timestamp));

        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).textMessage.setText(message.text);
            ((SentViewHolder) holder).textTime.setText(time);
        } else {
            ((ReceivedViewHolder) holder).textMessage.setText(message.text);
            ((ReceivedViewHolder) holder).textSender.setText(message.senderName);
            ((ReceivedViewHolder) holder).textTime.setText(time);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textSentMessage);
            textTime = itemView.findViewById(R.id.textSentTime);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textSender, textTime;

        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textReceivedMessage);
            textSender = itemView.findViewById(R.id.textSenderName);
            textTime = itemView.findViewById(R.id.textReceivedTime);
        }
    }
}
