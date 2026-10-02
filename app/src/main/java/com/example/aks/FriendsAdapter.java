package com.example.aks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendsViewHolder> {

    private List<User> friendsList;

    public FriendsAdapter(List<User> friendsList) {
        this.friendsList = friendsList;
    }

    @NonNull
    @Override
    public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friends, parent, false);
        return new FriendsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendsViewHolder holder, int position) {
        User friend = friendsList.get(position);
        holder.username.setText(friend.getUsername());

    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {

        TextView username;
        ImageView profileImage;

        public FriendsViewHolder(View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.friendName);
            profileImage = itemView.findViewById(R.id.profileImage);
        }
    }
}
