package com.example.springboot.actuator.extend.domain;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "tbl_sec_log")
public class QueryEntity {
    @Id
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
