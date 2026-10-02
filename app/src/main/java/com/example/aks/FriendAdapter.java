package com.example.aks;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    private List<User> userList;
    private Context context;
    private boolean isSearchResult;

    public FriendAdapter(List<User> userList, Context context, boolean isSearchResult) {
        this.userList = userList;
        this.context = context;
        this.isSearchResult = isSearchResult;
    }

    @NonNull
    @Override
    public FriendAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendAdapter.ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.username.setText(user.getUsername());

        if (isSearchResult) {
            holder.btnAction.setText("Send Request");
            holder.btnAction.setOnClickListener(v -> {
                if (context instanceof SearchFriendsActivity) {
                    ((SearchFriendsActivity) context).sendFriendRequest(user.getId());
                }
            });
        } else {
            holder.btnAction.setText("Chat");
            holder.btnAction.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("friendId", user.getId());
                intent.putExtra("friendName", user.getUsername());
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        Button btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.usernameTextView);
            btnAction = itemView.findViewById(R.id.actionButton);
        }
    }
}
