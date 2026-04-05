package com.fsmonitor.app.entity;

public enum TimeInterval {
    MINUTES("Minutes"),
    HOURS("Hours"),
    DAYS("Days"),
    WEEKS("Weeks"),
    MONTHS("Months");

    private final String displayName;

    TimeInterval(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long toMinutes(int value) {
        switch (this) {
            case MINUTES:
                return value;
            case HOURS:
                return value * 60L;
            case DAYS:
                return value * 24L * 60L;
            case WEEKS:
                return value * 7L * 24L * 60L;
            case MONTHS:
                return value * 30L * 24L * 60L; // Approximation
            default:
                return value;
        }
    }
}
