package com.example.aks;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class email_log extends AppCompatActivity {

    private EditText editTextEmail, editTextUsername, editTextPhone, editTextPassword;
    private ImageView togglePassword;
    private Button nextButton, log;

    private boolean isPasswordVisible = false;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    private Handler handler = new Handler();
    private boolean isCheckingVerification = false;
    private int attemptCount = 0;
    private static final int MAX_ATTEMPTS = 12; // ~1 minute

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_log);

        editTextEmail = findViewById(R.id.emailText);
        editTextUsername = findViewById(R.id.usernameText);
        editTextPhone = findViewById(R.id.phoneText);
        editTextPassword = findViewById(R.id.passwordText);
        togglePassword = findViewById(R.id.togglePassword);
        nextButton = findViewById(R.id.btn);
        log = findViewById(R.id.logText);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        togglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_closed);
            } else {
                editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_open);
            }
            isPasswordVisible = !isPasswordVisible;
            editTextPassword.setSelection(editTextPassword.length());
        });

        nextButton.setOnClickListener(v -> {
            requestLocationPermissions();

            String email = editTextEmail.getText().toString().trim();
            String username = editTextUsername.getText().toString().trim().toUpperCase();
            String phone = editTextPhone.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (email.isEmpty() || username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(email_log.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!phone.matches("\\d{10}")) {
                Toast.makeText(email_log.this, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 8) {
                Toast.makeText(email_log.this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            checkEmailExists(email, username, phone, password);
        });

        log.setOnClickListener(v -> startActivity(new Intent(email_log.this, login.class)));
    }

    private void requestLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }, 1);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
        }
    }

    private void checkEmailExists(String email, String username, String phone, String password) {
        databaseReference.orderByChild("email").equalTo(email).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Toast.makeText(this, "Email already registered. Please log in.", Toast.LENGTH_SHORT).show();
            } else {
                registerUser(email, username, phone, password);
            }
        });
    }

    private void registerUser(String email, String username, String phone, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                if (verifyTask.isSuccessful()) {
                                    Toast.makeText(this, "Verification email sent. Check your inbox.", Toast.LENGTH_LONG).show();
                                    startVerificationCheck(user, email, username, phone, password);
                                } else {
                                    Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startVerificationCheck(FirebaseUser user, String email, String username, String phone, String password) {
        isCheckingVerification = true;
        attemptCount = 0;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isCheckingVerification || attemptCount >= MAX_ATTEMPTS) {
                    Toast.makeText(email_log.this, "Verification timed out. Try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                attemptCount++;
                user.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && user.isEmailVerified()) {
                        isCheckingVerification = false;
                        saveUserDataToDatabase(user.getUid(), email, username, phone, password);
                    } else {
                        handler.postDelayed(this, 5000);
                    }
                });
            }
        }, 2000);
    }

    private void saveUserDataToDatabase(String userId, String email, String username, String phone, String password) {
        User user = new User(email, username, phone, password);
        databaseReference.child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    databaseReference.child(userId).child("signup").setValue(true);

                    saveUserDataToLocal(userId, email, username, phone, password);
                    Intent intent = new Intent(this, group.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveUserDataToLocal(String userId, String email, String username, String phone, String password) {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        username = username.toUpperCase();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isUserRegistered", true);
        editor.putString("userId", userId);
        editor.putString("userEmail", email);
        editor.putString("username", username);
        editor.putString("phone", phone);
        editor.putString("password", password);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isCheckingVerification = false;
    }

    public static class User {
        public String email, username, phone, password;

        public User() {}

        public User(String email, String username, String phone, String password) {
            this.email = email;
            this.username = username;
            this.phone = phone;
            this.password = password;
        }
    }
}
