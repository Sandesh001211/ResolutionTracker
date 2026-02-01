# Fix: Inconsistent Resolution Display on Past Dates

## Problem Description
Resolutions were showing inconsistently on past dates - sometimes appearing and sometimes not appearing. This created confusion and unreliable tracking.

## Root Causes Identified

### 1. **Default Habit with Dynamic Timestamp**
The "Weekly Cleaning" default habit was getting a new timestamp (`System.currentTimeMillis()`) every time the `DailyResolutionsActivity` loaded. This meant:
- If you viewed a past date, the default habit would have TODAY's timestamp
- The comparison logic would see: "This habit was created TODAY, so don't show it on YESTERDAY"
- Next time you load the same past date, it gets a NEW timestamp again
- Result: **Inconsistent behavior** - appearing/disappearing randomly

### 2. **Old Habits Without Timestamps**
Habits created before the timestamp feature was implemented might have:
- `createdTimestamp = 0` (default long value)
- `createdTimestamp = null` in Firestore

When `createdTimestamp = 0`, it represents January 1, 1970 (Unix epoch), which means:
- The habit would appear on ALL dates (since all dates are after 1970)
- But if the field was null or missing, behavior was unpredictable

### 3. **No Fallback Handling**
The code didn't handle missing or invalid timestamps gracefully, leading to:
- Inconsistent filtering logic
- Unpredictable behavior for old data

## Solutions Implemented

### Fix 1: Fixed Timestamp for Default Habits
**File**: `DailyResolutionsActivity.java` (lines 56-80)

**Before**:
```java
cleaning.createdTimestamp = System.currentTimeMillis(); // Changes every time!
```

**After**:
```java
// Use a fixed historical date (Jan 1, 2024)
Calendar fixedDate = Calendar.getInstance();
fixedDate.set(2024, Calendar.JANUARY, 1, 0, 0, 0);
fixedDate.set(Calendar.MILLISECOND, 0);
cleaning.createdTimestamp = fixedDate.getTimeInMillis();
```

**Impact**: Default habits now have a consistent creation date and will appear reliably on all dates from Jan 1, 2024 onwards.

### Fix 2: Validate Timestamp Before Filtering
**File**: `DailyResolutionsActivity.java` (lines 103-122)

**Before**:
```java
Calendar createdCal = Calendar.getInstance();
createdCal.setTimeInMillis(habit.createdTimestamp); // Could be 0!
// ... comparison logic
```

**After**:
```java
// Only filter if we have a valid timestamp
if (habit.createdTimestamp > 0) {
    Calendar createdCal = Calendar.getInstance();
    createdCal.setTimeInMillis(habit.createdTimestamp);
    // ... comparison logic
}
// If timestamp is 0 or invalid, show on all dates (backward compatibility)
```

**Impact**: Habits without valid timestamps will appear on ALL dates, maintaining backward compatibility with old data.

### Fix 3: Default Timestamp in Firestore Helper
**File**: `FirestoreHelper.java` (lines 75-84)

**Before**:
```java
if (ts != null) {
    h.createdTimestamp = ts;
}
// If null, createdTimestamp remains 0
```

**After**:
```java
if (ts != null && ts > 0) {
    h.createdTimestamp = ts;
} else {
    // For old habits without timestamp, use default historical date
    Calendar defaultDate = Calendar.getInstance();
    defaultDate.set(2024, Calendar.JANUARY, 1, 0, 0, 0);
    defaultDate.set(Calendar.MILLISECOND, 0);
    h.createdTimestamp = defaultDate.getTimeInMillis();
}
```

**Impact**: Old habits loaded from Firestore get a consistent default timestamp (Jan 1, 2024), ensuring they appear on all dates reliably.

## How It Works Now

### Scenario 1: New Resolution Created Today (Feb 1, 2026)
- **Creation Date**: February 1, 2026, 13:26:00
- **Normalized Date**: February 1, 2026, 00:00:00
- **Viewing Jan 31, 2026**: ❌ Won't appear (date is before creation)
- **Viewing Feb 1, 2026**: ✅ Will appear (date matches creation)
- **Viewing Feb 2, 2026**: ✅ Will appear (date is after creation)

### Scenario 2: Old Resolution Without Timestamp
- **Creation Date**: Missing/null in Firestore
- **Default Applied**: January 1, 2024, 00:00:00
- **Viewing any date in 2024**: ✅ Will appear (backward compatibility)
- **Viewing any date in 2025**: ✅ Will appear
- **Viewing any date in 2026**: ✅ Will appear

### Scenario 3: Default "Weekly Cleaning" Habit
- **Creation Date**: Fixed at January 1, 2024, 00:00:00
- **Viewing Jan 1, 2024**: ✅ Will appear (if it's Sunday)
- **Viewing Dec 31, 2023**: ❌ Won't appear (before creation)
- **Viewing any Sunday in 2025**: ✅ Will appear (frequency filter)
- **Viewing any Monday in 2025**: ❌ Won't appear (frequency filter)
- **Consistent**: ✅ Same behavior every time you view the same date

## Testing Verification

### Test 1: Consistency Check
1. Navigate to a past date (e.g., Jan 15, 2026)
2. Note which resolutions appear
3. Go back to home, then navigate to the same date again
4. **Expected**: Same resolutions should appear
5. **Result**: ✅ Consistent behavior

### Test 2: New Resolution
1. Create a new resolution today (Feb 1, 2026)
2. Navigate to yesterday (Jan 31, 2026)
3. **Expected**: New resolution should NOT appear
4. Navigate to today (Feb 1, 2026)
5. **Expected**: New resolution SHOULD appear
6. **Result**: ✅ Correct filtering

### Test 3: Default Habit
1. Navigate to any Sunday in January 2024
2. **Expected**: "Weekly Cleaning" should appear
3. Navigate to any Sunday in December 2023
4. **Expected**: "Weekly Cleaning" should NOT appear
5. **Result**: ✅ Fixed timestamp working

## Technical Details

### Date Normalization
All dates are normalized to midnight (00:00:00.000) to ensure:
- Only the date matters, not the time
- Consistent comparisons across different times of day
- A resolution created at 2:00 PM appears for the entire day

### Backward Compatibility
The fix maintains backward compatibility by:
- Assigning a default timestamp (Jan 1, 2024) to old habits
- Showing habits with invalid timestamps on all dates
- Not requiring database migration or data updates

### Default Date Choice
January 1, 2024 was chosen because:
- It's in the past (before current date)
- It's recent enough to be relevant
- It ensures old habits appear on most dates users care about
- It's a clean, memorable date

## Files Modified

1. **DailyResolutionsActivity.java**
   - Fixed default habit timestamp (lines 56-80)
   - Added timestamp validation (lines 103-122)

2. **FirestoreHelper.java**
   - Added default timestamp for old habits (lines 75-84)

## Build Status
✅ **BUILD SUCCESSFUL** - All changes compile without errors

## Summary
The inconsistent behavior was caused by dynamic timestamps for default habits and missing timestamps for old habits. The fix ensures:
- ✅ Default habits have a fixed, consistent creation date
- ✅ Old habits without timestamps get a default date
- ✅ All resolutions display consistently on past dates
- ✅ Backward compatibility with existing data
- ✅ No database migration required
