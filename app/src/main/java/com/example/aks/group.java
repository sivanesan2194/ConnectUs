package com.example.aks;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.security.SecureRandom;

public class group extends AppCompatActivity {
    private static final String BASE_URL = "https://ConnectUs.com/";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static String randomLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Check if user has already clicked "View Map"
        SharedPreferences preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        boolean user = preferences.getBoolean("SkipGroupPage", false);

        if (user) {
            startActivity(new Intent(group.this, MyGroupsActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_group);

        Button button =findViewById(R.id.createGroupButton);
        Button join = findViewById(R.id.joinGroup);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity( new Intent(group.this, createid.class)); }
        });

        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(group.this,join.class));
            }
        });
    }

}