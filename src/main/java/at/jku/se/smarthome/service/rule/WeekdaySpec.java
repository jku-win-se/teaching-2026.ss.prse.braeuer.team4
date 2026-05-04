package at.jku.se.smarthome.service.rule;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Optional;

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

    /** Parsed set of days this spec matches. */
    private final EnumSet<DayOfWeek> days;

    private WeekdaySpec(EnumSet<DayOfWeek> days) {
        this.days = days;
    }

    /**
     * Parses a weekday specification token.
     *
     * @param token the token to parse; null or blank returns empty
     * @return the parsed spec, or empty if the token is unrecognised
     */
    public static Optional<WeekdaySpec> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String normalised = token.trim();
        EnumSet<DayOfWeek> days = parseToken(normalised);
        if (days == null) {
            return Optional.empty();
        }
        return Optional.of(new WeekdaySpec(days));
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

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private static EnumSet<DayOfWeek> parseToken(String token) {
        String lower = token.toLowerCase();
        if ("daily".equals(lower)) {
            return EnumSet.allOf(DayOfWeek.class);
        }
        if ("weekdays".equals(lower)) {
            return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        }
        if ("weekends".equals(lower)) {
            return EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        }
        if (lower.contains(",")) {
            return parseList(lower);
        }
        if (lower.contains("-")) {
            return parseRange(lower);
        }
        DayOfWeek single = parseDay(lower);
        if (single != null) {
            return EnumSet.of(single);
        }
        return null;
    }

    private static EnumSet<DayOfWeek> parseList(String list) {
        EnumSet<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        String[] parts = list.split(",");
        for (String part : parts) {
            if (part.isBlank()) {
                return null;
            }
            DayOfWeek day = parseDay(part.trim());
            if (day == null) {
                return null;
            }
            result.add(day);
        }
        return result.isEmpty() ? null : result;
    }

    private static EnumSet<DayOfWeek> parseRange(String range) {
        String[] parts = range.split("-");
        if (parts.length != 2) {
            return null;
        }
        DayOfWeek start = parseDay(parts[0].trim());
        DayOfWeek end = parseDay(parts[1].trim());
        if (start == null || end == null || end.getValue() < start.getValue()) {
            return null;
        }
        return EnumSet.range(start, end);
    }

    private static DayOfWeek parseDay(String day) {
        return switch (day.toLowerCase()) {
            case "mon", "monday" -> DayOfWeek.MONDAY;
            case "tue", "tuesday" -> DayOfWeek.TUESDAY;
            case "wed", "wednesday" -> DayOfWeek.WEDNESDAY;
            case "thu", "thursday" -> DayOfWeek.THURSDAY;
            case "fri", "friday" -> DayOfWeek.FRIDAY;
            case "sat", "saturday" -> DayOfWeek.SATURDAY;
            case "sun", "sunday" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }
}
