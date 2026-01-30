package com.example.resolutionapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.List;

public class IntroActivity extends AppCompatActivity {

    private ViewPager2 viewPager2;
    private IntroAdapter introAdapter;
    private Button btnNext;
    private TextView btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if intro already seen
        if (getSharedPreferences("PREFS", MODE_PRIVATE).getBoolean("INTRO_SEEN", false)) {
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_intro);

        viewPager2 = findViewById(R.id.viewPager2);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);

        List<IntroSlide> slides = new ArrayList<>();
        slides.add(new IntroSlide(
                R.drawable.intro_resolutions,
                "Resolution Tracker",
                "Your companion for building consistent habits and tracking daily goals."));
        slides.add(new IntroSlide(
                R.drawable.intro_history,
                "Check Past Progress",
                "Use the calendar to view and track your resolutions for any previous date."));
        slides.add(new IntroSlide(
                R.drawable.intro_sunday,
                "Flexible Scheduling",
                "Set daily resolutions or choose specific days, including special habits for Sundays."));
        slides.add(new IntroSlide(
                R.drawable.intro_stats,
                "Detailed Statistics",
                "Visualize your improvement over time with comprehensive progress charts."));
        slides.add(new IntroSlide(
                R.drawable.intro_buttons,
                "App Overview",
                "Use '+' to manage habits, Calendar for history, Chart for stats, and Gear for settings."));
        introAdapter = new IntroAdapter(slides);
        viewPager2.setAdapter(introAdapter);

        // Link TabLayout with ViewPager2
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {
            // Dots are handled by background selector
        }).attach();

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == slides.size() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager2.getCurrentItem() + 1 < slides.size()) {
                viewPager2.setCurrentItem(viewPager2.getCurrentItem() + 1);
            } else {
                markIntroSeen();
                startMainActivity();
            }
        });

        btnSkip.setOnClickListener(v -> {
            markIntroSeen();
            startMainActivity();
        });
    }

    private void markIntroSeen() {
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putBoolean("INTRO_SEEN", true);
        editor.apply();
    }

    private void startMainActivity() {
        startActivity(new Intent(IntroActivity.this, MainActivity.class));
        finish();
    }
}
