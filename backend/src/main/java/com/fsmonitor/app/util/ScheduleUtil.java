package com.fsmonitor.app.util;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Shared schedule-window logic for monitoring services.
 * A schedule restricts checks to specific days of week and/or an hour range.
 */
public final class ScheduleUtil {

    private ScheduleUtil() {
    }

    /**
     * Returns true if {@code now} is inside the configured schedule.
     *
     * @param activeDays comma-separated 3-letter day names (e.g. "MON,TUE"), null/empty = every day
     * @param startHour  first active hour (inclusive), may be null
     * @param endHour    last active hour (exclusive), may be null
     */
    public static boolean isWithinSchedule(String activeDays, Integer startHour,
                                           Integer endHour, LocalDateTime now) {
        if (activeDays != null && !activeDays.isEmpty()) {
            String currentDay = now.getDayOfWeek().name().substring(0, 3);
            boolean dayMatches = Arrays.stream(activeDays.toUpperCase().split(","))
                    .map(String::trim)
                    .anyMatch(d -> d.equalsIgnoreCase(currentDay));
            if (!dayMatches) {
                return false;
            }
        }

        if (startHour != null && endHour != null) {
            int currentHour = now.getHour();
            boolean within;
            if (startHour <= endHour) {
                within = currentHour >= startHour && currentHour < endHour;
            } else {
                // Overnight window, e.g. 22-06: active 22:00-23:59 and 00:00-05:59
                within = currentHour >= startHour || currentHour < endHour;
            }
            if (!within) {
                return false;
            }
        }

        return true;
    }
}
