package com.example.aks;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Terms extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_terms);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView mapLocation = findViewById(R.id.mapLocation);
        mapLocation.setOnClickListener(v -> {
            // Geo URI with Kumbakonam coordinates
            Uri gmmIntentUri = Uri.parse("geo:10.9675,79.3548?q=Kumbakonam,+Tamil+Nadu");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps"); // Ensure Google Maps opens

            // Check if Google Maps is installed
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Toast to notify if Google Maps is not installed
                Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
            }
        });


        Button contactButton = findViewById(R.id.contactButton);
        contactButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:akashviolinist95008@gmail.com")); // Correct URI for email
            intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here"); // Optional: Set the subject
            intent.putExtra(Intent.EXTRA_TEXT, "Email body here"); // Optional: Set the body text
            startActivity(Intent.createChooser(intent, "Send Email"));
        });


    }
}