package com.example.aks;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    private List<User> requestList;
    private String currentUserId;

    public FriendRequestAdapter(List<User> requestList, String currentUserId) {
        this.requestList = requestList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        User user = requestList.get(position);
        holder.txtFriendName.setText(user.getUsername());

        holder.btnAccept.setOnClickListener(v -> {
            if (user == null || user.getId() == null) return;

            FirebaseDatabase.getInstance().getReference("Users")
                    .child(currentUserId).child("friends").child(user.getId()).setValue(true);

            FirebaseDatabase.getInstance().getReference("Users")
                    .child(user.getId()).child("friends").child(currentUserId).setValue(true);

            FirebaseDatabase.getInstance().getReference("Users")
                    .child(currentUserId).child("friendRequests").child(user.getId()).removeValue();

            FirebaseDatabase.getInstance().getReference("Users")
                    .child(user.getId()).child("friendRequests").child(currentUserId).removeValue();

            if (position >= 0 && position < requestList.size()) {
                requestList.remove(position);
                notifyItemRemoved(position);
            }

            Toast.makeText(holder.itemView.getContext(), "Friend request accepted!", Toast.LENGTH_SHORT).show();

            Context context = holder.itemView.getContext();
            Intent intent = new Intent(context, FriendsListActivity.class);
            intent.putExtra("currentUserId", currentUserId);
            context.startActivity(intent);
        });


        holder.btnDecline.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference("Users")
                    .child(currentUserId).child("friendRequests").child(user.getId()).removeValue();

            FirebaseDatabase.getInstance().getReference("Users")
                    .child(user.getId()).child("friendRequests").child(currentUserId).removeValue();

            requestList.remove(position);
            notifyItemRemoved(position);
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView txtFriendName;
        Button btnAccept, btnDecline;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            txtFriendName = itemView.findViewById(R.id.txtFriendName);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}
