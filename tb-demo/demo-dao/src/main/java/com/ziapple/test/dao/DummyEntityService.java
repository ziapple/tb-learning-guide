package com.ziapple.test.dao;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ziapple.test.dao.data.TsKvEntity;

import java.util.List;

/**
 * 模拟entityService
 */
public class DummyEntityService {
    private DummyEntityDao<TsKvEntity> dummyEntityDao;

    public DummyEntityService(DummyEntityDao dummyEntityDao){
        this.dummyEntityDao = dummyEntityDao;
    }

    public ListenableFuture<List<Void>> save(List<TsKvEntity> tsKvEntries){
        List<ListenableFuture<Void>> futures = Lists.newArrayListWithExpectedSize(tsKvEntries.size());
        for (TsKvEntity tsKvEntity : tsKvEntries) {
            futures.add(dummyEntityDao.save(tsKvEntity));
        }
        return Futures.allAsList(futures);
    }
}
