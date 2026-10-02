package com.example.aks;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class settings extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserId, tvChangePassword, tvDeleteAccount, tvTerms, tvPrivacy, tvAppVersion, tvProfileInitial;
    private SwitchMaterial switchPushNotifications, switchDisableLocation;
    private RadioGroup radioPhoneVisibility;
    private RadioButton radioPublic, radioPrivate;
    private Button btnLogout;
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private SharedPreferences preferences;
    private String userId, username, userEmail;
    EditText feedbackEditText;
    Button submitButton;
    DatabaseReference feedbackRef;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        feedbackEditText = findViewById(R.id.et_feedback);
        submitButton = findViewById(R.id.btn_submit_feedback);

        mAuth = FirebaseAuth.getInstance();

        if (currentUser == null) {
            userId = currentUser.getUid();
        } else {
            userId = preferences.getString("userId", "");
            username = preferences.getString("username", "");
            userEmail = preferences.getString("userEmail", "");

            // In case userId is still empty, check secondary keys
            if (userId.isEmpty()) {
                userId = preferences.getString("sp_userId", "");
                username = preferences.getString("sp_userName", "");
                userEmail = preferences.getString("sp_userEmail", "");
            }
        }

        feedbackRef = FirebaseDatabase.getInstance().getReference("Feedback").child(userId);
        initViews();
        loadUserData();
        setupListeners();

        submitButton.setOnClickListener(v -> {
            String feedback = feedbackEditText.getText().toString().trim();

            if (feedback.isEmpty()) {
                feedbackEditText.setError("Please enter your feedback");
                feedbackEditText.requestFocus();
                return;
            }

            // Store feedback
            Map<String, Object> feedbackData = new HashMap<>();
            feedbackData.put("username",username);
            feedbackData.put("feedback", feedback);

            feedbackRef.setValue(feedbackData)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(settings.this, "Feedback submitted", Toast.LENGTH_SHORT).show();
                        feedbackEditText.setText("");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(settings.this, "Failed to submit feedback", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvUserId = findViewById(R.id.tv_user_id);
        tvChangePassword = findViewById(R.id.tv_change_password);
        tvDeleteAccount = findViewById(R.id.tv_delete_account);
        tvTerms = findViewById(R.id.tv_terms);
        tvPrivacy = findViewById(R.id.tv_privacy);
        tvAppVersion = findViewById(R.id.tv_app_version);
        tvProfileInitial = findViewById(R.id.tv_user_initials);

        switchPushNotifications = findViewById(R.id.switch_push_notifications);
        switchDisableLocation = findViewById(R.id.switch_disable_location);

        radioPhoneVisibility = findViewById(R.id.radio_phone_visibility);
        radioPublic = findViewById(R.id.radio_public);
        radioPrivate = findViewById(R.id.radio_private);

        btnLogout = findViewById(R.id.btn_logout);
    }

    private void loadUserData() {
        if (!userId.isEmpty()) {
            tvUserEmail.setText("Email: " + userEmail);
            tvUserId.setText("User ID: " + userId);
            tvUserName.setText(username.isEmpty() ? "No name" : username);

            String initial = (username.isEmpty()) ? "U" : username.substring(0, 1).toUpperCase();
            tvProfileInitial.setText(initial);

            int[] colors = {
                    Color.parseColor("#F44336"), Color.parseColor("#E91E63"),
                    Color.parseColor("#9C27B0"), Color.parseColor("#3F51B5"),
                    Color.parseColor("#03A9F4"), Color.parseColor("#009688"),
                    Color.parseColor("#4CAF50"), Color.parseColor("#FF9800"),
                    Color.parseColor("#795548")
            };
            int randomColor = colors[new Random().nextInt(colors.length)];
            GradientDrawable bgShape = (GradientDrawable) tvProfileInitial.getBackground();
            bgShape.setColor(randomColor);
        }

        switchPushNotifications.setChecked(preferences.getBoolean("push_notifications", true));
        switchDisableLocation.setChecked(preferences.getBoolean("disable_location", false));

        String phoneVisibility = preferences.getString("phone_visibility", "public");
        if (phoneVisibility.equals("public")) {
            radioPublic.setChecked(true);
        } else {
            radioPrivate.setChecked(true);
        }
    }

    private void setupListeners() {
        switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("push_notifications", isChecked).apply();
            Toast.makeText(this, isChecked ? "Push notifications enabled" : "Push notifications disabled", Toast.LENGTH_SHORT).show();
        });

        switchDisableLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("disable_location", isChecked).apply();
            Intent locationIntent = new Intent(this, LocationService.class);

            if (isChecked) {
                stopService(locationIntent);
                showLocationDisabledNotification();
                Toast.makeText(this, "Location tracking disabled", Toast.LENGTH_SHORT).show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(locationIntent);
                } else {
                    startService(locationIntent);
                }
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.cancel(101);
                Toast.makeText(this, "Location tracking enabled", Toast.LENGTH_SHORT).show();
            }
        });


        radioPhoneVisibility.setOnCheckedChangeListener((group, checkedId) -> {
            String visibility = (checkedId == R.id.radio_public) ? "public" : "private";
            preferences.edit().putString("phone_visibility", visibility).apply();
        });

        tvChangePassword.setOnClickListener(v -> {
            if (currentUser != null) {
                String email = currentUser.getEmail();
                if (email != null) {
                    mAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused -> Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(this, "No email associated with this account", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            }
        });


        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Log Out ?")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Log out", (dialog, which) -> {
                        // Step 1: Update signup status to false in Firebase Database
                        DatabaseReference userRef = FirebaseDatabase.getInstance()
                                .getReference("Users")
                                .child(userId); // Make sure userId is already loaded

                        userRef.child("signup").setValue(false)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Step 2: Clear SharedPreferences
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.clear();
                                        editor.apply();

                                        // Step 3: Go back to login page
                                        startActivity(new Intent(settings.this, login.class));
                                        finish();
                                    } else {
                                        Toast.makeText(settings.this, "Failed to Log out ", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });


        tvTerms.setOnClickListener(v -> startActivity(new Intent(settings.this,Terms.class)));
        tvPrivacy.setOnClickListener(v -> startActivity(new Intent(settings.this,privacy.class)));

        tvDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to permanently delete your account?")
                    .setPositiveButton("Delete", (dialog, which) -> {

                        // Step 1: Remove user from 'Users' node
                        DatabaseReference userRef = FirebaseDatabase.getInstance()
                                .getReference("Users").child(userId);

                        // Step 2: Remove user from all groups
                        DatabaseReference joinedGroupsRef = FirebaseDatabase.getInstance()
                                .getReference("Users").child(userId).child("joinedGroups");

                        joinedGroupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                                    String groupId = groupSnapshot.getKey();
                                    DatabaseReference groupMembersRef = FirebaseDatabase.getInstance()
                                            .getReference("Groups").child(groupId)
                                            .child("Members").child(userId);
                                    groupMembersRef.removeValue();
                                }

                                // After removing from groups, delete user data
                                userRef.removeValue();

                                // Step 3: Delete Firebase Authentication account
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null) {
                                    currentUser.delete()
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    // Step 4: Clear SharedPreferences
                                                    SharedPreferences.Editor editor = preferences.edit();
                                                    editor.clear();
                                                    editor.apply();

                                                    // Step 5: Move to Login screen
                                                    startActivity(new Intent(settings.this, login.class));
                                                    finish();
                                                    Toast.makeText(settings.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(settings.this, "Failed to delete account. Please log in again and try.", Toast.LENGTH_LONG).show();
                                                }
                                            });
                                } else {
                                    Toast.makeText(settings.this, "User not logged in", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(settings.this, "Failed to delete account data", Toast.LENGTH_SHORT).show();
                            }
                        });

                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });


    }

    private void showLocationDisabledNotification() {
        boolean isPushEnabled = preferences.getBoolean("push_notifications", true);

        if (isPushEnabled) {
            String channelId = "location_channel";
            String channelName = "Location Notifications";

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.logo_o)
                    .setContentTitle("Location Disabled")
                    .setContentText("You have disabled location tracking.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            notificationManager.notify(101, builder.build());
        } else {
            Toast.makeText(this, "Push notifications are disabled, cannot send location updates", Toast.LENGTH_SHORT).show();
        }
    }
}
