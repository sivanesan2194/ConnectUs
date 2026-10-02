package com.example.aks;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class LocationService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String selectedGroupId;
    SharedPreferences preferences;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        selectedGroupId = intent.getStringExtra("groupId");
        if (selectedGroupId == null) stopSelf();

        startForegroundServiceNotification();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        startLocationUpdates();

        return START_STICKY;
    }

    private void startForegroundServiceNotification() {
        String channelId = "LocationServiceChannel";
        NotificationManager manager = getSystemService(NotificationManager.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Location Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            manager.createNotificationChannel(channel);
        }

        preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isPushNotificationEnabled = preferences.getBoolean("push_notifications", true);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.logo_o)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        if (isPushNotificationEnabled) {
            builder.setContentTitle("Group Location Sharing")
                    .setContentText("Sharing location with group: " + selectedGroupId);
        } else {
            builder.setContentTitle("Location Running")
                    .setContentText("Location sharing is active");
        }

        Notification notification = builder.build();
        startForeground(1, notification); // Always required
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(2000);
        request.setFastestInterval(1000);
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location location = result.getLastLocation();
                if (location != null) updateLocationToFirebase(location);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void updateLocationToFirebase(Location location) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isLocationDisabled = prefs.getBoolean("disable_location", false);

        if (isLocationDisabled) {
            stopSelf(); // Stop service if user disabled location
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Groups")
                .child(selectedGroupId)
                .child("Members")
                .child(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());
        data.put("timestamp", ServerValue.TIMESTAMP);

        ref.updateChildren(data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        stopForeground(true);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
