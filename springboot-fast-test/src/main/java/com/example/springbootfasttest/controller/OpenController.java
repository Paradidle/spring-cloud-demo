package com.example.springbootfasttest.controller;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import com.example.springbootfasttest.result.WechatEventResponseResult;
import com.example.springbootfasttest.result.WechatEventResult;
import com.example.springbootfasttest.utils.HttpClient;
import com.google.gson.Gson;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * CreateDate:2025/12/13
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2025/12/13；
 */
@RestController
@RequestMapping("/open")
public class OpenController {

    private final static String APP_ID = System.getenv("APP_ID");
    private final static String APP_SECRET = System.getenv("APP_SECRET");


    private final static String TOKEN = "test";
    private final static String ENCODING_AES_KEY = "6RYGllQyIbtMgxVWEdqpR5xPbdMgo7bUhFMTYewJkZk";


    private final static String USER_INFO_URL = "https://api.weixin.qq.com/cgi-bin/user/info";

    private final static String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";

    @GetMapping("/receiveSubscriptionEvent")
    public String verify(String signature, String nonce, Long timestamp, String echostr) {
        // signature=e417866efed469f22747dc0e2ee70f20f33ec180, nonce=1096287447, echostr=130213875726254039, timestamp=1765620611
        // <xml><ToUserName><![CDATA[gh_3c403a5bf7ad]]></ToUserName><FromUserName><![CDATA[ol6701dxD1P7iJ3Fme6o4_-9eQMI]]></FromUserName><CreateTime>1765621512</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[subscribe]]></Event><EventKey><![CDATA[]]></EventKey></xml>
        // 解析XML数据
        // 处理订阅事件
        // 返回处理结果
        return echostr;
    }


    @PostMapping(value = "/receiveSubscriptionEvent", consumes = MediaType.TEXT_XML_VALUE,
            produces = MediaType.TEXT_XML_VALUE)
    public WechatEventResponseResult receiveSubscriptionEvent(@RequestParam(required = false) String signature, @RequestParam(required = false) String nonce, @RequestParam(required = false) Long timestamp, @RequestBody WechatEventResult wechatEventResult){
        // signature=e417866efed469f22747dc0e2ee70f20f33ec180, nonce=1096287447, echostr=130213875726254039, timestamp=1765620611
        System.out.println("wechatEventResult" + wechatEventResult.getFromUserName());
        System.out.println("signature" + signature);
        System.out.println("nonce" + nonce);
        System.out.println("timestamp" + timestamp);

        // <xml><ToUserName><![CDATA[gh_3c403a5bf7ad]]></ToUserName><FromUserName><![CDATA[ol6701dxD1P7iJ3Fme6o4_-9eQMI]]></FromUserName><CreateTime>1765621512</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[subscribe]]></Event><EventKey><![CDATA[]]></EventKey></xml>


        System.out.println(wechatEventResult.getFromUserName());

        System.out.println("获取token开始");
        UriComponents tokenComponents = UriComponentsBuilder.fromUriString(ACCESS_TOKEN_URL)
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", APP_ID)
                .queryParam("secret", APP_SECRET)
                .build();

        try {
            System.out.println("获取token url" + tokenComponents.toUriString());
            String token = HttpClient.get(tokenComponents.toUriString());


            Gson gson = new Gson();

            Map<String, String> result = gson.fromJson(token, Map.class);

            System.out.println("获取token url响应" + token);

            String accessToken = result.get("access_token");


            System.out.println("获取用户信息开始");
            UriComponents uriComponents = UriComponentsBuilder.fromUriString(USER_INFO_URL)
                    .queryParam("access_token", accessToken)
                    .queryParam("openid", wechatEventResult.getFromUserName())
                    .queryParam("lang", "zh_CN")
                    .build();

            System.out.println("获取用户信息url" + uriComponents.toUriString());
            String response = HttpClient.get(uriComponents.toUriString());
            System.out.println("获取用户信息response" + response);


            WechatEventResponseResult responseResult = new WechatEventResponseResult();
            responseResult.setContent("感谢关注！→点击https://www.baidu.com");
            responseResult.setCreateTime(System.currentTimeMillis()/ 1000);
            responseResult.setMsgType("text");
            responseResult.setFromUserName(wechatEventResult.getToUserName());
            responseResult.setToUserName(wechatEventResult.getFromUserName());

            return responseResult;

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 解析XML数据
        // 处理订阅事件
        // 返回处理结果
        return null;
    }


    @GetMapping("/createMenu")
    public String createMenu() {
        RestTemplate restTemplate = new RestTemplate();

        System.out.println("获取token开始");
        UriComponents tokenComponents = UriComponentsBuilder.fromUriString(ACCESS_TOKEN_URL)
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", APP_ID)
                .queryParam("secret", APP_SECRET)
                .build();
        try {


            System.out.println("获取token url" + tokenComponents.toUriString());
            String token = HttpClient.get(tokenComponents.toUriString());

            Gson gson = new Gson();

            Map<String, String> result = gson.fromJson(token, Map.class);

            System.out.println("获取token url响应" + token);

            String accessToken = result.get("access_token");


            List<Map<String,Object>> buttonList = new ArrayList<>();
            HashMap<String, Object> first = new HashMap<>();
            HashMap<String, String> firstSub = new HashMap<>();

            first.put("name","今日歌曲");

            firstSub.put("name","biubiubiu");
            firstSub.put("url", "https://www.bilibili.com");
            first.put("sub_button",firstSub);

            buttonList.add(first);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> mapResponseEntity = restTemplate.postForEntity("https://api.weixin.qq.com/cgi-bin/menu/create?access_token=" + accessToken, new HttpEntity<>(buttonList, headers), Map.class);
            System.out.println(gson.toJson(mapResponseEntity.getBody()));

        }catch ( Exception e){
             e.printStackTrace();
        }
        return "success";
    }


    private WechatEventResult parseXml(String xml) {
        try {
            JAXBContext context = JAXBContext.newInstance(WechatEventResult.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (WechatEventResult) unmarshaller.unmarshal(new StringReader(xml));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
