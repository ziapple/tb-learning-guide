package com.ziapple.test.dao;

import com.google.common.util.concurrent.ListenableFuture;
import com.ziapple.test.dao.sql.TbSqlBlockingQueue;

/**
 * 模拟Dao层
 */
public class DummyEntityDao<T> {
    private TbSqlBlockingQueue<T> tbSqlBlockingQueue;

    public DummyEntityDao(TbSqlBlockingQueue tbSqlBlockingQueue){
        this.tbSqlBlockingQueue = tbSqlBlockingQueue;
    }

    // 保存实体,返回void
    public ListenableFuture<Void> save(T t){
        return this.tbSqlBlockingQueue.add(t);
    }
}
