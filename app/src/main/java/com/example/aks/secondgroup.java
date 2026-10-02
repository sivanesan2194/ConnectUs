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

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.security.SecureRandom;

public class secondgroup extends AppCompatActivity {

    String userId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_secondgroup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Check if user has already clicked "View Map"
        SharedPreferences preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        Button button =findViewById(R.id.createsGroupButton);
        Button join = findViewById(R.id.joinsGroup);

        preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        userId = preferences.getString("userId", "");

        if(userId == null){
            userId = preferences.getString("sp_userId", "");
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity( new Intent(secondgroup.this, createid.class)); }
        });

        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(secondgroup.this,join.class));
            }
        });
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

// Set the current selected item to groups
        bottomNavigationView.setSelectedItemId(R.id.nav_map);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int selectedItemId = item.getItemId();
            Intent intent = null;

            if (selectedItemId == R.id.nav_groups) {
                intent = new Intent(secondgroup.this, MyGroupsActivity.class);
            } else if (selectedItemId == R.id.nav_friends) {
                intent = new Intent(secondgroup.this, SearchFriendsActivity.class);
            } else if (selectedItemId == R.id.nav_map) {
                intent = new Intent(secondgroup.this, secondgroup.class);
            }

            if (intent != null) {
                intent.putExtra("userId",userId);
                startActivity(intent);
                return true;
            }

            return false;
        });

    }

}