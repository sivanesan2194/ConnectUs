    package com.example.aks;

    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.net.ConnectivityManager;
    import android.net.NetworkInfo;
    import android.os.Bundle;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AppCompatActivity;

    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.ValueEventListener;
    import com.google.firebase.database.DatabaseError;

    import java.util.HashMap;

    public class join extends AppCompatActivity {

        private EditText groupCodeEditText;
        private Button joinButton;
        private DatabaseReference databaseReference, userDatabase;
        private SharedPreferences preferences;
        private String userId, username, userPhone, userEmail;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_join);

            groupCodeEditText = findViewById(R.id.getGroupLink);
            joinButton = findViewById(R.id.btnJoinGroup);

            preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            databaseReference = FirebaseDatabase.getInstance().getReference("Groups");
            userDatabase = FirebaseDatabase.getInstance().getReference("Users");

            loadUserDataFromSharedPreferences();

            joinButton.setOnClickListener(v -> joinGroupIfValid());
        }

        private void loadUserDataFromSharedPreferences() {
            userId = preferences.getString("userId", "");
            username = preferences.getString("username", "");
            userPhone = preferences.getString("phone", "");
            userEmail = preferences.getString("userEmail", "");

            if (userId.isEmpty()) {
                userId = preferences.getString("sp_userId", "");
                username = preferences.getString("sp_userName", "");
                userPhone = preferences.getString("sp_userPhoneNumber", "");
                userEmail = preferences.getString("sp_userEmail", "");
            }
        }

        private void joinGroupIfValid() {
            String enteredCode = groupCodeEditText.getText().toString().trim();

            if (enteredCode.isEmpty()) {
                Toast.makeText(this, "Enter a valid Group Code", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                return;
            }


            databaseReference.child(enteredCode).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String groupName = snapshot.child("groupName").getValue(String.class);

                        // Add user to group
                        DatabaseReference memberRef = databaseReference.child(enteredCode).child("Members").child(userId);
                        HashMap<String, String> memberData = new HashMap<>();
                        memberData.put("username", username);
                        memberData.put("role", "Member");

                        memberRef.setValue(memberData).addOnSuccessListener(unused -> {
                            // Add group info under user node
                            DatabaseReference userGroupRef = userDatabase.child(userId).child("joinedGroups").child(enteredCode);
                            HashMap<String, Object> userData = new HashMap<>();
                            userData.put("groupId", enteredCode);
                            userData.put("groupName", groupName);

                            userGroupRef.setValue(userData).addOnSuccessListener(aVoid -> {

                                Toast.makeText(join.this, "Joined group successfully!", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(join.this, MyGroupsActivity.class);
                                intent.putExtra("groupId", enteredCode);
                                intent.putExtra("groupName", groupName);
                                startActivity(intent);
                                finish();

                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putBoolean("SkipGroupPage", true);
                                editor.apply();

                            }).addOnFailureListener(e -> Toast.makeText(join.this, "Failed to add group under user", Toast.LENGTH_SHORT).show());

                        }).addOnFailureListener(e -> Toast.makeText(join.this, "Failed to join group", Toast.LENGTH_SHORT).show());

                    } else {
                        Toast.makeText(join.this, "Group not found", Toast.LENGTH_SHORT).show();
                    }


                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(join.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }
