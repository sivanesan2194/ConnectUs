package com.example.aks;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;

public class GroupMembersActivity extends AppCompatActivity {

    Button button;
    SharedPreferences prefs;
    private RecyclerView recyclerView;
    private MemberAdapter adapter;
    private ArrayList<String> memberList = new ArrayList<>();
    private DatabaseReference groupRef;
    private String groupId, groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_members);

        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");

        prefs=getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        TextView groupNameText = findViewById(R.id.groupNameText);
        groupNameText.setText(groupName);

        recyclerView = findViewById(R.id.recyclerViewMembers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MemberAdapter(memberList);
        recyclerView.setAdapter(adapter);

        groupRef = FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Members");

        fetchGroupMembers();

        button = findViewById(R.id.buttonViewMap);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(GroupMembersActivity.this, GroupMapActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("groupname", groupName);
            startActivity(intent);
        });

        findViewById(R.id.fabMessage).setOnClickListener(v -> {
            Intent intent = new Intent(GroupMembersActivity.this, ChatActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("groupName", groupName);
            startActivity(intent);
        });

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(this::showPopupMenu);
    }

    private void fetchGroupMembers() {
        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String username = snap.child("username").getValue(String.class);
                    String role = snap.child("role").getValue(String.class);

                    if(username != null && role != null) {
                        memberList.add(username + " (" + role + ")");
                    } else {
                        Log.e("GroupMembers", "Missing username/role for: " + snap.getKey());
                    }
                }
                Collections.sort(memberList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GroupMembers", "Error: " + error.getMessage());
            }
        });
    }


    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.group_menu, popupMenu.getMenu());

        String userId = prefs.getString("username", "");

        if(userId == null){

            userId = prefs.getString("sp_username", "");
        }
        userId = userId.toUpperCase();

        DatabaseReference groupRootRef = FirebaseDatabase.getInstance().getReference("Groups")
                .child(groupId).child("createdBy");

        String finalUserId = userId;

        groupRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String creatorId = snapshot.getValue(String.class);

                    if (creatorId != null && !creatorId.equals(finalUserId)) {
                        // If current user is NOT the creator, hide delete option
                        popupMenu.getMenu().findItem(R.id.menu_delete_group).setVisible(false);
                        popupMenu.getMenu().findItem(R.id.menu_kick_member).setVisible(false);
                    }
                } else {
                    Log.e("GroupCheck", "createdBy field does not exist for groupId: " + groupId);
                    // Safer fallback: hide delete option if creator info not found
                    popupMenu.getMenu().findItem(R.id.menu_delete_group).setVisible(false);
                    popupMenu.getMenu().findItem(R.id.menu_kick_member).setVisible(false);
                }

                // **IMPORTANT: Show popup only after check is done**
                popupMenu.show();

                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_group_info) {
                        showGroupInfo();
                        return true;
                    } else if (item.getItemId() == R.id.menu_leave_group) {
                        leaveGroup();
                        return true;
                    } else if (item.getItemId() == R.id.menu_delete_group) {
                        deleteGroup();
                        return true;
                    }else if (item.getItemId() == R.id.menu_kick_member) {
                        kickMember();
                        return true;
                    }

                    return false;
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GroupCheck", "Database error: " + error.getMessage());
            }
        });
    }

    private void kickMember() {
        // Show a dialog to input the username of the member to kick
        final EditText editText = new EditText(this);
        editText.setHint("Enter the username of the member to kick");

        new AlertDialog.Builder(this)
                .setTitle("Kick Member")
                .setMessage("Enter the username of the member you want to kick:")
                .setView(editText)
                .setPositiveButton("Kick", (dialog, which) -> {
                    // Get the member's username from input
                    String memberUsernameToKick = editText.getText().toString().trim().toUpperCase();

                    if (memberUsernameToKick.isEmpty()) {
                        Toast.makeText(GroupMembersActivity.this, "Please enter a valid username.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Find the member by username and remove from the group
                    DatabaseReference groupMembersRef = FirebaseDatabase.getInstance().getReference("Groups")
                            .child(groupId).child("Members");

                    groupMembersRef.orderByChild("username").equalTo(memberUsernameToKick).addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                                            String memberId = memberSnapshot.getKey();

                                            // Remove the member from the group
                                            groupMembersRef.child(memberId).removeValue();
                                            DatabaseReference userRef = FirebaseDatabase.getInstance()
                                                    .getReference("Users").child(memberId).child("joinedGroups").child(groupId);
                                            userRef.removeValue(); // Remove the group from the member's joined groups

                                            Toast.makeText(GroupMembersActivity.this, memberUsernameToKick + " has been kicked from the group.", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(GroupMembersActivity.this, "Member not found.", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(GroupMembersActivity.this, "Failed to find the member.", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGroupInfo() {View view = getLayoutInflater().inflate(R.layout.bottom_sheet_group_info, null);
        TextView nameText = view.findViewById(R.id.groupNameText);
        TextView idText = view.findViewById(R.id.groupIdText);
        TextView creatorText = view.findViewById(R.id.groupCreatorText);
        ImageView copy = view.findViewById(R.id.copyIcon);

        nameText.setText("Group Name: " + groupName);

        DatabaseReference groupRootRef = FirebaseDatabase.getInstance().getReference("Groups")
                .child(groupId).child("createdBy");

        groupRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String creatorId = snapshot.getValue(String.class);

                if (creatorId != null) {
                    String currentUserId = prefs.getString("username", "");

                    if (currentUserId.isEmpty()) {
                        currentUserId = prefs.getString("sp_userName", "");
                    }

                    final String acurrentUserId = currentUserId.toUpperCase();

                    // First set the admin name
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Groups")
                            .child(groupId).child("createdBy");
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String adminName = snapshot.getValue(String.class);
                            if (adminName != null) {
                                creatorText.setText("Created by: " + adminName);
                            } else {
                                creatorText.setText("Created by: Unknown");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            creatorText.setText("Created by: Unknown");
                        }
                    });
                    if (acurrentUserId.equals(creatorId)) {
                        idText.setVisibility(View.VISIBLE);
                        copy.setVisibility(View.VISIBLE);
                        idText.setText(groupId);
                    } else {
                        idText.setVisibility(View.VISIBLE);
                        copy.setVisibility(View.GONE);
                        idText.setText("Contact Admin to know the Group ID");
                    }
                } else {
                    idText.setVisibility(View.GONE);
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                idText.setVisibility(View.GONE);
            }
        });

        copy.setOnClickListener(v -> {
            String groupId = idText.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", groupId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(GroupMembersActivity.this, "Text Copied", Toast.LENGTH_SHORT).show();
        });
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }


    private void leaveGroup() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave the group?")
                .setPositiveButton("Yes", (dialog, which) -> {

                    // 🔵 Get user info from SharedPreferences
                    String userId = prefs.getString("userId", "");
                    String username = prefs.getString("username", "");

                    if (userId.isEmpty()) {
                        userId = prefs.getString("sp_userId", "");
                    }
                    if (username.isEmpty()) {
                        Toast.makeText(GroupMembersActivity.this, "Member name is invalid", Toast.LENGTH_SHORT).show();
                        username = prefs.getString("sp_userName", "");
                    }
                    username = username.toUpperCase();

                    final String fuserId = userId;
                    final String fusername = username;

                    DatabaseReference groupRootRef = FirebaseDatabase.getInstance().getReference("Groups").child(groupId);

                    groupRootRef.child("createdBy").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String adminName = snapshot.getValue(String.class);

                            if (adminName != null && adminName.equals(fusername)) {
                                // 🔵 Current user is Admin
                                groupRootRef.child("Members").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            boolean foundNewAdmin = false;

                                            for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                                                String memberId = memberSnapshot.getKey();

                                                if (!memberId.equals(fuserId)) { // ✅ compare by userId, not username
                                                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                                                            .getReference("Users").child(memberId).child("username");

                                                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            if (snapshot.exists()) {
                                                                String newAdminUsername = snapshot.getValue(String.class);
                                                                if (newAdminUsername != null) {
                                                                    newAdminUsername = newAdminUsername.toUpperCase();

                                                                    // 🔵 1. Change 'createdBy' to new admin name
                                                                    groupRootRef.child("createdBy").setValue(newAdminUsername);

                                                                    // 🔵 2. Set role of new admin
                                                                    groupRootRef.child("Members").child(memberId).child("role").setValue("Admin");

                                                                    // 🔵 3. Remove old admin from Members
                                                                    groupRootRef.child("Members").child(fuserId).removeValue();

                                                                    // 🔵 4. Remove group from old admin's joinedGroups
                                                                    DatabaseReference userJoinedRef = FirebaseDatabase.getInstance()
                                                                            .getReference("Users").child(fuserId).child("joinedGroups").child(groupId);
                                                                    userJoinedRef.removeValue();

                                                                    Toast.makeText(GroupMembersActivity.this, "You left the group", Toast.LENGTH_SHORT).show();
                                                                    finish();
                                                                }
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {}
                                                    });

                                                    // ✅ Important: found new admin
                                                    foundNewAdmin = true;
                                                    break;
                                                }
                                            }

                                            if (!foundNewAdmin) {
                                                // ❗ No other members => Delete whole group
                                                groupRootRef.removeValue();
                                                Toast.makeText(GroupMembersActivity.this, "Group deleted as no members left.", Toast.LENGTH_SHORT).show();
                                                finish();
                                            }
                                        } else {
                                            // ❗ No members node itself
                                            groupRootRef.removeValue();
                                            Toast.makeText(GroupMembersActivity.this, "Group deleted beacuse no members found.", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                });
                            } else {
                                // 🔵 Not an Admin ➔ Just leave the group
                                removeUserFromGroup(fuserId, groupId);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });

                })
                .setNegativeButton("No", null)
                .show();

    }

    private void removeUserFromGroup(String userId, String groupId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(userId).child("joinedGroups").child(groupId);
        userRef.removeValue();

        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("Groups")
                .child(groupId).child("Members").child(userId);
        groupRef.removeValue();

        Toast.makeText(GroupMembersActivity.this, "Left the group", Toast.LENGTH_SHORT).show();
        finish(); // Close the page
    }


    private void deleteGroup() {
        String userId = prefs.getString("username", "");

        if (userId.isEmpty()) {
            userId = prefs.getString("sp_userName", "");
        }

        userId = userId.toUpperCase();

        // Get the creator (admin) of the group
        DatabaseReference groupRootRef = FirebaseDatabase.getInstance().getReference("Groups").child(groupId);
        String finalUserId = userId;

        groupRootRef.child("createdBy").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String creatorId = snapshot.getValue(String.class);

                if (creatorId != null && creatorId.equals(finalUserId)) {
                    // If the current user is the creator (admin), proceed with deletion
                    new AlertDialog.Builder(GroupMembersActivity.this)
                            .setTitle("Delete Group")
                            .setMessage("Are you sure you want to permanently delete this group?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                // Get all members of the group
                                DatabaseReference membersRef = groupRootRef.child("Members");

                                membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot membersSnapshot) {
                                        for (DataSnapshot memberSnapshot : membersSnapshot.getChildren()) {
                                            String memberId = memberSnapshot.getKey();
                                            if (memberId != null) {
                                                DatabaseReference userGroupsRef = FirebaseDatabase.getInstance()
                                                        .getReference("Users").child(memberId)
                                                        .child("joinedGroups").child(groupId);
                                                userGroupsRef.removeValue(); // Remove group from the user's joined groups
                                            }
                                        }

                                        // Finally, delete the group itself
                                        groupRootRef.removeValue().addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(GroupMembersActivity.this, "Group deleted successfully", Toast.LENGTH_SHORT).show();
                                                finish(); // Close the activity after deletion
                                            } else {
                                                Toast.makeText(GroupMembersActivity.this, "Failed to delete group", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(GroupMembersActivity.this, "Failed to fetch group members", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            })
                            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                            .show();
                } else {
                    Toast.makeText(GroupMembersActivity.this, "Only the group creator can delete this group", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GroupMembersActivity.this, "Failed to verify group creator", Toast.LENGTH_SHORT).show();
            }
        });
    }

}