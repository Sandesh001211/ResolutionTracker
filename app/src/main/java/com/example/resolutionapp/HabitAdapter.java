package com.example.resolutionapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.resolutionapp.model.Habit;
import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private List<Habit> habits;
    private final OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Habit habit);
    }

    public HabitAdapter(List<Habit> habits, OnDeleteClickListener deleteListener) {
        this.habits = habits;
        this.deleteListener = deleteListener;
    }

    public void updateList(List<Habit> newHabits) {
        this.habits = newHabits;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habits.get(position);
        holder.tvHabitName.setText(habit.getTitle());
        holder.btnDelete.setOnClickListener(v -> deleteListener.onDeleteClick(habit));
    }

    @Override
    public int getItemCount() {
        return habits != null ? habits.size() : 0;
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView tvHabitName;
        ImageButton btnDelete;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHabitName = itemView.findViewById(R.id.tvHabitName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
