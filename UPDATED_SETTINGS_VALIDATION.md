# Removed Reporting Demo & Improved Number Validation

## Change Summary
1.  **Removed "Send Report Demo"**: Deleted the button and associated logic from `SettingsFragment`.
2.  **Improved Phone Validation**:
    *   Strips spaces and hyphens.
    *   Adds `+91` prefix automatically if the user enters exactly 10 digits.
    *   Validates invalid formats (e.g., < 11 chars or missing +).

## Rationale
The demo feature was for testing. The validation prevents SMS failures due to incorrect formats.

## Build Status
✅ **BUILD SUCCESSFUL**
