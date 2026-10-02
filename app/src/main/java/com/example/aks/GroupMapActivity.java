package com.example.aks;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.io.IOException;
import java.util.*;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String groupId;
    private String groupName;
    private DatabaseReference membersRef;
    private final HashMap<String, Marker> memberMarkers = new HashMap<>();
    private final HashMap<Marker, String> markerUserMap = new HashMap<>();
    private boolean hasMovedCamera = false;
    private LinearLayout avatarContainer;

    private boolean isSatelliteView = false; // To track current map view type

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_map);

        TextView groupNameTextView = findViewById(R.id.groupNameTextView);

        // Get group name from intent
        String groupName = getIntent().getStringExtra("groupname");

        if (groupName != null && !groupName.isEmpty()) {
            groupNameTextView.setText(groupName);
        } else {
            groupNameTextView.setText("Group Map"); // Default
        }

        groupId = getIntent().getStringExtra("groupId");

        avatarContainer = findViewById(R.id.avatar_container);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null)
            mapFragment.getMapAsync(this);

        // Button for changing map view to Satellite
        ImageView mapTypeButton = findViewById(R.id.mapTypeButton);
        mapTypeButton.setOnClickListener(v -> toggleMapView());

        ImageView focusMeButton = findViewById(R.id.focusme);
        focusMeButton.setOnClickListener(v -> moveToCurrentUser());

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        membersRef = FirebaseDatabase.getInstance()
                .getReference("Groups")
                .child(groupId)
                .child("Members");

        startListeningToMemberLocations();

        mMap.setOnMarkerClickListener(marker -> {
            String userId = markerUserMap.get(marker);
            if (userId != null) {
                showBottomSheet(marker, userId);
            }
            return false;
        });
    }

    private void startListeningToMemberLocations() {
        membersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                avatarContainer.removeAllViews();

                for (DataSnapshot memberSnap : snapshot.getChildren()) {
                    String userId = memberSnap.getKey();
                    String username = memberSnap.child("username").getValue(String.class);
                    Double lat = memberSnap.child("latitude").getValue(Double.class);
                    Double lng = memberSnap.child("longitude").getValue(Double.class);

                    if (lat != null && lng != null) {
                        LatLng memberLatLng = new LatLng(lat, lng);

                        String initials = getInitials(username);
                        String initialsa = getInitialsa(username);

                        int markerColor = generateColorFromUserId(userId); // Consistent color
                        BitmapDescriptor markerIcon = createInitialsMarker(initialsa, markerColor);

                        if (memberMarkers.containsKey(userId)) {
                            memberMarkers.get(userId).setPosition(memberLatLng);
                        } else {
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(memberLatLng)
                                    .title(username)
                                    .icon(markerIcon));
                            memberMarkers.put(userId, marker);
                            markerUserMap.put(marker, userId);
                        }
                        // Move camera only once to current user's location
                        if (userId.equals(currentUserId) && !hasMovedCamera) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(memberLatLng, 16f));
                            hasMovedCamera = true;
                        }

                        // Add to avatar bar
                        View avatarView = LayoutInflater.from(GroupMapActivity.this)
                                .inflate(R.layout.item_avatar, avatarContainer, false);

                        ImageView avatarImage = avatarView.findViewById(R.id.avatarimage);
                        TextView avatarName = avatarView.findViewById(R.id.avatarname);

                        Bitmap bitmap = createInitialsBitmap(initialsa, markerColor);
                        avatarImage.setImageBitmap(bitmap);
                        avatarName.setText(initials); // or username

                        avatarView.setOnClickListener(v -> {
                            Marker marker = memberMarkers.get(userId);
                            if (marker != null) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 16f));
                                showBottomSheet(marker, userId);
                            }
                        });

                        avatarContainer.addView(avatarView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showBottomSheet(Marker marker, String userId) {
        DatabaseReference userRef = membersRef.child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = snapshot.child("username").getValue(String.class);
                Double lat = snapshot.child("latitude").getValue(Double.class);
                Double lng = snapshot.child("longitude").getValue(Double.class);

                String locationName = "Unknown Area";
                if (lat != null && lng != null) {
                    try {
                        Geocoder geocoder = new Geocoder(GroupMapActivity.this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                        if (!addresses.isEmpty()) {
                            locationName = addresses.get(0).getSubLocality();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // Inflate the bottom sheet view
                View view = LayoutInflater.from(GroupMapActivity.this)
                        .inflate(R.layout.bottom_sheet_user_info, null);

                // Get references to the views
                TextView usernameText = view.findViewById(R.id.textViewUsername);
                TextView locationText = view.findViewById(R.id.textViewLocation);
                CircleImageView initialsImage = view.findViewById(R.id.imageViewProfile);

                // Set username and location text
                usernameText.setText("Name: " + username);
                locationText.setText("Location: " + locationName);

                // Generate initials and color
                String initials = getInitials(username);
                int color = generateColorFromUserId(userId);

                // Create the bitmap with initials
                Bitmap initialsBitmap = topRight(color);

                // Set the generated bitmap to the profile image
                initialsImage.setImageBitmap(initialsBitmap);

                // Show the bottom sheet dialog
                BottomSheetDialog dialog = new BottomSheetDialog(GroupMapActivity.this);
                dialog.setContentView(view);
                dialog.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private Bitmap topRight(int color) {
        int size = 100;
        int ringWidth = 6;

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Draw fixed white outer ring (always white)
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // Draw inner circle with user color
        paint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - ringWidth, paint);

        // Draw initials
        paint.setColor(Color.WHITE);
        paint.setTextSize(40f);
        paint.setTextAlign(Paint.Align.CENTER);
        float yPos = (canvas.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);

        return bitmap;
    }

    private Bitmap createInitialsBitmap(String initials, int color) {
        int size = 100;
        int ringWidth = 6;

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Draw fixed white outer ring (always white)
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // Draw inner circle with user color
        paint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - ringWidth, paint);

        // Draw initials
        paint.setColor(Color.WHITE);
        paint.setTextSize(40f);
        paint.setTextAlign(Paint.Align.CENTER);
        float yPos = (canvas.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(initials, canvas.getWidth() / 2f, yPos, paint);

        return bitmap;
    }

    private String getInitials(String username) {
        if (username == null || username.isEmpty()) return "U";
        String[] words = username.trim().split(" ");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < words.length && i < 2; i++) {
            if (!words[i].isEmpty()) {
                initials.append(Character.toUpperCase(words[i].charAt(0)));
            }
        }
        return username;
    }
    private String getInitialsa(String username) {
        if (username == null || username.isEmpty()) return "U";
        String[] words = username.trim().split(" ");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < words.length && i < 2; i++) {
            if (!words[i].isEmpty()) {
                initials.append(Character.toUpperCase(words[i].charAt(0)));
            }
        }
        return initials.toString();
    }

    private int generateColorFromUserId(String userId) {
        int[] colors = {
                Color.parseColor("#FF6F61"),
                Color.parseColor("#6A5ACD"),
                Color.parseColor("#00BFA6"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#009688"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#FF5722"),
                Color.parseColor("#2196F3")
        };
        int hash = Math.abs(userId.hashCode());
        return colors[hash % colors.length];
    }

    private BitmapDescriptor createInitialsMarker(String initials, int color) {
        return BitmapDescriptorFactory.fromBitmap(createInitialsBitmap(initials, color));
    }
    private void moveToCurrentUser() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Marker marker = memberMarkers.get(currentUserId);
        if (marker != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 19f));
        }
    }
    private void toggleMapView() {
        if (isSatelliteView) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);  // Switch to normal view
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);  // Switch to satellite view
        }
        isSatelliteView = !isSatelliteView;
    }
}
