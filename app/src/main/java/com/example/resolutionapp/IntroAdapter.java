package com.example.resolutionapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class IntroAdapter extends RecyclerView.Adapter<IntroAdapter.IntroViewHolder> {

    private List<IntroSlide> slides;

    public IntroAdapter(List<IntroSlide> slides) {
        this.slides = slides;
    }

    @NonNull
    @Override
    public IntroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new IntroViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_intro_slide, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull IntroViewHolder holder, int position) {
        holder.bind(slides.get(position));
    }

    @Override
    public int getItemCount() {
        return slides.size();
    }

    static class IntroViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgSlide;
        private TextView txtTitle;
        private TextView txtDescription;

        public IntroViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSlide = itemView.findViewById(R.id.img_slide);
            txtTitle = itemView.findViewById(R.id.txt_title);
            txtDescription = itemView.findViewById(R.id.txt_description);
        }

        void bind(IntroSlide slide) {
            imgSlide.setImageResource(slide.getImage());
            txtTitle.setText(slide.getTitle());
            txtDescription.setText(slide.getDescription());
        }
    }
}
