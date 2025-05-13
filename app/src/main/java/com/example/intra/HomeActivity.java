package com.example.intra;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {
    FirebaseAuth auth;
    MaterialButton unarchivedChatsBtn, contactsBtn, logoutBtn;
    TextView userDetails;
    TextView tabHeading;
    TextInputLayout searchBar;
    TextInputEditText searchInput;
    FirebaseUser user;
    private UnarchivedChatsFragment unarchivedChatsFragment;
    private ContactsFragment contactsFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        setContentView(R.layout.activity_home);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Initialize views
        unarchivedChatsBtn = findViewById(R.id.unarchived_chats_btn);
        contactsBtn = findViewById(R.id.contacts_btn);
        logoutBtn = findViewById(R.id.logout_btn);
        userDetails = findViewById(R.id.user_details);
        tabHeading = findViewById(R.id.tab_heading);
        searchBar = findViewById(R.id.search_bar);
        searchInput = (TextInputEditText) searchBar.getEditText();

        // Check if user is logged in
        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        userDetails = findViewById(R.id.user_details);

        // Set user greeting
        FirebaseFirestore.getInstance().collection("Users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        userDetails.setText("Hello, " + name);
                    } else {
                        userDetails.setText("Unknown User");
                    }
                })
                .addOnFailureListener(e -> {
                    userDetails.setText("Error loading name");
                });


        // Create fragment instances
        unarchivedChatsFragment = new UnarchivedChatsFragment();
        contactsFragment = new ContactsFragment();

        // Show Unarchived Chats by default
        switchFragment(unarchivedChatsFragment, "Chats", "Search Chat");
        activeFragment = unarchivedChatsFragment;
        highlightButton(unarchivedChatsBtn);

        // Navigation button listeners
        unarchivedChatsBtn.setOnClickListener(v -> {
            switchFragment(unarchivedChatsFragment, "Chats", "Search Chat");
            activeFragment = unarchivedChatsFragment;
            highlightButton(unarchivedChatsBtn);
            filterActiveFragment(searchInput != null ? searchInput.getText().toString() : "");
        });

        contactsBtn.setOnClickListener(v -> {
            switchFragment(contactsFragment, "My Contacts", "Search Contact");
            activeFragment = contactsFragment;
            highlightButton(contactsBtn);
            filterActiveFragment(searchInput != null ? searchInput.getText().toString() : "");
        });

        // Search bar listener
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterActiveFragment(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Logout button
        logoutBtn.setOnClickListener(v -> {
            updateOnlineStatus(false);
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        });

    }

    private void switchFragment(Fragment fragment, String tabTitle, String searchHint) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitNow(); // Use commitNow to ensure the fragment is attached immediately
        tabHeading.setText(tabTitle);
        searchBar.setHint(searchHint);
    }

    private void highlightButton(MaterialButton selectedButton) {
        unarchivedChatsBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.neon_teal));
        contactsBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.neon_teal));
        selectedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.dark_teal));
    }

    private void filterActiveFragment(String query) {
        if (activeFragment == null || !activeFragment.isAdded()) {
            return; // Skip filtering if the fragment is not in a valid state
        }
        if (activeFragment instanceof UnarchivedChatsFragment) {
            ((UnarchivedChatsFragment) activeFragment).filterChats(query);
        }  else if (activeFragment instanceof ContactsFragment) {
            ((ContactsFragment) activeFragment).filterContacts(query);
        }
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof TextInputEditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();

                    // Hide the keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void updateOnlineStatus(boolean isOnline) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(currentUser.getUid())
                    .update(
                            "isOnline", isOnline,
                            "lastSeen", com.google.firebase.firestore.FieldValue.serverTimestamp()
                    );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateOnlineStatus(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateOnlineStatus(false);
    }


}