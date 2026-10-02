package com.example.aks;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MyGroupsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private RecyclerView recyclerView;
    private GroupAdapter groupAdapter;
    private ArrayList<HashMap<String, String>> groupList = new ArrayList<>();
    private DatabaseReference userRef;
    private SharedPreferences preferences;
    private String userId;
    private SearchView searchView;

    private ImageView userImage;  // ImageView for the user image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_groups);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewGroups);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchView = findViewById(R.id.searchViewGroups);
        userImage = findViewById(R.id.userimage);  // Find the user image view

        // Set click listener on the user image to open the SettingsActivity
        userImage.setOnClickListener(v -> {
            Intent intent = new Intent(MyGroupsActivity.this, settings.class);
            startActivity(intent);  // Navigate to settings page
        });

        // Get userId from SharedPreferences
        preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        userId = preferences.getString("userId", "");

        if(userId.isEmpty()){
            userId = preferences.getString("sp_userId", "");
        }
        // Reference to user's joined groups
        userRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(userId).child("joinedGroups");

        // Initialize group adapter
        groupAdapter = new GroupAdapter(this, groupList, group -> {
            // Check location permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            String groupId = group.get("groupId");

            // Start location tracking service
            Intent serviceIntent = new Intent(this, LocationService.class);
            serviceIntent.putExtra("groupId", groupId);
            ContextCompat.startForegroundService(this, serviceIntent);

            // Move to members activity
            Intent intent = new Intent(MyGroupsActivity.this, GroupMembersActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("groupName", group.get("groupName"));
            startActivity(intent);
        });

        recyclerView.setAdapter(groupAdapter);

        // Fetch joined groups from Firebase
        fetchJoinedGroups();

        // Search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String query) {
                groupAdapter.filter(query);
                return false;
            }

            public boolean onQueryTextChange(String newText) {
                groupAdapter.filter(newText);
                return false;
            }
        });
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

// Set the current selected item to groups
        bottomNavigationView.setSelectedItemId(R.id.nav_groups); // <-- Add this line

// Set listener for Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int selectedItemId = item.getItemId();
            Intent intent = null;

            if (selectedItemId == R.id.nav_groups) {
                intent = new Intent(MyGroupsActivity.this, MyGroupsActivity.class);
            } else if (selectedItemId == R.id.nav_friends) {
                intent = new Intent(MyGroupsActivity.this, SearchFriendsActivity.class);

            } else if (selectedItemId == R.id.nav_map) {
                intent = new Intent(MyGroupsActivity.this, secondgroup.class);
            }

            if (intent != null) {
                intent.putExtra("userId",userId);
                startActivity(intent);
                finish();
                return true;
            }

            return false;
        });

    }

    // Fetch the user's joined groups
    private void fetchJoinedGroups() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String groupId = snap.child("groupId").getValue(String.class);
                    String groupName = snap.child("groupName").getValue(String.class);

                    if (groupId != null && groupName != null) {
                        DatabaseReference groupRef = FirebaseDatabase.getInstance()
                                .getReference("Groups").child(groupId).child("Members");

                        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            public void onDataChange(@NonNull DataSnapshot memberSnap) {
                                HashMap<String, String> map = new HashMap<>();
                                map.put("groupId", groupId);
                                map.put("groupName", groupName);
                                groupList.add(map);
                                groupAdapter.updateData(groupList);
                            }

                            public void onCancelled(@NonNull DatabaseError error) { }
                        });
                    }
                }
            }

            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyGroupsActivity.this, "Failed to fetch groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted! Try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
