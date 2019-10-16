package io.split.android.client.validators;

class EventTypeNameHelper {

    public String getValidAllValidChars() {
        return "Abcdefghij:klmnopkrstuvwxyz_-12345.6789:";
    }

    public String getValidStartNumber() {
        return "1Abcdefghijklmnopkrstuvwxyz_-12345.6789:";
    }

    public String getInvalidHypenStart() {
        return "-1Abcdefghijklmnopkrstuvwxyz_-123456789:";
    }

    public String getInvalidUndercoreStart() {
        return "_1Abcdefghijklmnopkrstuvwxyz_-123456789:";
    }

    public String getInvalidChars() {
        return "Abcd,;][}{efghijklmnopkrstuvwxyz_-123456789:";
    }
}
