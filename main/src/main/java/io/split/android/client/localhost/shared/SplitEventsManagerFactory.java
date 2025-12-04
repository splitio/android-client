package io.split.android.client.localhost.shared;

import io.split.android.client.events.SplitEventsManager;

/**
 * Factory interface for creating SplitEventsManager instances.
 * Package-local interface to allow testing by injecting mock implementations.
 */
interface SplitEventsManagerFactory {
    /**
     * Creates a new SplitEventsManager instance.
     *
     * @return a new SplitEventsManager instance
     */
    SplitEventsManager create();
}

