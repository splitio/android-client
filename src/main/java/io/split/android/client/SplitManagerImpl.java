package io.split.android.client;

import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.ValidationErrorInfo;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.ParsedCondition;
import io.split.android.engine.experiments.ParsedSplit;
import io.split.android.engine.experiments.SplitParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitManagerImpl implements SplitManager {

    private final SplitsStorage _splitsStorage;
    private boolean _isManagerDestroyed = false;
    private final SplitValidator _splitValidator;
    private final ValidationMessageLogger _validationMessageLogger;
    private final SplitParser _splitParser;

    public SplitManagerImpl(SplitsStorage splitsStorage,
                            SplitValidator splitValidator,
                            SplitParser splitParser) {

        _validationMessageLogger = new ValidationMessageLoggerImpl();
        _splitsStorage = checkNotNull(splitsStorage);
        _splitValidator = checkNotNull(splitValidator);
        _splitParser = checkNotNull(splitParser);
    }

    @Override
    public List<SplitView> splits() {
        List<SplitView> result = new ArrayList<>();

        try {
            if (_isManagerDestroyed) {
                Logger.e("Manager has already been destroyed - no calls possible");
                return result;
            }

            Map<String, Split> splitMap = _splitsStorage.getAll();
            if (splitMap != null && splitMap.size() > 0) {
                Collection<Split> splits = _splitsStorage.getAll().values();
                for (Split split : splits) {
                    ParsedSplit parsedSplit = _splitParser.parse(split);
                    if (parsedSplit != null) {
                        result.add(toSplitView(parsedSplit));
                    }
                }
            }
        } catch (Exception exception) {
            Logger.e("Error getting splits: " + exception.getLocalizedMessage());
        }

        return result;
    }

    @Override
    public SplitView split(String featureName) {

        final String validationTag = "split";
        String splitName = featureName;

        try {
            if (_isManagerDestroyed) {
                Logger.e("Manager has already been destroyed - no calls possible");
                return null;
            }

            ValidationErrorInfo errorInfo = _splitValidator.validateName(featureName);
            if (errorInfo != null) {
                _validationMessageLogger.log(errorInfo, validationTag);
                if (errorInfo.isError()) {
                    return null;
                }
                splitName = featureName.trim();
            }

            ParsedSplit parsedSplit = null;
            Split split = _splitsStorage.get(splitName);
            if (split != null) {
                parsedSplit = _splitParser.parse(split);
            }
            if (parsedSplit == null) {
                _validationMessageLogger.w(_splitValidator.splitNotFoundMessage(splitName), validationTag);
                return null;
            }
            return toSplitView(parsedSplit);
        } catch (Exception exception) {
            Logger.e("Error getting split: " + exception.getLocalizedMessage());

            return null;
        }
    }

    @Override
    public List<String> splitNames() {
        List<String> result = new ArrayList<>();

        try {
            if (_isManagerDestroyed) {
                Logger.e("Manager has already been destroyed - no calls possible");
                return result;
            }

            Map<String, Split> splitMap = _splitsStorage.getAll();
            if (splitMap != null && splitMap.size() > 0) {
                for (Split split : splitMap.values()) {
                    result.add(split.name);
                }
            }

            return result;
        } catch (Exception exception) {
            Logger.e("Error getting split names: " + exception.getLocalizedMessage());

            return result;
        }
    }

    @Override
    public void destroy() {
        _isManagerDestroyed = true;
    }

    private SplitView toSplitView(ParsedSplit parsedSplit) {
        SplitView splitView = new SplitView();
        splitView.name = parsedSplit.feature();
        splitView.trafficType = parsedSplit.trafficTypeName();
        splitView.killed = parsedSplit.killed();
        splitView.changeNumber = parsedSplit.changeNumber();
        splitView.configs = parsedSplit.configurations();

        Set<String> treatments = new HashSet<>();
        for (ParsedCondition condition : parsedSplit.parsedConditions()) {
            for (Partition partition : condition.partitions()) {
                treatments.add(partition.treatment);
            }
        }
        treatments.add(parsedSplit.defaultTreatment());
        splitView.treatments = new ArrayList<>(treatments);

        return splitView;
    }
}
