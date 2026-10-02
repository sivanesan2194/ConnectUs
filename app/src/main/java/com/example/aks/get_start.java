package com.example.aks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;

public class get_start extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_get_start);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        boolean isUserRegistered = sharedPreferences.getBoolean("isUserRegistered", false);
        boolean skipGroup = sharedPreferences.getBoolean("SkipGroupPage", false);

        if (isUserRegistered) {
            startActivity(new Intent(this, group.class));
            finish();
            return;
        }

        if (skipGroup) {
            startActivity(new Intent(this, MyGroupsActivity.class));
            finish();
            return;
        }

        Button button = findViewById(R.id.btn);

        button.setOnClickListener(v->{
                startActivity(new Intent(this, email_log.class));
        });

        VideoView videoView=findViewById(R.id.videoView);

        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.get_video);

        videoView.setVideoURI(uri);

        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            mp.setVolume(0f,0f);
        });
        videoView.start();

    }
}
