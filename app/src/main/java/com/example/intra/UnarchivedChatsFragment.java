package com.example.intra;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class UnarchivedChatsFragment extends Fragment {
    private static final String TAG = "UnarchivedChatsFragment";
    private static final long LOADING_TIMEOUT_MS = 10000; // 10 seconds
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds
    private static final String ENCRYPTION_KEY = "your16bytekey123"; // Must match ChatActivity
    private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<Chat> allChats;
    private List<Chat> filteredChats;
    private ProgressBar loadingSpinner;
    private ListenerRegistration chatsListener;
    private Map<String, ListenerRegistration> userStatusListeners;
    private Map<String, ListenerRegistration> messageListeners;
    private final Map<String, UserInfo> userInfoCache = new HashMap<>();
    private final Map<String, String> lastMessageCache = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int retryCount = 0;

    private static class UserInfo {
        String name;
        String profilePicURL;
        boolean isOnline;

        UserInfo(String name, String profilePicURL, boolean isOnline) {
            this.name = name;
            this.profilePicURL = profilePicURL;
            this.isOnline = isOnline;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_content, container, false);
        loadingSpinner = view.findViewById(R.id.loading_spinner);

        // Initialize views
        recyclerView = view.findViewById(R.id.tab_content_list);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        allChats = new ArrayList<>();
        filteredChats = new ArrayList<>();
        userStatusListeners = new HashMap<>();
        messageListeners = new HashMap<>();

        // Initialize adapter
        adapter = new ChatAdapter(filteredChats, chat -> {
            if (!isAdded() || getContext() == null || getActivity() == null) {
                Log.e(TAG, "Fragment not attached or activity is null, cannot start ChatActivity");
                return;
            }
            Log.d(TAG, "Chat clicked: " + chat.getContactName());
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("contact_name", chat.getContactName());
            intent.putExtra("chat_id", chat.getChatID());
            intent.putExtra("receiver", chat.getOtherUserId());
            intent.putExtra("pfp", chat.getProfilePicURL());
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to start ChatActivity: ", e);
                Toast.makeText(getContext(), "Unable to open chat. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        // Fetch chats
        fetchChats();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetchChats(); // Ensure listener is reattached after view recreation
    }

    private String decryptMessage(String encryptedText, String ivBase64) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes("UTF-8"), "AES");
            byte[] iv = Base64.decode(ivBase64, Base64.DEFAULT);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(Base64.decode(encryptedText, Base64.DEFAULT));
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "Decryption error: " + e.getMessage());
            return "[Decryption Failed]";
        }
    }

    private void fetchChats() {
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping fetchChats");
            return;
        }
        loadingSpinner.setVisibility(View.VISIBLE);

        // Check Google Play Services
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(getContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services unavailable, resultCode: " + resultCode);
            getActivity().runOnUiThread(() -> {
                loadingSpinner.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Google Play Services is unavailable or restricted. Please enable it in Settings > Apps > Google Play Services.", Toast.LENGTH_LONG).show();
            });
            return;
        }

        // Set loading timeout
        handler.postDelayed(() -> {
            if (loadingSpinner.getVisibility() == View.VISIBLE) {
                loadingSpinner.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Chats failed to load. Please check your connection and Google Play Services settings.", Toast.LENGTH_LONG).show();
            }
        }, LOADING_TIMEOUT_MS);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found");
            getActivity().runOnUiThread(() -> {
                loadingSpinner.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Please sign in to view chats.", Toast.LENGTH_LONG).show();
            });
            handler.removeCallbacksAndMessages(null);
            return;
        }
        String userId = currentUser.getUid();

        // Clear chats only on initial fetch
        if (chatsListener == null) {
            allChats.clear();
            filteredChats.clear();
            lastMessageCache.clear();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }

        try {
            chatsListener = db.collection("Chats")
                    .whereArrayContains("participants", userId)
                    .orderBy("lastMessageTimeStamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        handler.removeCallbacksAndMessages(null);
                        if (!isAdded() || getContext() == null || getActivity() == null) {
                            Log.w(TAG, "Fragment not attached, skipping snapshot listener callback");
                            return;
                        }
                        if (e != null) {
                            Log.e(TAG, "Firestore listener error: ", e);
                            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
                            getActivity().runOnUiThread(() -> {
                                loadingSpinner.setVisibility(View.GONE);
                                if (errorMessage.contains("FAILED_PRECONDITION") && errorMessage.contains("requires an index")) {
                                    Toast.makeText(getContext(), "Chats require a Firestore index. Create it in Firebase Console using the URL in Logcat.", Toast.LENGTH_LONG).show();
                                } else if (errorMessage.contains("FAILED_PRECONDITION")) {
                                    Toast.makeText(getContext(), "Query failed: " + errorMessage + ". Check authentication or Firestore rules.", Toast.LENGTH_LONG).show();
                                } else if (errorMessage.contains("SecurityException") || errorMessage.contains("fir-auth-gms")) {
                                    Toast.makeText(getContext(), "Firebase Authentication is disabled. Please enable fir-auth-gms.firebaseapp.com in Settings > Apps > Google Play Services > Data Usage.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getContext(), "Failed to load chats: " + errorMessage, Toast.LENGTH_LONG).show();
                                }
                            });
                            if (retryCount < MAX_RETRIES && (errorMessage.contains("SecurityException") || errorMessage.contains("fir-auth-gms") || errorMessage.contains("FAILED_PRECONDITION"))) {
                                retryCount++;
                                long delay = RETRY_DELAY_MS * (long) Math.pow(2, retryCount); // Exponential backoff
                                Log.d(TAG, "Retrying fetchChats, attempt " + retryCount + ", delay: " + delay + "ms");
                                handler.postDelayed(this::fetchChats, delay);
                            }
                            return;
                        }
                        if (queryDocumentSnapshots == null) {
                            Log.w(TAG, "No chat documents received");
                            getActivity().runOnUiThread(() -> loadingSpinner.setVisibility(View.GONE));
                            return;
                        }

                        Log.d(TAG, "Snapshot listener triggered, documents: " + queryDocumentSnapshots.size());
                        retryCount = 0; // Reset retry count on success

                        // Collect user IDs and chats to fetch messages
                        Set<String> userIdsToFetch = new HashSet<>();
                        Set<String> chatsToFetchMessages = new HashSet<>();

                        // Process changes incrementally
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String chatID = doc.getId();
                            boolean isArchived = doc.getBoolean("archived") != null && doc.getBoolean("archived");
                            if (isArchived) {
                                int position = findChatPosition(chatID);
                                if (position >= 0) {
                                    allChats.remove(position);
                                    getActivity().runOnUiThread(() -> {
                                        filteredChats.remove(position);
                                        adapter.notifyItemRemoved(position);
                                    });
                                    if (messageListeners.containsKey(chatID)) {
                                        messageListeners.get(chatID).remove();
                                        messageListeners.remove(chatID);
                                    }
                                }
                                continue;
                            }

                            String lastMessage = doc.getString("lastMessage");
                            String lastSenderId = doc.getString("lastSenderID");
                            Timestamp timestamp = doc.getTimestamp("lastMessageTimeStamp");
                            long lastMessageTimeStamp = (timestamp != null) ? timestamp.toDate().getTime() : 0L;
                            List<String> participants = (List<String>) doc.get("participants");

                            Log.d(TAG, "Processing chat: " + chatID + ", lastMessage: " + lastMessage + ", timestamp: " + lastMessageTimeStamp + ", participants: " + participants);

                            if (participants == null || participants.size() != 2) {
                                Log.w(TAG, "Invalid participants for chat " + chatID);
                                continue;
                            }

                            String otherUserId = participants.get(0).equals(userId) ? participants.get(1) : participants.get(0);

                            // Fetch unhashed message immediately after getting lastMessage
                            fetchLatestMessage(chatID);
                            chatsToFetchMessages.add(chatID);

                            // Check if chat exists
                            Chat existingChat = allChats.stream()
                                    .filter(chat -> chat.getChatID().equals(chatID))
                                    .findFirst()
                                    .orElse(null);

                            // Use cached unhashed message or fall back to hashed
                            String displayMessage = lastMessageCache.getOrDefault(chatID, lastMessage != null ? lastMessage : "");

                            if (existingChat != null) {
                                // Update existing chat
                                int oldPosition = findChatPosition(chatID);
                                existingChat.setLastMessage(displayMessage);
                                existingChat.setLastSenderId(lastSenderId);
                                existingChat.setLastMessageTimestamp(lastMessageTimeStamp);
                                allChats.sort((c1, c2) -> Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));
                                int newPosition = findChatPosition(chatID);
                                getActivity().runOnUiThread(() -> {
                                    filteredChats.clear();
                                    filteredChats.addAll(allChats);
                                    if (oldPosition != newPosition) {
                                        adapter.notifyItemMoved(oldPosition, newPosition);
                                    } else {
                                        adapter.notifyItemChanged(newPosition);
                                    }
                                });
                            } else {
                                // Check cache for participant info
                                UserInfo cachedInfo = userInfoCache.get(otherUserId);
                                if (cachedInfo != null) {
                                    Chat newChat = new Chat(
                                            cachedInfo.name != null ? cachedInfo.name : "Unknown",
                                            displayMessage,
                                            lastSenderId,
                                            lastMessageTimeStamp,
                                            cachedInfo.isOnline,
                                            otherUserId,
                                            cachedInfo.profilePicURL,
                                            chatID
                                    );
                                    allChats.add(newChat);
                                    allChats.sort((c1, c2) -> Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));
                                    int newPosition = findChatPosition(chatID);
                                    getActivity().runOnUiThread(() -> {
                                        filteredChats.clear();
                                        filteredChats.addAll(allChats);
                                        adapter.notifyItemInserted(newPosition);
                                    });
                                } else {
                                    userIdsToFetch.add(otherUserId);
                                    Chat newChat = new Chat(
                                            "Loading...",
                                            displayMessage,
                                            lastSenderId,
                                            lastMessageTimeStamp > 0 ? lastMessageTimeStamp : System.currentTimeMillis(), // Fallback timestamp
                                            false,
                                            otherUserId,
                                            null,
                                            chatID
                                    );
                                    allChats.add(newChat);
                                    allChats.sort((c1, c2) -> Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));
                                    int newPosition = findChatPosition(chatID);
                                    getActivity().runOnUiThread(() -> {
                                        filteredChats.clear();
                                        filteredChats.addAll(allChats);
                                        adapter.notifyItemInserted(newPosition);
                                    });
                                }
                            }
                        }

                        // Batch fetch user info
                        if (!userIdsToFetch.isEmpty()) {
                            batchFetchUserInfo(userIdsToFetch);
                        }

                        // Hide spinner only after all messages and user info are processed
                        if (userIdsToFetch.isEmpty() && chatsToFetchMessages.isEmpty()) {
                            getActivity().runOnUiThread(() -> loadingSpinner.setVisibility(View.GONE));
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to set up Firestore listener: ", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            getActivity().runOnUiThread(() -> {
                loadingSpinner.setVisibility(View.GONE);
                if (errorMessage.contains("FAILED_PRECONDITION") && errorMessage.contains("requires an index")) {
                    Toast.makeText(getContext(), "Chats require a Firestore index. Create it in Firebase Console using the URL in Logcat.", Toast.LENGTH_LONG).show();
                } else if (errorMessage.contains("FAILED_PRECONDITION")) {
                    Toast.makeText(getContext(), "Query failed: " + errorMessage + ". Check authentication or Firestore rules.", Toast.LENGTH_LONG).show();
                } else if (errorMessage.contains("SecurityException") || errorMessage.contains("fir-auth-gms")) {
                    Toast.makeText(getContext(), "Firebase Authentication is disabled. Please enable fir-auth-gms.firebaseapp.com in Settings > Apps > Google Play Services > Data Usage.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Failed to initialize chats: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
            if (retryCount < MAX_RETRIES) {
                retryCount++;
                long delay = RETRY_DELAY_MS * (long) Math.pow(2, retryCount); // Exponential backoff
                Log.d(TAG, "Retrying fetchChats, attempt " + retryCount + ", delay: " + delay + "ms");
                handler.postDelayed(this::fetchChats, delay);
            }
        }
    }

    private void fetchLatestMessage(String chatID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (messageListeners.containsKey(chatID)) {
            Log.d(TAG, "Skipping duplicate message listener for chat: " + chatID);
            return;
        }
        ListenerRegistration listener = db.collection("Chats").document(chatID).collection("Messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (!isAdded() || getContext() == null || getActivity() == null) {
                        Log.w(TAG, "Fragment not attached, skipping message snapshot");
                        return;
                    }
                    if (e != null) {
                        Log.e(TAG, "Error fetching latest message for chat " + chatID + ": ", e);
                        return;
                    }
                    String lastMessageText = "[Message Unavailable]";
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String text = doc.getString("text");
                        Log.d(TAG, "Fetched message for chat " + chatID + ": text=" + text);
                        if (text != null) {
                            if (text.startsWith("https://mwbfpvqaygvxmpsysjbf.supabase.co")) {
                                lastMessageText = text; // File URL, use as-is
                            } else {
                                String ivBase64 = doc.getString("iv");
                                if (ivBase64 != null) {
                                    lastMessageText = decryptMessage(text, ivBase64);
                                } else {
                                    lastMessageText = "[Decryption Failed: Missing IV]";
                                    Log.w(TAG, "Missing IV for encrypted message in chat: " + chatID);
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "No messages found for chat: " + chatID);
                    }
                    lastMessageCache.put(chatID, lastMessageText);
                    updateChatWithLastMessage(chatID, lastMessageText);

                    // Hide spinner if all messages and user info are processed
                    if (userInfoCache.keySet().containsAll(allChats.stream().map(Chat::getOtherUserId).collect(Collectors.toList())) &&
                            lastMessageCache.keySet().containsAll(allChats.stream().map(Chat::getChatID).collect(Collectors.toList()))) {
                        getActivity().runOnUiThread(() -> loadingSpinner.setVisibility(View.GONE));
                    }
                });
        messageListeners.put(chatID, listener);
    }

    private void updateChatWithLastMessage(String chatID, String lastMessage) {
        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "Fragment not attached, skipping last message update");
                return;
            }
            boolean updated = false;
            for (Chat chat : allChats) {
                if (chat.getChatID().equals(chatID)) {
                    chat.setLastMessage(lastMessage);
                    int position = allChats.indexOf(chat);
                    if (position >= 0) {
                        updated = true;
                        int filteredPosition = filteredChats.indexOf(chat);
                        if (filteredPosition >= 0) {
                            filteredChats.set(filteredPosition, chat);
                            adapter.notifyItemChanged(filteredPosition);
                        }
                    }
                }
            }
            if (updated) {
                Log.d(TAG, "Updated last message for chat: " + chatID);
            } else {
                Log.w(TAG, "No chats updated for chat: " + chatID);
            }
        });
    }

    private void batchFetchUserInfo(Set<String> userIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (String userId : userIds) {
            if (userStatusListeners.containsKey(userId)) {
                Log.d(TAG, "Skipping duplicate listener for user: " + userId);
                continue;
            }
            ListenerRegistration listener = db.collection("Users").document(userId)
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (!isAdded() || getContext() == null || getActivity() == null) {
                            Log.w(TAG, "Fragment not attached, skipping user snapshot");
                            return;
                        }
                        String name = null;
                        String profilePicURL = null;
                        boolean isOnline = false;

                        if (e != null) {
                            Log.e(TAG, "Error fetching user data for " + userId + ": ", e);
                            return;
                        }

                        Log.d(TAG, "Snapshot received for user: " + userId);
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            name = documentSnapshot.getString("name");
                            profilePicURL = documentSnapshot.getString("profilePicURL");
                            Boolean onlineStatus = documentSnapshot.getBoolean("isOnline");
                            isOnline = onlineStatus != null ? onlineStatus : false;
                            userInfoCache.put(userId, new UserInfo(name, profilePicURL, isOnline));
                            Log.d(TAG, "Fetched data for " + userId + ": name=" + name + ", isOnline=" + isOnline + ", profilePicURL=" + profilePicURL);
                        } else {
                            Log.w(TAG, "No document found for user: " + userId);
                        }

                        updateChatWithUserInfo(userId, name, isOnline, profilePicURL);

                        // Hide spinner after all users are processed
                        if (userInfoCache.keySet().containsAll(userIds)) {
                            getActivity().runOnUiThread(() -> loadingSpinner.setVisibility(View.GONE));
                        }
                    });
            userStatusListeners.put(userId, listener);
        }
    }

    private int findChatPosition(String chatID) {
        for (int i = 0; i < allChats.size(); i++) {
            if (allChats.get(i).getChatID().equals(chatID)) {
                return i;
            }
        }
        return -1;
    }

    private void updateChatWithUserInfo(String participantId, String name, boolean isOnline, String profilePicURL) {
        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "Fragment not attached, skipping user info update");
                return;
            }
            boolean updated = false;
            for (Chat chat : allChats) {
                if (chat.getOtherUserId() != null && chat.getOtherUserId().equals(participantId)) {
                    chat.setOnline(isOnline);
                    chat.setContactName(name != null ? name : "Unknown");
                    chat.setProfilePicURL(profilePicURL);
                    int position = allChats.indexOf(chat);
                    if (position >= 0) {
                        updated = true;
                        int filteredPosition = filteredChats.indexOf(chat);
                        if (filteredPosition >= 0) {
                            filteredChats.set(filteredPosition, chat);
                            adapter.notifyItemChanged(filteredPosition);
                        }
                    }
                }
            }
            if (updated) {
                Log.d(TAG, "Updated user info for participant: " + participantId + ", isOnline: " + isOnline);
            } else {
                Log.w(TAG, "No chats updated for participant: " + participantId);
            }
        });
    }

    public void filterChats(String query) {
        if (adapter == null || filteredChats == null || allChats == null) {
            Log.w(TAG, "Adapter or chat lists are null, skipping filter");
            return;
        }
        filteredChats.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredChats.addAll(allChats);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Chat chat : allChats) {
                if ((chat.getContactName() != null && chat.getContactName().toLowerCase().contains(lowerCaseQuery)) ||
                        (chat.getLastMessage() != null && chat.getLastMessage().toLowerCase().contains(lowerCaseQuery))) {
                    filteredChats.add(chat);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            Log.d(TAG, "Filtered chats: " + filteredChats.size());
        } else {
            Log.e(TAG, "Adapter is null during filter");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
            Log.d(TAG, "Chats Firestore listener removed");
        }
        for (ListenerRegistration listener : userStatusListeners.values()) {
            listener.remove();
        }
        userStatusListeners.clear();
        for (ListenerRegistration listener : messageListeners.values()) {
            listener.remove();
        }
        messageListeners.clear();
        Log.d(TAG, "User status and message listeners removed");
        recyclerView.setAdapter(null);
        adapter = null;
    }

    public interface OnNameFetchedListener {
        void onNameFetched(String name, boolean isOnline, String profilePicURL);
    }
}