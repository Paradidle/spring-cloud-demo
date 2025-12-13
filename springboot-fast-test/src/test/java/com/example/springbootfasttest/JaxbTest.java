package com.example.springbootfasttest;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
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

public class JaxbTest {
    public static void main(String[] args) {
        try {
            // 测试JAXB是否可用
            JAXBContext context = JAXBContext.newInstance(WechatEventResult.class);
            System.out.println("JAXBContext创建成功: " + context.getClass().getName());

            // 测试XML到对象的转换
            String xml = "<xml>" +
                    "<ToUserName><![CDATA[toUser]]></ToUserName>" +
                    "<FromUserName><![CDATA[fromUser]]></FromUserName>" +
                    "<CreateTime>123456789</CreateTime>" +
                    "<MsgType><![CDATA[event]]></MsgType>" +
                    "<Event><![CDATA[subscribe]]></Event>" +
                    "</xml>";

            Unmarshaller unmarshaller = context.createUnmarshaller();
            WechatEventResult msg = (WechatEventResult) unmarshaller.unmarshal(new StringReader(xml));

            System.out.println("XML解析成功:");
            System.out.println("  Event: " + msg.getEvent());
            System.out.println("  From: " + msg.getFromUserName());

            // 测试对象到XML的转换
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            StringWriter writer = new StringWriter();
            marshaller.marshal(msg, writer);
            System.out.println("对象转XML成功:");
            System.out.println(writer.toString());

        } catch (Exception e) {
            System.err.println("JAXB测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
