package io.split.android.client.network;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Adapter that bridges the :http module's {@link TlsUpdater} interface with the
 * :main module's {@link LegacyTlsUpdater} class.
 */
public class LegacyTlsUpdaterAdapter implements TlsUpdater {

    @Nullable
    private final Context mContext;

    public LegacyTlsUpdaterAdapter(@Nullable Context context) {
        mContext = context;
    }

    @Override
    public boolean couldBeOld() {
        return LegacyTlsUpdater.couldBeOld();
    }

    @Override
    public void update() {
        LegacyTlsUpdater.update(mContext);
    }
}
