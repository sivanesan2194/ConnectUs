package com.example.aks;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class login extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton, signUpButton;
    private ImageView togglePassword;
    private TextView forgotPasswordText;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private SharedPreferences preferences;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        emailInput = findViewById(R.id.logemail);
        passwordInput = findViewById(R.id.logpassword);
        togglePassword = findViewById(R.id.togglePassword);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        loginButton = findViewById(R.id.log_btn);
        signUpButton = findViewById(R.id.sigText);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        if (preferences.getBoolean("sp_isLoggedIn", false)) {
            navigateToGroups();
            return;
        }

        togglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_closed);
            } else {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_open);
            }
            isPasswordVisible = !isPasswordVisible;
            passwordInput.setSelection(passwordInput.getText().length());
        });

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim().toLowerCase();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(login.this, "Please enter email", Toast.LENGTH_SHORT).show();
            } else if (password.isEmpty()) {
                Toast.makeText(login.this, "Please enter password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        signUpButton.setOnClickListener(v -> {
            startActivity(new Intent(login.this, email_log.class));
        });

        forgotPasswordText.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                emailInput.setError("Enter your email to reset password");
                return;
            }

            firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(login.this, "Reset email sent!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(login.this, "Error sending reset email.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loginUser(String email, String enteredPassword) {
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        fetchUserDetails(userId, email, enteredPassword);
                        return;
                    }
                } else {
                    Toast.makeText(login.this, "User not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(login.this, "Error fetching user ID!", Toast.LENGTH_SHORT).show();
                Log.e("FirebaseError", "Error: " + error.getMessage());
            }
        });
    }

    private void fetchUserDetails(String userId, String email, String enteredPassword) {
        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(login.this, "User details not found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String userName = snapshot.child("username").getValue(String.class);
                String userPhone = snapshot.child("phone").getValue(String.class);
                String storedPassword = snapshot.child("password").getValue(String.class);
                Boolean isSignedUp = snapshot.child("signup").getValue(Boolean.class);

                if (userName == null || userPhone == null || storedPassword == null) {
                    Toast.makeText(login.this, "User data is incomplete!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!storedPassword.equals(enteredPassword)) {
                    Toast.makeText(login.this, "Incorrect password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isSignedUp != null && isSignedUp) {
                    Toast.makeText(login.this, "User is already logged in on another device. Please try with another ID.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Mark signup as true (logged in)
                databaseReference.child(userId).child("signup").setValue(true);

                userName = userName.toUpperCase();

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("sp_isLoggedIn", true);
                editor.putBoolean("isUserRegistered", true);
                editor.putString("sp_userEmail", email);
                editor.putString("sp_userId", userId);
                editor.putString("sp_userName", userName);
                editor.putString("sp_userPhoneNumber", userPhone);
                editor.apply();

                Toast.makeText(login.this, "Welcome " + userName + "!", Toast.LENGTH_SHORT).show();
                navigateToGroups();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(login.this, "Error retrieving user details. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToGroups() {
        startActivity(new Intent(login.this, MyGroupsActivity.class));
        finish();
    }
}
