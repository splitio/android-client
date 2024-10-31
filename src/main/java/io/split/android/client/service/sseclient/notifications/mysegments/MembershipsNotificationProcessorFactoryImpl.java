package io.split.android.client.service.sseclient.notifications.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;
import io.split.android.client.service.sseclient.notifications.memberships.MembershipsNotificationProcessor;
import io.split.android.client.service.sseclient.notifications.memberships.MembershipsNotificationProcessorImpl;

public class MembershipsNotificationProcessorFactoryImpl implements MembershipsNotificationProcessorFactory {

    private final NotificationParser mNotificationParser;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final MySegmentsV2PayloadDecoder mMySegmentsPayloadDecoder;
    private final CompressionUtilProvider mCompressionProvider;

    public MembershipsNotificationProcessorFactoryImpl(@NonNull NotificationParser notificationParser,
                                                       @NonNull SplitTaskExecutor splitTaskExecutor,
                                                       @NonNull MySegmentsV2PayloadDecoder mySegmentsPayloadDecoder,
                                                       @NonNull CompressionUtilProvider compressionProvider) {
        mNotificationParser = checkNotNull(notificationParser);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mMySegmentsPayloadDecoder = checkNotNull(mySegmentsPayloadDecoder);
        mCompressionProvider = checkNotNull(compressionProvider);
    }

    @Override
    public MembershipsNotificationProcessor getProcessor(MySegmentsNotificationProcessorConfiguration configuration) {
        return new MembershipsNotificationProcessorImpl(mNotificationParser, mSplitTaskExecutor, mMySegmentsPayloadDecoder, mCompressionProvider, configuration, new SyncDelayCalculatorImpl());
    }
}
