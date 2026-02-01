# Removed Default "Weekly Cleaning" Habit

## Change Summary
Removed the hardcoded "Weekly Cleaning" default habit injection. The app will now only show resolutions explicitly added by the user.

## Implementation Details

**File**: `DailyResolutionsActivity.java`

- Removed `injectDefaultHabits()` method call in `loadHabits()`.
- Removed `private void injectDefaultHabits()` method definition completely.

## Why?
To provide a cleaner slate for users and remove unwanted default data.

## Build Status
✅ **BUILD SUCCESSFUL**
