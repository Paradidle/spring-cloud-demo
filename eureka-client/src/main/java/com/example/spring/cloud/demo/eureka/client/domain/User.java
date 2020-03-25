package com.example.spring.cloud.demo.eureka.client.domain;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * Copyright: 2020 . All rights reserved.
 * </p>
 * <p>
 * Company: Zsoft
 * </p>
 * <p>
 * CreateDate:2020/1/25
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2020/1/25；
 */
public class User {
    private String name;
    private String age;
    private String id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User(String name, String age, String id) {
        this.name = name;
        this.age = age;
        this.id = id;
    }
}
