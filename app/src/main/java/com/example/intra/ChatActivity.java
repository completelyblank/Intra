package com.example.intra;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.util.Base64;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final long STATUS_CHECK_INTERVAL_MS = 2000; // Check status every 2s
    private static final String ENCRYPTION_KEY = "your16bytekey123"; // Must be 16 bytes for AES-128
    private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";
    private Uri selectedFileUri;
    private TextView contactNameTextView, lastSeenTextView;
    private ImageView statusCircle;
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private ImageView sendButton, backArrow, profilePic, uploadButton;
    private ProgressBar loadingSpinner;
    private MessageAdapter messageAdapter;
    private List<MessageItem> messageItems;
    private ListenerRegistration userStatusListener;
    private String receiverID;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable statusCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (getApplication() instanceof MyApplication) {
                ((MyApplication) getApplication()).updateOnlineStatus(true);
                Log.d(TAG, "Periodic status check: set user online status to true");
                handler.postDelayed(this, STATUS_CHECK_INTERVAL_MS);
            }
        }
    };

    // Method to hash a message using SHA-256
    private String hashMessage(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            return Base64.encodeToString(hash, Base64.DEFAULT).trim();
        } catch (Exception e) {
            Log.e(TAG, "Hashing error: " + e.getMessage());
            return input; // Fallback to original input if hashing fails
        }
    }

    // Activity Result Launcher for file picking
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    if (selectedFileUri != null) {
                        Log.d(TAG, "File selected: " + selectedFileUri.toString());
                        String fileName = selectedFileUri.getLastPathSegment() != null
                                ? selectedFileUri.getLastPathSegment().replaceAll("[^a-zA-Z0-9.-]", "_")
                                : "file_" + System.currentTimeMillis();
                        long currentTime = System.currentTimeMillis();
                        String chatID = getIntent().getStringExtra("chat_id");
                        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        // Display the file message in the chat
                        Message fileMessage = new Message("https://mwbfpvqaygvxmpsysjbf.supabase.co/storage/v1/object/public/chatfiles/" + fileName, true, currentTime);
                        messageItems.add(new MessageItem.MessageWrapper(fileMessage));
                        int insertedPosition = messageItems.size() - 1;
                        messageAdapter.notifyItemInserted(insertedPosition);
                        messagesRecyclerView.scrollToPosition(insertedPosition);

                        // Upload file to Supabase
                        new Thread(() -> {
                            try {
                                OkHttpClient client = new OkHttpClient();
                                String supabaseUrl = "https://mwbfpvqaygvxmpsysjbf.supabase.co/storage/v1/object/chatfiles/" + fileName;
                                String supabaseApiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im13YmZwdnFheWd2eG1wc3lzamJmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDYyOTA2ODIsImV4cCI6MjA2MTg2NjY4Mn0.-vFnH486sJvdLEqvR2mHfvDM9NNikp7MLuGaXEafll0";
                                String bearerToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im13YmZwdnFheWd2eG1wc3lzamJmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDYyOTA2ODIsImV4cCI6MjA2MTg2NjY4Mn0.-vFnH486sJvdLEqvR2mHfvDM9NNikp7MLuGaXEafll0";

                                // Read file content from Uri
                                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
                                if (inputStream == null) {
                                    Log.e(TAG, "Failed to open input stream for file");
                                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed to read file", Toast.LENGTH_SHORT).show());
                                    return;
                                }

                                // Read stream into byte array
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                                }
                                inputStream.close();
                                byte[] fileBytes = byteArrayOutputStream.toByteArray();

                                // Determine MIME type (optional, fallback to generic)
                                String mimeType = getContentResolver().getType(selectedFileUri);
                                if (mimeType == null) {
                                    mimeType = "application/octet-stream";
                                }

                                // Create request body
                                RequestBody fileBody = RequestBody.create(fileBytes, MediaType.parse(mimeType));
                                RequestBody requestBody = new MultipartBody.Builder()
                                        .setType(MultipartBody.FORM)
                                        .addFormDataPart("file", fileName, fileBody)
                                        .build();

                                // Build the request with authorization headers
                                Request request = new Request.Builder()
                                        .url(supabaseUrl)
                                        .post(requestBody)
                                        .addHeader("Authorization", "Bearer " + bearerToken)
                                        .addHeader("apikey", supabaseApiKey)
                                        .addHeader("Content-Type", "multipart/form-data")
                                        .build();

                                // Execute the request
                                try (Response response = client.newCall(request).execute()) {
                                    if (response.isSuccessful()) {
                                        Log.d(TAG, "File uploaded successfully to Supabase: " + fileName);
                                        // Construct the public URL for the uploaded file
                                        String fileUrl = "https://mwbfpvqaygvxmpsysjbf.supabase.co/storage/v1/object/public/chatfiles/" + fileName;

                                        // Store file message in Firestore
                                        Map<String, Object> messageData = new HashMap<>();
                                        messageData.put("text", fileUrl);
                                        messageData.put("senderID", userID);
                                        messageData.put("timestamp", new Timestamp(new Date(currentTime)));
                                        messageData.put("receiverID", receiverID);
                                        messageData.put("isRead", false);

                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                        db.collection("Chats").document(chatID).collection("Messages")
                                                .add(messageData)
                                                .addOnSuccessListener(documentReference -> {
                                                    Log.d(TAG, "File message stored in Firestore");
                                                    // Update chat metadata
                                                    Map<String, Object> chatData = new HashMap<>();
                                                    chatData.put("lastMessage", hashMessage(fileUrl));
                                                    chatData.put("lastMessageTimeStamp", new Timestamp(new Date(currentTime)));
                                                    chatData.put("lastSenderID", userID);
                                                    db.collection("Chats").document(chatID)
                                                            .update(chatData)
                                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Chat updated with file message"))
                                                            .addOnFailureListener(e -> Log.e(TAG, "Error updating chat: " + e.getMessage()));
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Error storing file message in Firestore: " + e.getMessage());
                                                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed to send file", Toast.LENGTH_SHORT).show());
                                                });
                                    } else {
                                        Log.e(TAG, "File upload failed: " + response.message() + ", Code: " + response.code());
                                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed to upload file", Toast.LENGTH_SHORT).show());
                                    }
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error uploading file: " + e.getMessage());
                                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Error uploading file", Toast.LENGTH_SHORT).show());
                            }
                        }).start();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        try {
            setContentView(R.layout.activity_chat);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set content view: " + e.getMessage());
            finish();
            return;
        }
        loadingSpinner = findViewById(R.id.loading_spinner);
        loadingSpinner.setVisibility(View.VISIBLE);

        // Signal transition to ChatActivity
        if (getApplication() instanceof MyApplication) {
            ((MyApplication) getApplication()).setTransitioningToChatActivity(true);
            Log.d(TAG, "Signaled transitioning to ChatActivity");
            // Set user online status immediately
            ((MyApplication) getApplication()).updateOnlineStatus(true);
            Log.d(TAG, "Set user online status to true on ChatActivity onCreate");
        }

        // Initialize views
        contactNameTextView = findViewById(R.id.chat_contact_name);
        lastSeenTextView = findViewById(R.id.last_seen_text);
        statusCircle = findViewById(R.id.status_circle);
        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        backArrow = findViewById(R.id.back_arrow);
        profilePic = findViewById(R.id.profile_picture);
        uploadButton = findViewById(R.id.attach_button);

        if (contactNameTextView == null || lastSeenTextView == null || messagesRecyclerView == null ||
                profilePic == null || messageInput == null || sendButton == null || backArrow == null ||
                uploadButton == null) {
            Log.e(TAG, "One or more views are null, finishing activity");
            finish();
            return;
        }

        // Get data from Intent
        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "Intent is null, finishing activity");
            finish();
            return;
        }

        String contactName = intent.getStringExtra("contact_name");
        String chatID = intent.getStringExtra("chat_id");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        receiverID = intent.getStringExtra("receiver");
        String pfp = intent.getStringExtra("pfp");
        String userID = currentUser != null ? currentUser.getUid() : null;

        if (userID == null) {
            Log.e(TAG, "No authenticated user found, finishing activity");
            finish();
            return;
        }

        // Set contact name
        contactNameTextView.setText(contactName != null ? contactName : "Unknown");

        // Load profile picture
        if (pfp != null && !pfp.isEmpty()) {
            Glide.with(this)
                    .load(pfp)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(profilePic);
        } else {
            profilePic.setImageResource(R.drawable.ic_person);
        }

        // Set up RecyclerView
        messageItems = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageItems);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);

        // Fetch messages
        fetchMessages(chatID, userID);

        // Fetch and listen for online status
        fetchUserStatus(receiverID);

        // Back arrow click listener
        backArrow.setOnClickListener(v -> finish());

        // Attach button click listener
        uploadButton.setOnClickListener(v -> {
            Log.d(TAG, "Attach button clicked");
            Intent filePickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
            filePickerIntent.setType("*/*");
            filePickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(Intent.createChooser(filePickerIntent, "Select a file"));
        });

        // Send button click listener
        sendButton.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim();
            if (!messageText.isEmpty()) {
                long currentTime = System.currentTimeMillis();
                Message newMessage = new Message(messageText, true, currentTime);

                // Check if date header is needed for the new message
                Calendar currentDate = Calendar.getInstance();
                currentDate.setTimeInMillis(currentTime);
                Calendar lastDate = messageItems.isEmpty() ? null : getLastMessageDate();
                if (lastDate == null || !isSameDay(lastDate, currentDate)) {
                    String headerText = getFormattedDateHeader(currentTime);
                    messageItems.add(new MessageItem.DateHeader(headerText));
                    messageAdapter.notifyItemInserted(messageItems.size() - 1);
                }

                messageItems.add(new MessageItem.MessageWrapper(newMessage));
                messageAdapter.notifyItemInserted(messageItems.size() - 1);
                messagesRecyclerView.scrollToPosition(messageItems.size() - 1);
                messageInput.setText("");

                // Encrypt the message text
                String encryptedText;
                byte[] iv;
                try {
                    Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
                    SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes("UTF-8"), "AES");
                    SecureRandom random = new SecureRandom();
                    iv = new byte[cipher.getBlockSize()];
                    random.nextBytes(iv);
                    IvParameterSpec ivSpec = new IvParameterSpec(iv);
                    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
                    byte[] encrypted = cipher.doFinal(messageText.getBytes("UTF-8"));
                    encryptedText = Base64.encodeToString(encrypted, Base64.DEFAULT);
                } catch (Exception e) {
                    Log.e(TAG, "Encryption error: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed to encrypt message", Toast.LENGTH_SHORT).show());
                    return;
                }

                Map<String, Object> messageData = new HashMap<>();
                messageData.put("text", encryptedText);
                messageData.put("iv", Base64.encodeToString(iv, Base64.DEFAULT)); // Store IV with the message
                messageData.put("senderID", userID);
                messageData.put("timestamp", new Timestamp(new Date(currentTime)));
                messageData.put("receiverID", receiverID);
                messageData.put("isRead", false);

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("Chats").document(chatID).collection("Messages")
                        .add(messageData)
                        .addOnSuccessListener(documentReference -> {
                            Log.d(TAG, "Encrypted message sent successfully");

                            Map<String, Object> chatData = new HashMap<>();
                            chatData.put("lastMessage", hashMessage(messageText));
                            chatData.put("lastMessageTimeStamp", new Timestamp(new Date(currentTime)));
                            chatData.put("lastSenderID", userID);

                            db.collection("Chats").document(chatID)
                                    .update(chatData)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Chat updated with last message details"))
                                    .addOnFailureListener(e -> Log.e(TAG, "Error updating chat: " + e.getMessage()));
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Error sending encrypted message: " + e.getMessage()));
            }
        });

        messageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                messagesRecyclerView.post(() -> messagesRecyclerView.scrollToPosition(messageItems.size() - 1));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start periodic status checks
        if (getApplication() instanceof MyApplication) {
            ((MyApplication) getApplication()).updateOnlineStatus(true);
            Log.d(TAG, "Set user online status to true on ChatActivity onStart");
            handler.post(statusCheckRunnable);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop periodic status checks
        handler.removeCallbacks(statusCheckRunnable);
        if (getApplication() instanceof MyApplication) {
            ((MyApplication) getApplication()).setTransitioningToChatActivity(false);
            Log.d(TAG, "Cleared transitioning to ChatActivity onStop");
        }
    }

    private void fetchMessages(String chatID, String userID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Chats").document(chatID).collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Message fetch failed: " + e.getMessage());
                        runOnUiThread(() -> loadingSpinner.setVisibility(View.GONE));
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        messageItems.clear();
                        Calendar lastDate = null;

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String text = doc.getString("text");
                            String senderID = doc.getString("senderID");
                            Timestamp firebaseTimestamp = doc.getTimestamp("timestamp");
                            Long timestamp = firebaseTimestamp != null ? firebaseTimestamp.toDate().getTime() : System.currentTimeMillis();
                            Log.d(TAG, "Processing message: text=" + text + ", senderID=" + senderID + ", timestamp=" + timestamp);

                            if (text != null && senderID != null) {
                                String displayText = text;
                                // Check if the message is a file URL
                                if (!text.startsWith("https://mwbfpvqaygvxmpsysjbf.supabase.co")) {
                                    // Decrypt the message if it's not a file URL
                                    try {
                                        String ivBase64 = doc.getString("iv");
                                        if (ivBase64 != null) {
                                            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
                                            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes("UTF-8"), "AES");
                                            byte[] iv = Base64.decode(ivBase64, Base64.DEFAULT);
                                            IvParameterSpec ivSpec = new IvParameterSpec(iv);
                                            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
                                            byte[] decrypted = cipher.doFinal(Base64.decode(text, Base64.DEFAULT));
                                            displayText = new String(decrypted, "UTF-8");
                                        }
                                    } catch (Exception ex) {
                                        Log.e(TAG, "Decryption error: " + ex.getMessage());
                                        displayText = "[Decryption Failed]";
                                    }
                                }

                                Message message = new Message(displayText, senderID.equals(userID), timestamp);
                                Calendar messageDate = Calendar.getInstance();
                                messageDate.setTimeInMillis(timestamp);

                                // Check if date header is needed
                                if (lastDate == null || !isSameDay(lastDate, messageDate)) {
                                    String headerText = getFormattedDateHeader(timestamp);
                                    messageItems.add(new MessageItem.DateHeader(headerText));
                                }

                                messageItems.add(new MessageItem.MessageWrapper(message));
                                lastDate = messageDate;
                            } else {
                                Log.w(TAG, "Skipping message due to missing or invalid fields: " + doc.getId());
                            }
                        }
                        messageAdapter.notifyDataSetChanged();
                        messagesRecyclerView.scrollToPosition(messageItems.size() - 1);
                        runOnUiThread(() -> loadingSpinner.setVisibility(View.GONE));
                    }
                });
    }

    private void fetchUserStatus(String userId) {
        if (userId == null) {
            Log.w(TAG, "Receiver ID is null, cannot fetch user status");
            if (statusCircle != null && lastSeenTextView != null) {
                runOnUiThread(() -> {
                    statusCircle.setImageResource(R.drawable.circle_offline);
                    lastSeenTextView.setText("Offline");
                    Log.d(TAG, "Set status to offline due to null userId");
                });
            }
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        userStatusListener = db.collection("Users").document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error fetching user status for " + userId + ": ", e);
                        if (statusCircle != null && lastSeenTextView != null) {
                            runOnUiThread(() -> {
                                statusCircle.setImageResource(R.drawable.circle_offline);
                                lastSeenTextView.setText("Offline");
                                Log.d(TAG, "Set status to offline due to Firestore error");
                            });
                        }
                        return;
                    }
                    Log.d(TAG, "Snapshot received for user status: " + userId);
                    boolean isOnline;
                    Timestamp lastSeen;
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Boolean onlineStatus = documentSnapshot.getBoolean("isOnline");
                        isOnline = onlineStatus != null ? onlineStatus : false;
                        lastSeen = documentSnapshot.getTimestamp("lastSeen");
                        String name = documentSnapshot.getString("name");
                        String profilePicURL = documentSnapshot.getString("profilePicURL");
                        Log.d(TAG, "Fetched status for " + userId + ": isOnline=" + isOnline +
                                ", lastSeen=" + (lastSeen != null ? lastSeen.toDate() : "null") +
                                ", name=" + name + ", profilePicURL=" + profilePicURL);
                        // Update UI
                        runOnUiThread(() -> {
                            if (name != null && !name.isEmpty()) {
                                contactNameTextView.setText(name);
                                Log.d(TAG, "Updated contact name to: " + name);
                            }
                            if (profilePicURL != null && !profilePicURL.isEmpty()) {
                                Glide.with(this)
                                        .load(profilePicURL)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_person)
                                        .error(R.drawable.ic_person)
                                        .into(profilePic);
                                Log.d(TAG, "Updated profile picture to: " + profilePicURL);
                            }
                            if (statusCircle != null) {
                                statusCircle.setImageResource(isOnline ? R.drawable.circle_online : R.drawable.circle_offline);
                                Log.d(TAG, "Updated status circle to " + (isOnline ? "online" : "offline") + " for user: " + userId);
                            }
                            if (lastSeenTextView != null) {
                                if (isOnline) {
                                    lastSeenTextView.setText("Online");
                                    Log.d(TAG, "Set last seen text to Online for user: " + userId);
                                } else {
                                    String lastSeenText = lastSeen != null ? formatLastSeen(lastSeen.toDate().getTime()) : "Offline";
                                    lastSeenTextView.setText(lastSeenText);
                                    Log.d(TAG, "Set last seen text to: " + lastSeenText + " for user: " + userId);
                                }
                            }
                        });
                    } else {
                        isOnline = false;
                        Log.w(TAG, "No document found for user: " + userId);
                        if (statusCircle != null && lastSeenTextView != null) {
                            runOnUiThread(() -> {
                                statusCircle.setImageResource(R.drawable.circle_offline);
                                lastSeenTextView.setText("Offline");
                                Log.d(TAG, "Set status to offline due to missing document for user: " + userId);
                            });
                        }
                    }
                });
    }

    private String formatLastSeen(long timestamp) {
        Calendar lastSeenDate = Calendar.getInstance();
        lastSeenDate.setTimeInMillis(timestamp);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String timeString = timeFormat.format(new Date(timestamp));

        if (isSameDay(lastSeenDate, today)) {
            return "Last seen today at " + timeString;
        } else if (isSameDay(lastSeenDate, yesterday)) {
            return "Last seen yesterday at " + timeString;
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            String dateString = dateFormat.format(new Date(timestamp));
            return "Last seen on " + dateString + " at " + timeString;
        }
    }

    private boolean isSameDay(Calendar date1, Calendar date2) {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.DAY_OF_YEAR) == date2.get(Calendar.DAY_OF_YEAR);
    }

    private String getFormattedDateHeader(long timestamp) {
        Calendar messageDate = Calendar.getInstance();
        messageDate.setTimeInMillis(timestamp);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        if (isSameDay(messageDate, today)) {
            return "Today";
        } else if (isSameDay(messageDate, yesterday)) {
            return "Yesterday";
        } else {
            return dateFormat.format(new Date(timestamp));
        }
    }

    private Calendar getLastMessageDate() {
        for (int i = messageItems.size() - 1; i >= 0; i--) {
            MessageItem item = messageItems.get(i);
            if (item instanceof MessageItem.MessageWrapper) {
                MessageItem.MessageWrapper messageWrapper = (MessageItem.MessageWrapper) item;
                Calendar date = Calendar.getInstance();
                date.setTimeInMillis(messageWrapper.getTimestamp());
                return date;
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userStatusListener != null) {
            userStatusListener.remove();
            userStatusListener = null;
            Log.d(TAG, "User status listener removed");
        }
        handler.removeCallbacks(statusCheckRunnable);
        if (getApplication() instanceof MyApplication) {
            ((MyApplication) getApplication()).setTransitioningToChatActivity(false);
            Log.d(TAG, "Cleared transitioning to ChatActivity onDestroy");
        }
    }
}