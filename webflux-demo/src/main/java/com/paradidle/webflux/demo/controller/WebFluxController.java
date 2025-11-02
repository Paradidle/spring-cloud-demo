package com.paradidle.webflux.demo.controller;

import com.paradidle.webflux.demo.vo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
 * CreateDate:2021/7/4
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/7/4；
 */
@RestController
public class WebFluxController {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    // 阻塞5秒钟
    private String createStr() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
        }
        return "some string";
    }

    // 普通的SpringMVC方法
    @GetMapping("/1")
    public String get1() {
        logger.info("get1 start");
        String result = createStr();
        logger.info("get1 end.");
        return result;
    }

    // WebFlux(返回的是Mono)
    @GetMapping("/2")
    public Mono<String> get2() {
        logger.info("get2 start");
        Mono<String> result = Mono.fromSupplier(this::createStr);
        logger.info("get2 end.");
        return result;
    }

    @GetMapping(value = "/3", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> flux() {
        return Flux.fromStream(IntStream.range(1, 10).mapToObj(i -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                    }
                    return "flux data--" + i;
                }));
    }

    @GetMapping(value = "/4", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<User> flux4() {
        return Flux.range(1, 9)
                .flatMap(i -> Mono.delay(Duration.ofSeconds(i))
                        .thenReturn(new User("user" + i)));
    }
}
