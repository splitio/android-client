package io.split.android.client.storage.db;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.utils.logger.Logger;

class SplitMapInitializationTask implements Runnable {
    
    private final SplitQueryDaoImpl mDao;
    private final Object mLock;
    
    public SplitMapInitializationTask(SplitQueryDaoImpl dao, Object lock) {
        mDao = dao;
        mLock = lock;
    }
    
    @Override
    public void run() {
        Map<String, SplitEntity> result = null;
        boolean success = false;
        
        try {
            result = mDao.loadSplitsMap();
            success = true;
        } catch (Exception e) {
            Logger.e("Failed to initialize splits map cache: " + e.getLocalizedMessage());
        } finally {
            synchronized (mLock) {
                try {
                    if (success) {
                        mDao.setCachedSplitsMap(result);
                    } else {
                        // If initialization fails, use empty map to prevent infinite waiting
                        mDao.setCachedSplitsMap(new HashMap<>());
                    }
                } catch (Exception e) {
                    Logger.e("Failed to set cached splits map: " + e.getLocalizedMessage());
                    // Even if setting the map fails, we should still mark as initialized
                    // to prevent infinite waiting
                }
                
                try {
                    mDao.setInitialized(true);
                } catch (Exception e) {
                    Logger.e("Failed to set initialization status: " + e.getLocalizedMessage());
                }
                
                mLock.notifyAll();
            }
        }
    }
}
