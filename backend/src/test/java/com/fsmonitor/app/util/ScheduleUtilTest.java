package com.fsmonitor.app.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleUtilTest {

    private static LocalDateTime at(String iso) {
        return LocalDateTime.parse(iso); // e.g. 2026-09-28 is a Monday
    }

    @Test
    void noScheduleIsAlwaysActive() {
        assertTrue(ScheduleUtil.isWithinSchedule(null, null, null, at("2026-09-28T03:00:00")));
        assertTrue(ScheduleUtil.isWithinSchedule("", null, null, at("2026-09-28T03:00:00")));
        // Missing start OR end hour means no hour restriction
        assertTrue(ScheduleUtil.isWithinSchedule(null, 9, null, at("2026-09-28T03:00:00")));
        assertTrue(ScheduleUtil.isWithinSchedule(null, null, 17, at("2026-09-28T03:00:00")));
    }

    @Test
    void dayMatchingIsExactNotSubstring() {
        // Monday 2026-09-28
        assertTrue(ScheduleUtil.isWithinSchedule("MON,TUE", null, null, at("2026-09-28T12:00:00")));
        assertTrue(ScheduleUtil.isWithinSchedule("fri, mon", null, null, at("2026-09-28T12:00:00")));
        assertFalse(ScheduleUtil.isWithinSchedule("TUE,WED", null, null, at("2026-09-28T12:00:00")));
        // "MONDAY" must not sneak-match a different day via substring
        assertFalse(ScheduleUtil.isWithinSchedule("SAT", null, null, at("2026-09-28T12:00:00")));
    }

    @Test
    void hourWindowIsStartInclusiveEndExclusive() {
        assertTrue(ScheduleUtil.isWithinSchedule(null, 9, 17, at("2026-09-28T09:00:00")));
        assertTrue(ScheduleUtil.isWithinSchedule(null, 9, 17, at("2026-09-28T16:59:00")));
        assertFalse(ScheduleUtil.isWithinSchedule(null, 9, 17, at("2026-09-28T17:00:00")));
        assertFalse(ScheduleUtil.isWithinSchedule(null, 9, 17, at("2026-09-28T08:59:00")));
    }

    @Test
    void overnightWindowWrapsMidnight() {
        // 22 -> 06 means 22:00-23:59 OR 00:00-05:59
        assertTrue(ScheduleUtil.isWithinSchedule(null, 22, 6, at("2026-09-28T23:30:00")));
        assertTrue(ScheduleUtil.isWithinSchedule(null, 22, 6, at("2026-09-29T02:15:00")));
        assertFalse(ScheduleUtil.isWithinSchedule(null, 22, 6, at("2026-09-28T12:00:00")));
        assertFalse(ScheduleUtil.isWithinSchedule(null, 22, 6, at("2026-09-29T06:00:00")));
        assertTrue(ScheduleUtil.isWithinSchedule(null, 22, 6, at("2026-09-28T22:00:00")));
    }

    @Test
    void dayAndHourCombine() {
        assertTrue(ScheduleUtil.isWithinSchedule("MON", 8, 18, at("2026-09-28T10:00:00")));
        assertFalse(ScheduleUtil.isWithinSchedule("MON", 8, 18, at("2026-09-28T19:00:00")));
        assertFalse(ScheduleUtil.isWithinSchedule("TUE", 8, 18, at("2026-09-28T10:00:00")));
    }
}
