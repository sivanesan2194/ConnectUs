package com.example.aks;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 100;

    private static final String[] PERMISSIONS_LOCATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ? new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
    }
            : new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        boolean fineLocation = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocation = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean foregroundLocation = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            foregroundLocation = checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        if (fineLocation && coarseLocation && foregroundLocation) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            navigateToNextPage();
        } else {
            requestPermissionsWithDialog();
        }
    }

    private void requestPermissionsWithDialog() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setMessage("This app requires location permissions to function properly.")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, which) -> {
                        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_LOCATION, PERMISSION_REQ_CODE);
                        dialog.dismiss();
                    })
                    .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION, PERMISSION_REQ_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQ_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_LONG).show();
                navigateToNextPage();
            } else {
                showSettingsDialog();
            }
        }
    }

    private void navigateToNextPage() {
        Intent intent = new Intent(MainActivity.this, get_start.class);
        startActivity(intent);
        finish();
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Location permission is required for this feature. Please enable it in settings.")
                .setTitle("Permission Required")
                .setCancelable(false)
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                    dialog.dismiss();
                })
                .show();
    }
}
