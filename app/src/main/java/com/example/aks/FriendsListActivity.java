package com.example.aks;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendsListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FriendsAdapter friendsAdapter;
    private List<User> friendList = new ArrayList<>();
    private String currentUserId;
    private DatabaseReference userRef;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        currentUserId = getIntent().getStringExtra("currentUserId");

        recyclerView = findViewById(R.id.recyclerViewFriends);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        progressBar = findViewById(R.id.progressBar);

        friendsAdapter = new FriendsAdapter(friendList);
        recyclerView.setAdapter(friendsAdapter);

        userRef = FirebaseDatabase.getInstance().getReference("Users");

        loadFriends();
    }

    private void loadFriends() {
        progressBar.setVisibility(View.VISIBLE);

        userRef.child(currentUserId).child("friends").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendList.clear();

                if (snapshot.exists()) {
                    List<String> friendIds = new ArrayList<>();
                    for (DataSnapshot friendSnap : snapshot.getChildren()) {
                        friendIds.add(friendSnap.getKey());
                    }

                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (String friendId : friendIds) {
                                if (dataSnapshot.hasChild(friendId)) {
                                    User friend = dataSnapshot.child(friendId).getValue(User.class);
                                    if (friend != null) {
                                        friendList.add(friend);
                                    }
                                }
                            }
                            friendsAdapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(FriendsListActivity.this, "Error loading friends", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(FriendsListActivity.this, "You have no friends yet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(FriendsListActivity.this, "Error loading friends", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
