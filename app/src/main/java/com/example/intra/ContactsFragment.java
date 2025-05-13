package com.example.intra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactsFragment extends Fragment {
    private static final String TAG = "Contacts";
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private List<Contact> allContacts;
    private List<Contact> filteredContacts;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private ListenerRegistration usersListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_tab_content, container, false);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.tab_content_list);
        progressBar = view.findViewById(R.id.loading_spinner);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext() != null ? getContext() : requireContext()));
        allContacts = new ArrayList<>();
        filteredContacts = new ArrayList<>();

        adapter = new ContactAdapter(filteredContacts, contact -> openChatWithContact(contact));
        recyclerView.setAdapter(adapter);

        fetchUsers();

        return view;
    }

    private void fetchUsers() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Log.e(TAG, "No authenticated user found");
            if (isAdded()) {
                progressBar.setVisibility(View.GONE);
            }
            return;
        }

        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping fetchUsers");
            return;
        }

        Log.d(TAG, "fetchUsers started");
        progressBar.setVisibility(View.VISIBLE);

        allContacts.clear();
        filteredContacts.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        // Store the listener to remove it later
        usersListener = firestore.collection("Users")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (!isAdded() || getContext() == null || getActivity() == null) {
                        Log.w(TAG, "Fragment not attached, skipping snapshot listener callback");
                        return;
                    }

                    if (e != null) {
                        Log.e(TAG, "Error fetching users: ", e);
                        getActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                        });
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        allContacts.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String userId = document.getId();
                            String name = document.getString("name") != null ? document.getString("name") : "Unknown";
                            String email = document.getString("email") != null ? document.getString("email") : "No email";
                            String profilePicURL = document.getString("profilePicURL");

                            if (!userId.equals(currentUserId)) {
                                allContacts.add(new Contact(userId, name, email, profilePicURL));
                            }
                        }

                        // Update UI on the main thread
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded() || getContext() == null) {
                                Log.w(TAG, "Fragment not attached, skipping UI update");
                                return;
                            }
                            filteredContacts.clear();
                            filteredContacts.addAll(allContacts);
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "Adapter updated with " + filteredContacts.size() + " contacts");
                            } else {
                                Log.e(TAG, "Adapter is null during UI update");
                            }
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                });
    }

    private void openChatWithContact(Contact contact) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found");
            if (isAdded()) {
                Toast.makeText(getContext(), "Please sign in to start a chat.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        String userId = currentUser.getUid();
        String contactUserId = contact.getUserId();

        // Check for existing chat
        firestore.collection("Chats")
                .whereArrayContains("participants", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Fragment not attached, skipping chat check callback");
                        return;
                    }
                    String chatID = null;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        List<String> participants = (List<String>) doc.get("participants");
                        if (participants != null && participants.contains(contactUserId)) {
                            chatID = doc.getId();
                            break;
                        }
                    }

                    if (chatID != null) {
                        // Existing chat found, open ChatActivity
                        startChatActivity(contact.getName(), chatID, contactUserId);
                    } else {
                        // No chat exists, create a new one
                        createNewChat(userId, contactUserId, contact.getName());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for existing chat: ", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to open chat. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createNewChat(String userId, String contactUserId, String contactName) {
        List<String> participants = Arrays.asList(userId, contactUserId);
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("participants", participants);
        chatData.put("lastMessage", "");
        chatData.put("lastSenderID", "");
        chatData.put("lastMessageTimeStamp", new com.google.firebase.Timestamp(new Date(0)));
        chatData.put("archived", false);

        firestore.collection("Chats")
                .add(chatData)
                .addOnSuccessListener(documentReference -> {
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Fragment not attached, skipping new chat callback");
                        return;
                    }
                    String chatID = documentReference.getId();
                    startChatActivity(contactName, chatID, contactUserId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating new chat: ", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to create chat. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startChatActivity(String contactName, String chatID, String receiverId) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("contact_name", contactName);
        intent.putExtra("chat_id", chatID);
        intent.putExtra("receiver", receiverId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start ChatActivity: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Unable to open chat. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called");
        super.onDestroyView();
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
            Log.d(TAG, "Firestore listener removed");
        }
        recyclerView.setAdapter(null);
        adapter = null;
    }

    public void filterContacts(String query) {
        if (!isAdded() || adapter == null || filteredContacts == null || allContacts == null) {
            Log.w(TAG, "Fragment not attached or adapter/lists null, skipping filter");
            return;
        }
        Log.d(TAG, "Filtering contacts with query: " + (query != null ? query : "null"));
        filteredContacts.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredContacts.addAll(allContacts);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Contact contact : allContacts) {
                if ((contact.getName() != null && contact.getName().toLowerCase().contains(lowerCaseQuery)) ||
                        (contact.getEmail() != null && contact.getEmail().toLowerCase().contains(lowerCaseQuery))) {
                    filteredContacts.add(contact);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            Log.d(TAG, "Filtered contacts: " + filteredContacts.size());
        } else {
            Log.e(TAG, "Adapter is null during filter");
        }
    }
}