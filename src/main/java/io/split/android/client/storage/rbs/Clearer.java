package io.split.android.client.storage.rbs;

import io.split.android.client.storage.db.rbs.RuleBasedSegmentDao;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.utils.logger.Logger;

class Clearer implements Runnable {

    private final RuleBasedSegmentDao mDao;
    private final GeneralInfoStorage mGeneralInfoStorage;

    public Clearer(RuleBasedSegmentDao dao, GeneralInfoStorage generalInfoStorage) {
        mDao = dao;
        mGeneralInfoStorage = generalInfoStorage;
    }

    @Override
    public void run() {
        try {
            mDao.deleteAll();
            mGeneralInfoStorage.setRbsChangeNumber(-1);
        } catch (Exception e) {
            Logger.e("Error clearing RBS: " + e.getLocalizedMessage());
            throw e;
        }
    }
}
