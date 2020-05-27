package com.ziapple.test.dao;

import com.ziapple.test.dao.data.TsKvEntity;

import java.util.List;
import java.util.Random;

/**
 * 模拟数据库客户端
 */
public class DummySqlClient {
    // 数据库最大批处理能力
    public static int max_tps = 10000;

    // 模拟批处理提交
    public void batch(List<TsKvEntity> entities){
        try {
            if (entities.size() < max_tps) {
                // 模拟处理时间 < 100 ms
                Thread.sleep(new Random().nextInt(100));
            } else {
                Thread.sleep(entities.size() * 1000 / max_tps);
            }
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
