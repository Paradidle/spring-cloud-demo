package com.example.spring.cloud.hystrix.annotation;

import java.lang.annotation.*;

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
 * CreateDate:2021/7/18
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/7/18；
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IHystrixCommand {

    /**
     * 超时时间
     * @return
     */
    long timeOut() default 1000;

    /**
     * 回退方法
     */
    String fallBack() default "";


}
