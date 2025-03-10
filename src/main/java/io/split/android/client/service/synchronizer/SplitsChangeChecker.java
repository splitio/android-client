package io.split.android.client.service.synchronizer;

public class SplitsChangeChecker {
    public boolean changeNumberIsNewer(long oldChangeNumber, long newChangeNumber) {
        return oldChangeNumber < newChangeNumber;
    }
}