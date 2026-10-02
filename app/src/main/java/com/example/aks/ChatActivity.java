package com.example.aks;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class ChatActivity extends AppCompatActivity {

    private String groupId, currentUserId, currentUsername;
    private EditText editTextMessage;
    private ImageButton buttonSendMessage;
    private RecyclerView recyclerViewChat;
    private ChatAdapter chatAdapter;
    private ArrayList<Message> messages = new ArrayList<>();
    private DatabaseReference groupChatRef, userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        groupId = getIntent().getStringExtra("groupId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSendMessage = findViewById(R.id.buttonSendMessage);
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(messages, currentUserId);
        recyclerViewChat.setAdapter(chatAdapter);

        TextView textViewGroupName = findViewById(R.id.textViewGroupName);

        // Get group name passed via intent
        String groupId = getIntent().getStringExtra("groupId");

        if (groupId != null) {
            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("groupName")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String groupName = snapshot.getValue(String.class);
                            if (groupName != null) {
                                textViewGroupName.setText(groupName);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            textViewGroupName.setText("Group Chat");
                        }
                    });
        } else {
            textViewGroupName.setText("Group Chat");
        }


        groupChatRef = FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Messages");
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);

        // Get current username
        userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUsername = snapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        fetchMessages();

        buttonSendMessage.setOnClickListener(v -> {
            String msg = editTextMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(msg);
                editTextMessage.setText("");
            } else {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMessages() {
        groupChatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messages.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Message msg = snap.getValue(Message.class);
                    messages.add(msg);
                }
                chatAdapter.notifyDataSetChanged();
                recyclerViewChat.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage(String text) {
        String msgId = groupChatRef.push().getKey();
        if (msgId != null) {
            Message message = new Message(currentUserId, currentUsername, text, System.currentTimeMillis());
            groupChatRef.child(msgId).setValue(message);

            sendNotificationToGroup("New message", currentUsername + ": " + text);
        }
    }
    private void sendNotificationToGroup(String title, String body) {
        String serverKey = "YOUR_FIREBASE_SERVER_KEY_HERE";
        String topic = "/topics/" + groupId;

        JSONObject notification = new JSONObject();
        JSONObject notifBody = new JSONObject();
        try {
            notifBody.put("title", title);
            notifBody.put("body", body);
            notification.put("to", topic);
            notification.put("notification", notifBody);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    "https://fcm.googleapis.com/fcm/send",
                    notification,
                    response -> {},
                    error -> {}
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "key=" + serverKey);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            RequestQueue queue = Volley.newRequestQueue(ChatActivity.this);
            queue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}