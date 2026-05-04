package at.jku.se.smarthome.service.rule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.time.DayOfWeek;

/**
 * Unit tests for WeekdaySpec parser and matcher.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.CommentRequired",
        "PMD.MethodNamingConventions", "PMD.UnitTestContainsTooManyAsserts"})
public class WeekdaySpecTest {

    @Test
    public void parse_validTokens_returnsSpec() {
        assertTrue("Daily", WeekdaySpec.parse("Daily").isPresent());
        assertTrue("daily", WeekdaySpec.parse("daily").isPresent());
        assertTrue("Weekdays", WeekdaySpec.parse("Weekdays").isPresent());
        assertTrue("weekdays", WeekdaySpec.parse("weekdays").isPresent());
        assertTrue("Weekends", WeekdaySpec.parse("Weekends").isPresent());
        assertTrue("weekends", WeekdaySpec.parse("weekends").isPresent());
        assertTrue("Mon", WeekdaySpec.parse("Mon").isPresent());
        assertTrue("TUE", WeekdaySpec.parse("TUE").isPresent());
        assertTrue("wed", WeekdaySpec.parse("wed").isPresent());
        assertTrue("Thu", WeekdaySpec.parse("Thu").isPresent());
        assertTrue("fri", WeekdaySpec.parse("fri").isPresent());
        assertTrue("Sat", WeekdaySpec.parse("Sat").isPresent());
        assertTrue("sun", WeekdaySpec.parse("sun").isPresent());
        assertTrue("Mon,Wed,Fri", WeekdaySpec.parse("Mon,Wed,Fri").isPresent());
        assertTrue("mon,wed,fri", WeekdaySpec.parse("mon,wed,fri").isPresent());
        assertTrue("Mon-Fri", WeekdaySpec.parse("Mon-Fri").isPresent());
        assertTrue("mon-fri", WeekdaySpec.parse("mon-fri").isPresent());
    }

    @Test
    public void parse_invalidTokens_returnsEmpty() {
        assertFalse(WeekdaySpec.parse("Funday").isPresent());
        assertFalse(WeekdaySpec.parse("Mon-").isPresent());
        assertFalse(WeekdaySpec.parse("-Fri").isPresent());
        assertFalse(WeekdaySpec.parse("").isPresent());
        assertFalse(WeekdaySpec.parse("Mon,,Fri").isPresent());
    }

    @Test
    public void matches_dailyMatchesEveryDay() {
        WeekdaySpec spec = WeekdaySpec.parse("Daily").orElseThrow();
        for (DayOfWeek day : DayOfWeek.values()) {
            assertTrue(spec.matches(day));
        }
    }

    @Test
    public void matches_weekdaysMatchesMonToFri_butNotSatSun() {
        WeekdaySpec spec = WeekdaySpec.parse("Weekdays").orElseThrow();
        assertTrue(spec.matches(DayOfWeek.MONDAY));
        assertTrue(spec.matches(DayOfWeek.TUESDAY));
        assertTrue(spec.matches(DayOfWeek.WEDNESDAY));
        assertTrue(spec.matches(DayOfWeek.THURSDAY));
        assertTrue(spec.matches(DayOfWeek.FRIDAY));
        assertFalse(spec.matches(DayOfWeek.SATURDAY));
        assertFalse(spec.matches(DayOfWeek.SUNDAY));
    }

    @Test
    public void matches_weekendsMatchesSatSun_butNotMonToFri() {
        WeekdaySpec spec = WeekdaySpec.parse("Weekends").orElseThrow();
        assertTrue(spec.matches(DayOfWeek.SATURDAY));
        assertTrue(spec.matches(DayOfWeek.SUNDAY));
        assertFalse(spec.matches(DayOfWeek.MONDAY));
        assertFalse(spec.matches(DayOfWeek.TUESDAY));
        assertFalse(spec.matches(DayOfWeek.WEDNESDAY));
        assertFalse(spec.matches(DayOfWeek.THURSDAY));
        assertFalse(spec.matches(DayOfWeek.FRIDAY));
    }

    @Test
    public void parse_invertedRange_returnsEmpty() {
        assertFalse(WeekdaySpec.parse("Fri-Mon").isPresent());
    }
}
