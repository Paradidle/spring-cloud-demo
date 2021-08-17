package main.java.com.paradidle.webflux.demo.vo;

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
 * CreateDate:2021/7/30
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/7/30；
 */
public class DCLDemo {
    static volatile DCLDemo instance;
    public static DCLDemo getInstance() {
        if(instance==null){
            synchronized (DCLDemo.class){
                instance = new DCLDemo();
            }
        }
        return instance;
    }
}
