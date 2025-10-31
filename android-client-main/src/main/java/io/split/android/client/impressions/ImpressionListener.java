package io.split.android.client.impressions;

import java.util.List;

/**
 * A listener for Impressions generated each time getTreatment is called.
 */
public interface ImpressionListener {

    /**
     * Log this impression to the listener. This method MUST NOT throw any exception
     *
     * @param impression
     */
    void log(Impression impression);

    /**
     * MUST NOT throw any exceptions
     */
    void close();

    final class NoopImpressionListener implements ImpressionListener, DecoratedImpressionListener {
        @Override
        public void log(Impression impression) {
            // noop
        }

        @Override
        public void log(DecoratedImpression impression) {

        }

        @Override
        public void close() {
            // noop
        }
    }

    final class FederatedImpressionListener implements ImpressionListener, DecoratedImpressionListener {
        private final DecoratedImpressionListener mDecoratedImpressionListener;
        private final List<ImpressionListener> _delegates;

        public FederatedImpressionListener(DecoratedImpressionListener decoratedImpressionListener, List<ImpressionListener> delegates) {
            mDecoratedImpressionListener = decoratedImpressionListener;
            _delegates = delegates;
        }

        @Override
        public void log(Impression impression) {
            for (ImpressionListener listener : _delegates) {
                listener.log(impression);
            }
        }

        @Override
        public void log(DecoratedImpression impression) {
            mDecoratedImpressionListener.log(impression);
        }

        @Override
        public void close() {
            for (ImpressionListener listener : _delegates) {
                listener.close();
            }
        }
    }
}
