package io.split.android.client.localhost;

import android.content.Context;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import io.split.android.client.dtos.Split;
import io.split.android.client.utils.Logger;

public class LocalhostPropertiesFileParser implements LocalhostFileParser {

    Context mContext;

    public LocalhostPropertiesFileParser(Context context) {
        mContext = context;
    }

    @Override
    public Map<String, Split> parse(String fileName) {

        Map<String, Split> splits = null;
        try {
            Properties _properties = new Properties();
            _properties.load(mContext.getAssets().open(fileName));
            splits = new HashMap<>();
            for (Object k: _properties.keySet()) {
                Split split = new Split();
                split.name = (String) k;
                split.defaultTreatment = _properties.getProperty((String) k);
                splits.put(split.name, split);
            }
        } catch (FileNotFoundException e) {
            Logger.e("Localhost property file not found. Add split.properties in your application assets");
        } catch (Exception e){
            Logger.e("Error loading localhost property file");
        }
        return splits;
    }
}
