package com.ziapple.test.dao.data;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 模拟时序数据{key, value, ts}
 */
@Data
@Builder
public class TsKvEntity {
    private Integer id;
    private String key;
    private Object value;
    private Timestamp ts;

    public String toString(){
        return "{key:" + this.key + ", value:" + this.value + ", ts:" + ts.toLocalDateTime() + "}";
    }
}
