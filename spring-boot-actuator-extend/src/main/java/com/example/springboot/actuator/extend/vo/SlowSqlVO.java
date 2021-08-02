package com.example.springboot.actuator.extend.vo;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * Copyright: 2021 . All rights reserved.
 * </p>
 * <p>
 * Company: Zsoft
 * </p>
 * <p>
 * CreateDate:2021/8/1
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/8/1；
 */
public class SlowSqlVO {
    private String sqlText;
    private long time;
    private Object[] values;


    public SlowSqlVO() {
    }

    public SlowSqlVO(String sqlText, long time, Object[] values) {
        this.sqlText = sqlText;
        this.time = time;
        this.values = values;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Object[] getValues() {
        return values;
    }

    public void setValues(Object[] values) {
        this.values = values;
    }
}
