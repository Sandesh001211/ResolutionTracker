# Resolution Tracker
## Project Overview
Resolution Tracker is a mission-driven Android habit tracker designed to help users build long-term consistency through structured daily goals, visual progress tracking, and SMS-based accountability.

## Technical Details
- **Platform**: Native Android (Java)
- **Database**: Firebase Firestore (Real-time synchronization)
- **Authentication**: Firebase Anonymous Authentication (Frictionless onboarding)
- **Messaging**: Android SmsManager (Automated daily reporting)
- **Background Tasks**: WorkManager API (Reliable daily reminders)
- **UI/UX**: Material Design, Custom Particle Animations (Bokeh View).

## Key Features & Implementation
### 1. Automated Accountability Reports
Implemented a background reporting system that uses `SmsManager` to automatically send the user's daily progress to a designated recipient (like a parent or coach) at 11:56 PM, fostering external accountability.

### 2. Custom Progress Visualization
Developed a **GitHub-style Heatmap** component from scratch. Use `GridLayout` and custom color-mapping logic to visualize habit completion density over time, providing users with immediate visual feedback on their streaks.

### 3. Reliable Notifications
Implemented a multi-tier notification system using **WorkManager**. Configured `PeriodicWorkRequest` to handle daily completion checks and intermittent reminders, ensuring high reliability even if the app is closed or the device is rebooted.

### 4. High-Performance UI Animations
Authored a custom **Bokeh Particle System** (`BokehView`) using the Android Canvas API. This provides a "Wow" moment for users upon completing all daily tasks, enhancing the emotional feedback loop of habit formation.

## Challenges Overcome
- **Concurrency**: Managed parallel data fetching from Firestore using `AtomicInteger` and `AtomicReference` to ensure UI updates only after all necessary data (habits + resolutions) was retrieved.
- **Resource Management**: Optimized drawing logic in the custom particle view to maintain 60FPS while animating over 80 dynamic elements.
- **Data Modeling**: Designed a flexible Firestore schema to support date-based resolution tracking and habit management.

## Future Roadmap
- Integration with Android Health Connect for automated habit tracking.
- Social features for "Habit Squads" and collaborative challenges.
- Advanced analytics for habit-success correlation.
