    package com.example.aks;


    import android.content.ClipData;
    import android.content.ClipboardManager;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.net.ConnectivityManager;
    import android.net.NetworkInfo;
    import android.os.Bundle;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.ImageView;
    import android.widget.Toast;
    import androidx.appcompat.app.AppCompatActivity;
    import com.google.android.gms.location.FusedLocationProviderClient;
    import com.google.android.gms.location.LocationServices;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import java.util.HashMap;
    import java.util.UUID;

    public class createid extends AppCompatActivity {

        private EditText text, groupNameEditText;
        private ImageView image, share;
        private SharedPreferences preferences;
        private DatabaseReference databaseReference, userDatabase;
        private String username, userId, userPhone, userEmail, groupId;
        private FusedLocationProviderClient fusedLocationClient;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_createid);

            preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            loadUserDataFromSharedPreferences();

            databaseReference = FirebaseDatabase.getInstance().getReference("Groups");
            userDatabase = FirebaseDatabase.getInstance().getReference("Users");

            text = findViewById(R.id.grouplink);
            groupNameEditText = findViewById(R.id.groupNameEditText);
            image = findViewById(R.id.copyIcon);
            share = findViewById(R.id.shareIcon);
            ImageView imageView = findViewById(R.id.closeIcon);

            groupId = generateGroupLink();
            text.setText(groupId);

            imageView.setOnClickListener(v -> startActivity(new Intent(createid.this, group.class)));

            image.setOnClickListener(v -> {
                String textToCopy = text.getText().toString();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", textToCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(createid.this, "Text Copied", Toast.LENGTH_SHORT).show();
            });

            share.setOnClickListener(v -> {
                String content = text.getText().toString();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared Content");
                shareIntent.putExtra(Intent.EXTRA_TEXT, content);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            });

            Button createGroupButton = findViewById(R.id.createGroupButton);
            createGroupButton.setOnClickListener(v -> saveGroupToFirebase());
        }

        private void loadUserDataFromSharedPreferences() {
            userId = preferences.getString("userId", "");
            username = preferences.getString("username", "");
            userPhone = preferences.getString("phone", "");
            userEmail = preferences.getString("userEmail", "");

            if (userId.isEmpty()) {
                userId = preferences.getString("sp_userId", "");
                username = preferences.getString("sp_username", "");
                userPhone = preferences.getString("sp_userPhoneNumber", "");
                userEmail = preferences.getString("sp_userEmail", "");
            }

        }

        private String generateGroupLink() {
            return UUID.randomUUID().toString().substring(0, 16);
        }

        private void saveGroupToFirebase() {
            String groupName = groupNameEditText.getText().toString().trim();

            if (groupName.isEmpty()) {
                Toast.makeText(this, "Please enter a Group Name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isNetworkAvailable()) {
                Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔐 Group count limit: check for max 10 groups
            int groupCount = 0;
            for (int i = 1; i <= 10; i++) {
                if (preferences.contains("groupId" + i)) {
                    groupCount++;
                }
            }

            if (groupCount >= 10) {
                Toast.makeText(this, "You can only join or create up to 10 groups.", Toast.LENGTH_LONG).show();
                return;
            }

            if (username.isEmpty()) {
                Toast.makeText(this, "Fetching user details, please wait...", Toast.LENGTH_SHORT).show();
                fetchUserDetailsFromFirebase(this::saveGroupToFirebase);
                return;
            }

            HashMap<String, Object> groupData = new HashMap<>();
            groupData.put("groupName", groupName);
            groupData.put("groupId", groupId);
            groupData.put("createdBy", username);

            databaseReference.child(groupId).setValue(groupData).addOnSuccessListener(aVoid -> {
                DatabaseReference membersRef = databaseReference.child(groupId).child("Members").child(userId);
                HashMap<String, String> memberData = new HashMap<>();
                memberData.put("username", username);
                memberData.put("role", "Admin");

                membersRef.setValue(memberData).addOnSuccessListener(aVoid1 -> {
                    updateUserGroup(groupId, groupName, () ->{
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("SkipGroupPage", true);
                        editor.apply();

                        Toast.makeText(createid.this, "Group Created Successfully!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(createid.this, MyGroupsActivity.class);
                        intent.putExtra("groupId", groupId);
                        intent.putExtra("groupName", groupName);
                        startActivity(intent);
                        finish();
                    });
                });
            });
        }

        private void fetchUserDetailsFromFirebase(Runnable onComplete) {
            userDatabase.child(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    username = task.getResult().child("username").getValue(String.class);
                    userPhone = task.getResult().child("phone").getValue(String.class);
                    userEmail = task.getResult().child("email").getValue(String.class);

                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("loguserId", userId);
                    editor.putString("logusername", username);
                    editor.putString("loguserPhoneNumber", userPhone);
                    editor.putString("loguserEmail", userEmail);
                    editor.apply();

                    if (!username.isEmpty()) {
                        Toast.makeText(this, "Welcome " + username, Toast.LENGTH_SHORT).show();
                    }

                    if (onComplete != null) {
                        onComplete.run();
                    }
                } else {
                    Toast.makeText(this, "Failed to fetch user details.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void updateUserGroup(String groupId, String groupName, Runnable onSuccess) {
            DatabaseReference userGroupsRef = userDatabase.child(userId).child("joinedGroups").child(groupId);
            HashMap<String, Object> userData = new HashMap<>();
            userData.put("groupId", groupId);
            userData.put("groupName", groupName);

            userGroupsRef.setValue(userData).addOnSuccessListener(aVoid -> {
                onSuccess.run();
            }).addOnFailureListener(e -> {
                Toast.makeText(createid.this, "Failed to update user groups", Toast.LENGTH_SHORT).show();
            });
        }

        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }
