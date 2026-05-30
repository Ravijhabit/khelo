package com.ghostcoach.validation;

import com.ghostcoach.model.ExperienceLevel;
import com.ghostcoach.model.Sport;
import jakarta.validation.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;

class EnumValidatorTest {

    private EnumValidator sportValidator;
    private EnumValidator expValidator;

    @BeforeEach
    void setUp() {
        sportValidator = new EnumValidator();
        sportValidator.initialize(annotation(Sport.class, true));

        expValidator = new EnumValidator();
        expValidator.initialize(annotation(ExperienceLevel.class, true));
    }

    // ── Sport ──────────────────────────────────────────────────────────────────

    @Test
    void isValid_cricket_returnsTrue() {
        assertThat(sportValidator.isValid("Cricket", null)).isTrue();
    }

    @Test
    void isValid_cricketUppercase_returnsTrue() {
        assertThat(sportValidator.isValid("CRICKET", null)).isTrue();
    }

    @Test
    void isValid_football_returnsTrue() {
        assertThat(sportValidator.isValid("football", null)).isTrue();
    }

    @Test
    void isValid_allSupportedSports_returnTrue() {
        for (String sport : new String[]{"Cricket", "Football", "Basketball", "Badminton"}) {
            assertThat(sportValidator.isValid(sport, null))
                    .as("Expected %s to be valid", sport)
                    .isTrue();
        }
    }

    @Test
    void isValid_hockey_returnsFalse() {
        assertThat(sportValidator.isValid("Hockey", null)).isFalse();
    }

    @Test
    void isValid_null_returnsFalse() {
        assertThat(sportValidator.isValid(null, null)).isFalse();
    }

    @Test
    void isValid_emptyString_returnsFalse() {
        assertThat(sportValidator.isValid("", null)).isFalse();
    }

    // ── ExperienceLevel ────────────────────────────────────────────────────────

    @Test
    void isValid_beginner_returnsTrue() {
        assertThat(expValidator.isValid("Beginner", null)).isTrue();
    }

    @Test
    void isValid_intermediate_returnsTrue() {
        assertThat(expValidator.isValid("INTERMEDIATE", null)).isTrue();
    }

    @Test
    void isValid_expert_returnsFalse() {
        assertThat(expValidator.isValid("Expert", null)).isFalse();
    }

    // ── Case-sensitive mode ────────────────────────────────────────────────────

    @Test
    void isValid_caseSensitiveMode_requiresExactUppercase() {
        EnumValidator csValidator = new EnumValidator();
        csValidator.initialize(annotation(Sport.class, false));

        assertThat(csValidator.isValid("cricket", null)).isFalse();
        assertThat(csValidator.isValid("CRICKET", null)).isTrue();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static ValidEnum annotation(Class<? extends Enum<?>> enumClass, boolean ignoreCase) {
        return new ValidEnum() {
            @Override public Class<? extends Enum<?>> enumClass() { return enumClass; }
            @Override public boolean ignoreCase() { return ignoreCase; }
            @Override public String message() { return "invalid"; }
            @Override public Class<?>[] groups() { return new Class<?>[0]; }
            @Override @SuppressWarnings("unchecked")
            public Class<? extends Payload>[] payload() { return new Class[0]; }
            @Override public Class<? extends Annotation> annotationType() { return ValidEnum.class; }
        };
    }
}
