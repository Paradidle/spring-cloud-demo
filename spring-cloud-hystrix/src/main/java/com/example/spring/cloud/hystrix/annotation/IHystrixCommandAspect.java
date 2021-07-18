package com.example.spring.cloud.hystrix.annotation;

import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;

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
@Component
@Aspect
public class IHystrixCommandAspect {

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Pointcut(value = "@annotation(IHystrixCommand)")
    public void pointCut(){}

    @Around(value = "pointCut()&&@annotation(iHystrixCommand)")
    public Object aroundPointCut(ProceedingJoinPoint joinPoint,IHystrixCommand iHystrixCommand) throws ExecutionException, InterruptedException, TimeoutException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        long l = iHystrixCommand.timeOut();
        Future future = executorService.submit(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            return null;
        });
        Object result;
        try {
            result = future.get(l, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            future.cancel(true);
            if(StringUtils.isBlank(iHystrixCommand.fallBack()))
                throw e;
            result = invokeFallBack(joinPoint,iHystrixCommand);
        }
        return result;
    }

    private Object invokeFallBack(ProceedingJoinPoint joinPoint, IHystrixCommand iHystrixCommand) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //获取被代理的方法和参数
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        Class[] parameterTypes = method.getParameterTypes();

        Method fallBackMethod = joinPoint.getTarget().getClass().getMethod(iHystrixCommand.fallBack(),parameterTypes);
        fallBackMethod.setAccessible(true);
        return fallBackMethod.invoke(joinPoint.getTarget(),joinPoint.getArgs());
    }


}
