package com.example.aks;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FriendRequestAdapter adapter;
    private List<User> requestList = new ArrayList<>();
    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        recyclerView = findViewById(R.id.recyclerViewRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendRequestAdapter(requestList, currentUserId);
        recyclerView.setAdapter(adapter);

        loadIncomingRequests();
    }

    private void loadIncomingRequests() {
        FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUserId)
                .child("friendRequests")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> incomingIds = new ArrayList<>();

                        for (DataSnapshot req : snapshot.getChildren()) {
                            if ("pending".equals(req.getValue(String.class))) {
                                incomingIds.add(req.getKey());
                            }
                        }

                        fetchRequestUserDetails(incomingIds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchRequestUserDetails(List<String> userIds) {
        requestList.clear();

        FirebaseDatabase.getInstance().getReference("Users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (String id : userIds) {
                            if (snapshot.hasChild(id)) {
                                User user = snapshot.child(id).getValue(User.class);
                                user.setId(id);
                                requestList.add(user);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(FriendRequestsActivity.this, "Error loading requests", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
