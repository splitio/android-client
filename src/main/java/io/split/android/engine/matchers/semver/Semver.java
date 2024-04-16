package io.split.android.engine.matchers.semver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.utils.logger.Logger;

class Semver {

    private static final String METADATA_DELIMITER = "+";
    private static final String PRE_RELEASE_DELIMITER = "-";
    private static final String VALUE_DELIMITER = "\\.";

    private Long mMajor;
    private Long mMinor;
    private Long mPatch;
    private String[] mPreRelease = new String[]{};
    private boolean mIsStable;
    private String mMetadata;
    private final String mVersion;

    @Nullable
    static Semver build(String version) {
        try {
            return new Semver(version);
        } catch (Exception ex) {
            Logger.e("An error occurred during the creation of a Semver instance:", ex.getMessage());
            return null;
        }
    }

    private Semver(String version) throws SemverParseException {
        String vWithoutMetadata = setAndRemoveMetadataIfExists(version);
        String vWithoutPreRelease = setAndRemovePreReleaseIfExists(vWithoutMetadata);
        setMajorMinorAndPatch(vWithoutPreRelease);
        mVersion = setVersion();
    }

    /**
     * Precedence comparison between 2 Semver objects.
     *
     * @return the value {@code 0} if {@code this == toCompare};
     * a value less than {@code 0} if {@code this < toCompare}; and
     * a value greater than {@code 0} if {@code this > toCompare}
     */
    public int compare(Semver toCompare) {
        if (mVersion.equals(toCompare.getVersion())) {
            return 0;
        }

        // Compare major, minor, and patch versions numerically
        int result = Long.compare(mMajor, toCompare.mMajor);
        if (result != 0) {
            return result;
        }

        result = Long.compare(mMinor, toCompare.mMinor);
        if (result != 0) {
            return result;
        }

        result = Long.compare(mPatch, toCompare.mPatch);
        if (result != 0) {
            return result;
        }

        if (!mIsStable && toCompare.mIsStable) {
            return -1;
        } else if (mIsStable && !toCompare.mIsStable) {
            return 1;
        }

        // Compare pre-release versions lexically
        int minLength = Math.min(mPreRelease.length, toCompare.mPreRelease.length);
        for (int i = 0; i < minLength; i++) {
            if (mPreRelease[i].equals(toCompare.mPreRelease[i])) {
                continue;
            }

            if (isNumeric(mPreRelease[i]) && isNumeric(toCompare.mPreRelease[i])) {
                return Integer.compare(Integer.parseInt(mPreRelease[i]), Integer.parseInt(toCompare.mPreRelease[i]));
            }

            return mPreRelease[i].compareTo(toCompare.mPreRelease[i]);
        }

        // Compare lengths of pre-release versions
        return Integer.compare(mPreRelease.length, toCompare.mPreRelease.length);
    }

    @NonNull
    public String getVersion() {
        return mVersion;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Semver)) {
            return false;
        }
        return mVersion.equals(((Semver) obj).getVersion());
    }

    private String setAndRemoveMetadataIfExists(String version) throws SemverParseException {
        int index = version.indexOf(METADATA_DELIMITER);
        if (index == -1) {
            return version;
        }

        mMetadata = version.substring(index + 1);
        if (mMetadata == null || mMetadata.isEmpty()) {
            throw new SemverParseException("Unable to convert to Semver, incorrect metadata");
        }

        return version.substring(0, index);
    }

    private String setAndRemovePreReleaseIfExists(String vWithoutMetadata) throws SemverParseException {
        int index = vWithoutMetadata.indexOf(PRE_RELEASE_DELIMITER);
        if (index == -1) {
            mIsStable = true;
            return vWithoutMetadata;
        }
        String preReleaseData = vWithoutMetadata.substring(index + 1);
        mPreRelease = preReleaseData.split(VALUE_DELIMITER);

        if (mPreRelease == null || containsNullOrEmpty(mPreRelease)) {
            throw new SemverParseException("Unable to convert to Semver, incorrect pre release data");
        }
        return vWithoutMetadata.substring(0, index);
    }

    private static boolean containsNullOrEmpty(String[] preRelease) {
        for (String pr : preRelease) {
            if (pr == null || pr.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void setMajorMinorAndPatch(String version) throws SemverParseException {
        String[] vParts = version.split(VALUE_DELIMITER);
        if (vParts.length != 3) {
            Logger.e("Unable to convert to Semver, incorrect format: " + version);
            throw new SemverParseException("Unable to convert to Semver, incorrect format: " + version);
        }
        mMajor = Long.parseLong(vParts[0]);
        mMinor = Long.parseLong(vParts[1]);
        mPatch = Long.parseLong(vParts[2]);
    }

    @NonNull
    private String setVersion() {
        String toReturn = mMajor + VALUE_DELIMITER + mMinor + VALUE_DELIMITER + mPatch;
        if (mPreRelease != null && mPreRelease.length != 0) {
            toReturn = toReturn + PRE_RELEASE_DELIMITER + String.join(VALUE_DELIMITER, mPreRelease);
        }

        if (mMetadata != null && !mMetadata.isEmpty()) {
            toReturn = toReturn + METADATA_DELIMITER + mMetadata;
        }

        return toReturn;
    }

    private static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }

        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
