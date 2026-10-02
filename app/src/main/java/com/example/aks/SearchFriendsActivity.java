package com.example.aks;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SearchFriendsActivity extends AppCompatActivity {

    private EditText edtSearch;
    private RecyclerView recyclerViewFriends, recyclerViewSearchResults;
    private FriendAdapter friendAdapter, searchAdapter;
    private List<User> userList = new ArrayList<>();
    private List<User> searchResults = new ArrayList<>();
    private String currentUserId ;
    private View viewDot;
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_friends);

        edtSearch = findViewById(R.id.edtSearch);
        recyclerViewFriends = findViewById(R.id.recyclerViewFriends);
        recyclerViewSearchResults = findViewById(R.id.recyclerViewSearchResults);
        viewDot = findViewById(R.id.viewDot);

        recyclerViewFriends.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnFriendRequests).setOnClickListener(v -> {
            startActivity(new Intent(SearchFriendsActivity.this, FriendRequestsActivity.class));
        });

        currentUserId = getIntent().getStringExtra("userId");

        friendAdapter = new FriendAdapter(userList, this, false);
        recyclerViewFriends.setAdapter(friendAdapter);

        searchAdapter = new FriendAdapter(searchResults, this, true);
        recyclerViewSearchResults.setAdapter(searchAdapter);
        
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    if (!query.isEmpty()) {
                        recyclerViewSearchResults.setVisibility(View.VISIBLE);
                        recyclerViewFriends.setVisibility(View.GONE);
                        searchUsers(query);
                    } else {
                        recyclerViewSearchResults.setVisibility(View.GONE);
                        recyclerViewFriends.setVisibility(View.VISIBLE);
                        loadAcceptedFriends();
                    }
                };

                searchHandler.postDelayed(searchRunnable, 300); // Debounce
            }
        });

        loadAcceptedFriends();
        checkIncomingRequests();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_friends);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int selectedItemId = item.getItemId();
            Intent intent = null;

            if (selectedItemId == R.id.nav_groups) {
                intent = new Intent(SearchFriendsActivity.this, MyGroupsActivity.class);
            } else if (selectedItemId == R.id.nav_map) {
                intent = new Intent(SearchFriendsActivity.this, secondgroup.class);
            } else if (selectedItemId == R.id.nav_friends) {
                return true; // Already here
            }

            if (intent != null) {
                startActivity(intent);
                finish();
                return true;
            }

            return false;
        });
    }

    private void searchUsers(String query) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.orderByChild("username").startAt(query).endAt(query + "\uf8ff")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        searchResults.clear();
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            User user = userSnapshot.getValue(User.class);
                            user.setId(userSnapshot.getKey());
                            if (!user.getId().equals(currentUserId)) {
                                searchResults.add(user);
                            }
                        }
                        searchAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SearchFriendsActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAcceptedFriends() {
        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUserId).child("friendRequests");

        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String friendId = snap.getKey();
                    String status = snap.getValue(String.class);

                    if ("accepted".equals(status)) {
                        FirebaseDatabase.getInstance().getReference("Users")
                                .child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        User user = dataSnapshot.getValue(User.class);
                                        if (user != null && !containsUser(userList, user.getId())) {
                                            user.setId(friendId);
                                            userList.add(user);
                                            friendAdapter.notifyDataSetChanged();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                                });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkIncomingRequests() {
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUserId).child("friendRequests");

        requestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasRequests = false;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    if ("pending".equals(snap.getValue(String.class))) {
                        hasRequests = true;
                        break;
                    }
                }
                viewDot.setVisibility(hasRequests ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void sendFriendRequest(String foundUserId) {
        DatabaseReference friendRequestsRef = FirebaseDatabase.getInstance().getReference("Users");

        friendRequestsRef.child(foundUserId).child("friendRequests").child(currentUserId).setValue("pending")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SearchFriendsActivity.this, "Friend request sent!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SearchFriendsActivity.this, "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        friendRequestsRef.child(currentUserId).child("friendRequests").child(foundUserId).setValue("sent");
    }

    private boolean containsUser(List<User> list, String userId) {
        for (User u : list) {
            if (u.getId().equals(userId)) return true;
        }
        return false;
    }
}
