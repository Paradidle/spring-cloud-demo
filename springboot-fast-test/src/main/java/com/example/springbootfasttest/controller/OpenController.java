package com.example.springbootfasttest.controller;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Objects;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import com.example.springbootfasttest.result.WechatEventResult;
import com.example.springbootfasttest.utils.HttpClient;
import com.example.springbootfasttest.utils.OkHttpUtils;
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
@RequestMapping("/open")
public class OpenController {

    private final static String APP_ID = "wx4963ced332c06b33";

    private final static String APP_SECRET = "2c4029e8ec7959614361d006943b7e5f";

    private final static String TOKEN = "test";
    private final static String ENCODING_AES_KEY = "6RYGllQyIbtMgxVWEdqpR5xPbdMgo7bUhFMTYewJkZk";


    private final static String USER_INFO_URL = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=o6_bmjrPTlm6_2sgVt7hMZOPfL2M&lang=zh_CN";

    private final static String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";

    private OkHttpUtils okHttpUtils = OkHttpUtils.getInstance();

    @GetMapping("/receiveSubscriptionEvent")
    public String verify(String signature, String nonce, Long timestamp, String echostr) {
        // signature=e417866efed469f22747dc0e2ee70f20f33ec180, nonce=1096287447, echostr=130213875726254039, timestamp=1765620611
        // <xml><ToUserName><![CDATA[gh_3c403a5bf7ad]]></ToUserName><FromUserName><![CDATA[ol6701dxD1P7iJ3Fme6o4_-9eQMI]]></FromUserName><CreateTime>1765621512</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[subscribe]]></Event><EventKey><![CDATA[]]></EventKey></xml>
        // 解析XML数据
        // 处理订阅事件
        // 返回处理结果
        return echostr;
    }


    @PostMapping(value = "/receiveSubscriptionEvent", consumes = "application/xml")
    public String receiveSubscriptionEvent(String xmlData) throws IOException {
        // signature=e417866efed469f22747dc0e2ee70f20f33ec180, nonce=1096287447, echostr=130213875726254039, timestamp=1765620611


        // <xml><ToUserName><![CDATA[gh_3c403a5bf7ad]]></ToUserName><FromUserName><![CDATA[ol6701dxD1P7iJ3Fme6o4_-9eQMI]]></FromUserName><CreateTime>1765621512</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[subscribe]]></Event><EventKey><![CDATA[]]></EventKey></xml>

        WechatEventResult wechatEventResult = parseXml(xmlData);

        if (Objects.isNull(wechatEventResult)) {
            return "error";
        }

        System.out.println(wechatEventResult.getFromUserName());

        UriComponents tokenComponents = UriComponentsBuilder.fromUriString(ACCESS_TOKEN_URL)
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", APP_ID)
                .queryParam("secret", APP_SECRET)
                .build();

        String token = HttpClient.get(tokenComponents.toUriString());


        Gson gson = new Gson();

        Map<String,String> result = gson.fromJson(token, Map.class);

        String accessToken = result.get("access_token");


        UriComponents uriComponents = UriComponentsBuilder.fromUriString(USER_INFO_URL)
                .queryParam("access_token", accessToken)
                .queryParam("openid", wechatEventResult.getFromUserName())
                .queryParam("lang", "zh_CN")
                .build();

        String response = HttpClient.get(uriComponents.toUriString());
        System.out.println(response);

        // 解析XML数据
        // 处理订阅事件
        // 返回处理结果
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
