package com.example.aks;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.UserViewHolder> {
    private List<User> users;
    private Context context;
    private List<User> searchResults;

    public SearchUserAdapter(List<User> users, Context context) {
        this.users = users;
        this.context = context;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_card, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.usernameTextView.setText(user.getUsername()); // Set the username text
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateUsers(List<User> updatedUsers) {
        this.searchResults = users;
        notifyDataSetChanged();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.textUsername);
        }
    }
}

