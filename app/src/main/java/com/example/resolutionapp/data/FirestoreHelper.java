package com.example.resolutionapp.data;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.resolutionapp.model.Habit;
import com.example.resolutionapp.model.ResolutionDay;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class FirestoreHelper {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // --- Habit Management ---

    public interface HabitCallback {
        void onCallback(List<Habit> habits);
    }

    public void addHabit(Habit habit, OnCompleteListener<Void> onCompleteListener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            auth.signInAnonymously().addOnSuccessListener(authResult -> addHabit(habit, onCompleteListener));
            return;
        }

        db.collection("users").document(user.getUid())
                .collection("habits").document(habit.getId())
                .set(habit)
                .addOnCompleteListener(onCompleteListener);
    }

    public void deleteHabit(String habitId, OnCompleteListener<Void> onCompleteListener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return;

        db.collection("users").document(user.getUid())
                .collection("habits").document(habitId)
                .delete()
                .addOnCompleteListener(onCompleteListener);
    }

    public void getHabits(HabitCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            signInAnonymouslyForHabits(callback);
            return;
        }

        db.collection("users").document(user.getUid())
                .collection("habits")
                .orderBy("createdTimestamp", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Habit> list = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            list.add(doc.toObject(Habit.class));
                        }
                        callback.onCallback(list);
                    } else {
                        callback.onCallback(new ArrayList<>());
                    }
                });
    }

    // --- Worker Helpers (Task-based for await) ---
    public Task<QuerySnapshot> getHabitsTask() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return null;
        return db.collection("users").document(user.getUid())
                .collection("habits")
                .get();
    }

    // --- Resolution Management ---

    public interface ResolutionCallback {
        void onCallback(List<String> completedHabitIds);
    }

    public void getResolutionsForDate(String date, ResolutionCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            signInAnonymouslyForResolutions(date, callback);
            return;
        }

        DocumentReference docRef = db.collection("users").document(user.getUid())
                .collection("resolutions").document(date);

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                // Check if document exists
                if (document != null && document.exists()) {
                    ResolutionDay day = document.toObject(ResolutionDay.class);
                    // Ensure we return a valid list (even if empty) to avoid nulls upstream
                    if (day != null && day.getCompletedHabitIds() != null) {
                        callback.onCallback(day.getCompletedHabitIds());
                    } else {
                        callback.onCallback(new ArrayList<>());
                    }
                } else {
                    callback.onCallback(new ArrayList<>());
                }
            } else {
                Log.e("Firestore", "Error getting documents: ", task.getException());
                callback.onCallback(new ArrayList<>());
            }
        });
    }

    public Task<DocumentSnapshot> getResolutionsForDateTask(String date) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return null;
        return db.collection("users").document(user.getUid())
                .collection("resolutions").document(date).get();
    }

    public void saveResolutions(String date, List<String> completedHabitIds) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            auth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    saveResolutions(date, completedHabitIds);
                }
            });
            return;
        }

        ResolutionDay day = new ResolutionDay(date, completedHabitIds);
        DocumentReference docRef = db.collection("users").document(user.getUid())
                .collection("resolutions").document(date);

        docRef.set(day).addOnFailureListener(e -> Log.e("Firestore", "Write failed", e));
    }

    public interface FirestoreCallbackAll {
        void onCallback(List<ResolutionDay> days);
    }

    public void getAllResolutions(final FirestoreCallbackAll callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onCallback(new ArrayList<>());
            return;
        }

        db.collection("users").document(user.getUid())
                .collection("resolutions")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<ResolutionDay> list = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            list.add(doc.toObject(ResolutionDay.class));
                        }
                        callback.onCallback(list);
                    } else {
                        callback.onCallback(new ArrayList<>());
                    }
                });
    }

    // --- Helpers ---

    private void signInAnonymouslyForHabits(HabitCallback callback) {
        auth.signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful())
                getHabits(callback);
            else
                callback.onCallback(new ArrayList<>());
        });
    }

    private void signInAnonymouslyForResolutions(String date, ResolutionCallback callback) {
        auth.signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful())
                getResolutionsForDate(date, callback);
            else
                callback.onCallback(new ArrayList<>());
        });
    }

    public void getResolutionsSince(String startDate, final FirestoreCallbackAll callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onCallback(new ArrayList<>());
            return;
        }

        db.collection("users").document(user.getUid())
                .collection("resolutions")
                .whereGreaterThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), startDate)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<ResolutionDay> list = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            list.add(doc.toObject(ResolutionDay.class));
                        }
                        callback.onCallback(list);
                    } else {
                        callback.onCallback(new ArrayList<>());
                    }
                });
    }
}
