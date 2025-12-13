package com.example.springbootfasttest.controller;

import java.io.StringReader;
import java.util.Objects;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.example.springbootfasttest.result.WechatEventResult;

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
    public String receiveSubscriptionEvent(String xmlData) {
        // signature=e417866efed469f22747dc0e2ee70f20f33ec180, nonce=1096287447, echostr=130213875726254039, timestamp=1765620611


        // <xml><ToUserName><![CDATA[gh_3c403a5bf7ad]]></ToUserName><FromUserName><![CDATA[ol6701dxD1P7iJ3Fme6o4_-9eQMI]]></FromUserName><CreateTime>1765621512</CreateTime><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[subscribe]]></Event><EventKey><![CDATA[]]></EventKey></xml>

        WechatEventResult wechatEventResult = parseXml(xmlData);

        if (Objects.isNull(wechatEventResult)) {
            return "error";
        }

        System.out.println(wechatEventResult.getFromUserName());
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
