package at.jku.se.smarthome.service.rule;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Stateless parser and matcher for weekday prefixes used in time trigger conditions.
 *
 * <p>Recognised tokens (case-insensitive):</p>
 * <ul>
 *   <li>{@code Daily} — matches every day</li>
 *   <li>{@code Weekdays} — matches Monday through Friday</li>
 *   <li>{@code Weekends} — matches Saturday and Sunday</li>
 *   <li>{@code Mon}, {@code Tue}, …, {@code Sun} — single day</li>
 *   <li>{@code Mon,Wed,Fri} — comma-separated list</li>
 *   <li>{@code Mon-Fri} — hyphenated forward range</li>
 * </ul>
 */
public final class WeekdaySpec {

    /** Token for daily match. */
    private static final String TOKEN_DAILY = "daily";
    /** Token for weekdays match. */
    private static final String TOKEN_WEEKDAYS = "weekdays";
    /** Token for weekends match. */
    private static final String TOKEN_WEEKENDS = "weekends";
    /** Separator for day lists. */
    private static final String LIST_SEPARATOR = ",";
    /** Separator for day ranges. */
    private static final String RANGE_SEPARATOR = "-";
    /** Expected number of parts in a range. */
    private static final int RANGE_PARTS = 2;

    /** Parsed set of days this spec matches. */
    private final Set<DayOfWeek> days;

    private WeekdaySpec(Set<DayOfWeek> days) {
        this.days = days;
    }

    /**
     * Parses a weekday specification token.
     *
     * @param token the token to parse; null or blank returns empty
     * @return the parsed spec, or empty if the token is unrecognised
     */
    public static Optional<WeekdaySpec> parse(String token) {
        Optional<WeekdaySpec> result = Optional.empty();
        if (token != null && !token.isBlank()) {
            Set<DayOfWeek> parsed = parseToken(token.trim().toLowerCase());
            if (parsed != null) {
                result = Optional.of(new WeekdaySpec(parsed));
            }
        }
        return result;
    }

    /**
     * Checks whether the given day of week is included in this spec.
     *
     * @param day the day to check
     * @return true when the day matches
     */
    public boolean matches(DayOfWeek day) {
        return days.contains(day);
    }

    private static Set<DayOfWeek> parseToken(String lower) {
        Set<DayOfWeek> result = null;
        if (TOKEN_DAILY.equals(lower)) {
            result = EnumSet.allOf(DayOfWeek.class);
        } else if (TOKEN_WEEKDAYS.equals(lower)) {
            result = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        } else if (TOKEN_WEEKENDS.equals(lower)) {
            result = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        } else if (lower.contains(LIST_SEPARATOR)) {
            result = parseList(lower);
        } else if (lower.contains(RANGE_SEPARATOR)) {
            result = parseRange(lower);
        } else {
            DayOfWeek single = parseDay(lower);
            if (single != null) {
                result = EnumSet.of(single);
            }
        }
        return result;
    }

    private static Set<DayOfWeek> parseList(String list) {
        Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        String[] parts = list.split(LIST_SEPARATOR);
        boolean valid = true;
        for (String part : parts) {
            if (part.isBlank()) {
                valid = false;
                break;
            }
            DayOfWeek day = parseDay(part.trim());
            if (day == null) {
                valid = false;
                break;
            }
            result.add(day);
        }
        return valid && !result.isEmpty() ? result : null;
    }

    private static Set<DayOfWeek> parseRange(String range) {
        Set<DayOfWeek> result = null;
        String[] parts = range.split(RANGE_SEPARATOR);
        if (parts.length == RANGE_PARTS) {
            DayOfWeek start = parseDay(parts[0].trim());
            DayOfWeek end = parseDay(parts[1].trim());
            if (start != null && end != null && end.getValue() >= start.getValue()) {
                result = EnumSet.range(start, end);
            }
        }
        return result;
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.AvoidLiteralsInIfCondition"})
    private static DayOfWeek parseDay(String day) {
        DayOfWeek result = null;
        String lower = day.toLowerCase();
        if ("mon".equals(lower) || "monday".equals(lower)) {
            result = DayOfWeek.MONDAY;
        } else if ("tue".equals(lower) || "tuesday".equals(lower)) {
            result = DayOfWeek.TUESDAY;
        } else if ("wed".equals(lower) || "wednesday".equals(lower)) {
            result = DayOfWeek.WEDNESDAY;
        } else if ("thu".equals(lower) || "thursday".equals(lower)) {
            result = DayOfWeek.THURSDAY;
        } else if ("fri".equals(lower) || "friday".equals(lower)) {
            result = DayOfWeek.FRIDAY;
        } else if ("sat".equals(lower) || "saturday".equals(lower)) {
            result = DayOfWeek.SATURDAY;
        } else if ("sun".equals(lower) || "sunday".equals(lower)) {
            result = DayOfWeek.SUNDAY;
        }
        return result;
    }
}
