package dociq_backend.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Profession {
    STUDENT,
    TEACHER,
    LAWYER,
    PERSON;

    @JsonCreator
    public static Profession fromString(String value) {
        return value == null
                ? PERSON
                : Profession.valueOf(value.toUpperCase());
    }
}