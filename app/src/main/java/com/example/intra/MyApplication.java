package com.example.intra;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static final long OFFLINE_DELAY_MS = 4000; // Delay for background offline updates
    private static final long DEBOUNCE_MS = 500; // Debounce Firestore updates
    private static final long WRITE_TIMEOUT_MS = 5000; // Timeout for synchronous writes
    private int activityCount = 0;
    private boolean isChatActivityActive = false;
    private boolean isTransitioningToChatActivity = false;
    private Class<?> nextActivityClass = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable offlineRunnable;
    private long lastUpdateTime = 0;
    private boolean lastUpdateWasOnline = false;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                nextActivityClass = activity.getClass();
                Log.d(TAG, "onActivityCreated: " + activity.getClass().getSimpleName() + ", nextActivityClass: " + nextActivityClass.getSimpleName() + ", timestamp: " + System.nanoTime());
            }

            @Override
            public void onActivityStarted(Activity activity) {
                // Cancel any pending offline updates
                if (offlineRunnable != null) {
                    handler.removeCallbacks(offlineRunnable);
                    offlineRunnable = null;
                    Log.d(TAG, "Canceled pending offline status update on activity start, timestamp: " + System.nanoTime());
                }

                if (activity instanceof ChatActivity) {
                    isChatActivityActive = true;
                    Log.d(TAG, "ChatActivity started, isChatActivityActive=true");
                }

                if (activityCount == 0) {
                    updateOnlineStatus(true);
                    Log.d(TAG, "App entering foreground, user set online");
                }
                activityCount++;
                Log.d(TAG, "Activity started: " + activity.getClass().getSimpleName() + ", count: " + activityCount + ", isChatActivityActive: " + isChatActivityActive + ", timestamp: " + System.nanoTime());
                nextActivityClass = null; // Reset after start
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log.d(TAG, "onActivityResumed: " + activity.getClass().getSimpleName() + ", timestamp: " + System.nanoTime());
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log.d(TAG, "onActivityPaused: " + activity.getClass().getSimpleName() + ", timestamp: " + System.nanoTime());
            }

            @Override
            public void onActivityStopped(Activity activity) {
                activityCount = Math.max(0, activityCount - 1);
                if (activity instanceof ChatActivity) {
                    isChatActivityActive = false;
                    isTransitioningToChatActivity = false;
                    Log.d(TAG, "ChatActivity stopped, isChatActivityActive=false, isTransitioningToChatActivity=false");
                }

                Log.d(TAG, "Activity stopped: " + activity.getClass().getSimpleName() + ", count: " + activityCount + ", isChatActivityActive: " + isChatActivityActive + ", isTransitioningToChatActivity: " + isTransitioningToChatActivity + ", nextActivityClass: " + (nextActivityClass != null ? nextActivityClass.getSimpleName() : "null") + ", timestamp: " + System.nanoTime());

                // Skip offline scheduling if transitioning to ChatActivity
                if (isTransitioningToChatActivity || nextActivityClass == ChatActivity.class) {
                    Log.d(TAG, "Skipping offline schedule: transitioning to ChatActivity");
                    return;
                }

                // Only schedule offline update if app is in background and no ChatActivity is active
                if (activityCount <= 0 && !isChatActivityActive) {
                    if (offlineRunnable != null) {
                        handler.removeCallbacks(offlineRunnable);
                        offlineRunnable = null;
                        Log.d(TAG, "Canceled existing offline runnable before scheduling new one");
                    }

                    offlineRunnable = () -> {
                        Log.d(TAG, "Delayed runnable executed. activityCount: " + activityCount + ", isChatActivityActive: " + isChatActivityActive + ", isTransitioningToChatActivity: " + isTransitioningToChatActivity + ", timestamp: " + System.nanoTime());
                        if (activityCount <= 0 && !isChatActivityActive && !isTransitioningToChatActivity) {
                            updateOnlineStatus(false);
                            Log.d(TAG, "User marked offline after delay");
                        } else {
                            Log.d(TAG, "No offline update needed; user still active");
                        }
                        offlineRunnable = null;
                    };
                    handler.postDelayed(offlineRunnable, OFFLINE_DELAY_MS);
                    Log.d(TAG, "Scheduled offline status update with delay");
                } else {
                    Log.d(TAG, "Skipping offline schedule: activityCount=" + activityCount + ", isChatActivityActive=" + isChatActivityActive + ", isTransitioningToChatActivity=" + isTransitioningToChatActivity);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.d(TAG, "onActivityDestroyed: " + activity.getClass().getSimpleName() + ", timestamp: " + System.nanoTime());
                if (activity.getClass().equals(nextActivityClass)) {
                    nextActivityClass = null;
                }
                if (activity instanceof ChatActivity) {
                    isTransitioningToChatActivity = false;
                    Log.d(TAG, "ChatActivity destroyed, isTransitioningToChatActivity=false");
                }
            }
        });
    }

    public void setTransitioningToChatActivity(boolean transitioning) {
        isTransitioningToChatActivity = transitioning;
        Log.d(TAG, "setTransitioningToChatActivity: " + transitioning + ", timestamp: " + System.nanoTime());
    }

    public void updateOnlineStatus(boolean isOnline) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found, cannot update online status");
            return;
        }
        long currentTime = System.currentTimeMillis();
        // Prioritize online updates and debounce
        if (currentTime - lastUpdateTime < DEBOUNCE_MS && !(isOnline && !lastUpdateWasOnline)) {
            Log.d(TAG, "Debouncing updateOnlineStatus, skipping isOnline=" + isOnline);
            return;
        }
        lastUpdateTime = currentTime;
        lastUpdateWasOnline = isOnline;

        String userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("isOnline", isOnline);
        if (!isOnline) {
            userData.put("lastSeen", Timestamp.now());
        }

        // Use transaction for atomic update
        db.runTransaction(transaction -> {
                    transaction.set(db.collection("Users").document(userId), userData, SetOptions.merge());
                    return null;
                }).addOnSuccessListener(aVoid -> Log.d(TAG, "Updated online status to " + isOnline + (isOnline ? "" : ", lastSeen: " + Timestamp.now().toDate()) + " for user: " + userId + ", timestamp: " + System.nanoTime()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update online status for " + userId + ": ", e);
                    // Retry once after 2s
                    handler.postDelayed(() -> updateOnlineStatus(isOnline), 2000);
                });
    }

    public void forceOffline() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found, cannot force offline");
            return;
        }

        // Cancel any pending offline updates
        if (offlineRunnable != null) {
            handler.removeCallbacks(offlineRunnable);
            offlineRunnable = null;
            Log.d(TAG, "Canceled pending offline runnable for forceOffline");
        }

        String userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("isOnline", false);
        userData.put("lastSeen", Timestamp.now());

        try {
            // Synchronous write with timeout
            Tasks.await(db.runTransaction(transaction -> {
                transaction.set(db.collection("Users").document(userId), userData, SetOptions.merge());
                return null;
            }), WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            Log.d(TAG, "Force offline successful for user: " + userId + ", lastSeen: " + Timestamp.now().toDate() + ", timestamp: " + System.nanoTime());
        } catch (Exception e) {
            Log.e(TAG, "Failed to force offline for " + userId + ": ", e);
            // Fallback to asynchronous retry
            db.runTransaction(transaction -> {
                        transaction.set(db.collection("Users").document(userId), userData, SetOptions.merge());
                        return null;
                    }).addOnSuccessListener(aVoid -> Log.d(TAG, "Force offline retry successful for user: " + userId))
                    .addOnFailureListener(ex -> Log.e(TAG, "Force offline retry failed for " + userId + ": ", ex));
        }
    }
}