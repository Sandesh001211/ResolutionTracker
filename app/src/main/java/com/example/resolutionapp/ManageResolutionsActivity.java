package com.example.resolutionapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.resolutionapp.data.FirestoreHelper;
import com.example.resolutionapp.model.Habit;
import java.util.ArrayList;
import java.util.UUID;

public class ManageResolutionsActivity extends AppCompatActivity {

    private FirestoreHelper firestoreHelper;
    private HabitAdapter adapter;
    private EditText etHabitTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_resolutions);

        firestoreHelper = new FirestoreHelper();
        etHabitTitle = findViewById(R.id.etHabitTitle);
        Button btnAdd = findViewById(R.id.btnAddHabit);
        RecyclerView rvHabits = findViewById(R.id.rvHabits);

        adapter = new HabitAdapter(new ArrayList<>(), this::deleteHabit);
        rvHabits.setLayoutManager(new LinearLayoutManager(this));
        rvHabits.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> addHabit());

        loadHabits();
    }

    private void loadHabits() {
        firestoreHelper.getHabits(habits -> adapter.updateList(habits));
    }

    private void addHabit() {
        String title = etHabitTitle.getText().toString().trim();
        if (title.isEmpty())
            return;

        String id = UUID.randomUUID().toString();
        Habit habit = new Habit(id, title, "", System.currentTimeMillis());

        android.widget.CheckBox cbSunday = findViewById(R.id.cbSundayOnly);
        if (cbSunday.isChecked()) {
            java.util.List<String> frequency = new java.util.ArrayList<>();
            frequency.add("SUNDAY");
            habit.setFrequency(frequency);
        }

        firestoreHelper.addHabit(habit, task -> {
            if (task.isSuccessful()) {
                etHabitTitle.setText("");
                cbSunday.setChecked(false);
                loadHabits();
            } else {
                android.widget.Toast.makeText(this, "Failed to add habit", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteHabit(Habit habit) {
        firestoreHelper.deleteHabit(habit.getId(), task -> {
            if (task.isSuccessful()) {
                loadHabits();
            } else {
                android.widget.Toast.makeText(this, "Failed to delete habit", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
}
