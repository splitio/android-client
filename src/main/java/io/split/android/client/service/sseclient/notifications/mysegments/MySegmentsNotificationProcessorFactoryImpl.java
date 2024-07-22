package io.split.android.client.service.sseclient.notifications.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.common.CompressionUtilProvider;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.sseclient.notifications.MySegmentsV2PayloadDecoder;
import io.split.android.client.service.sseclient.notifications.NotificationParser;

public class MySegmentsNotificationProcessorFactoryImpl implements MySegmentsNotificationProcessorFactory {

    private final NotificationParser mNotificationParser;
    private final SplitTaskExecutor mSplitTaskExecutor;
    private final MySegmentsV2PayloadDecoder mMySegmentsPayloadDecoder;
    private final CompressionUtilProvider mCompressionProvider;

    public MySegmentsNotificationProcessorFactoryImpl(@NonNull NotificationParser notificationParser,
                                                      @NonNull SplitTaskExecutor splitTaskExecutor,
                                                      @NonNull MySegmentsV2PayloadDecoder mySegmentsPayloadDecoder,
                                                      @NonNull CompressionUtilProvider compressionProvider) {
        mNotificationParser = checkNotNull(notificationParser);
        mSplitTaskExecutor = checkNotNull(splitTaskExecutor);
        mMySegmentsPayloadDecoder = checkNotNull(mySegmentsPayloadDecoder);
        mCompressionProvider = checkNotNull(compressionProvider);
    }

    @Override
    public MySegmentsNotificationProcessor getProcessor(@NonNull MySegmentsNotificationProcessorConfiguration configuration) {
        return new MySegmentsNotificationProcessorImpl(mNotificationParser,
                mSplitTaskExecutor,
                mMySegmentsPayloadDecoder,
                mCompressionProvider,
                checkNotNull(configuration));
    }

    @Override
    public MyLargeSegmentsNotificationProcessor getForLargeSegments(MySegmentsNotificationProcessorConfiguration configuration) {
        return new MyLargeSegmentsNotificationProcessorImpl(mNotificationParser,
                mSplitTaskExecutor,
                mMySegmentsPayloadDecoder,
                mCompressionProvider,
                checkNotNull(configuration));
    }
}
