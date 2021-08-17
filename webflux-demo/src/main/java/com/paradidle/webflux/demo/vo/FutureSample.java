package main.java.com.paradidle.webflux.demo.vo;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
 * CreateDate:2021/8/2
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/8/2；
 */
public class FutureSample {

    private static List<String> tempList = new ArrayList<>();

    static {
        tempList.add("1");
        tempList.add("2");
        tempList.add("3");
        tempList.add("4");
        tempList.add("5");
        tempList.add("6");
    }


    public static void main(String[] args) {
        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(FutureSample::getTempList);
        CompletableFuture subFuture = future.thenApplyAsync(s-> CompletableFuture.supplyAsync(()-> s+"Str"));
        subFuture.
        try {
            System.out.println(future.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getTempList(){
        return tempList;
    }
}
