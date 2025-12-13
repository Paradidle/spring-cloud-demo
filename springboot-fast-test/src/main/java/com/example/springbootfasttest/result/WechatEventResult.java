package com.example.springbootfasttest.result;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

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
@Data
@XmlRootElement(name = "xml")
@XmlAccessorType(XmlAccessType.FIELD)
public class WechatEventResult {
    /**
     * 开发者账号
     */
    @XmlElement(name = "ToUserName")
    private String toUserName;

    /**
     * 用户openid
     */
    @XmlElement(name = "FromUserName")
    private String fromUserName;

    /**
     * 消息创建时间 （整型）
     */
    @XmlElement(name = "CreateTime")
    private Long createTime;
    /**
     * 消息类型，event
     */
    @XmlElement(name = "MsgType")
    private String msgType;
    /**
     * 事件类型，subscribe(订阅)、unsubscribe(取消关注)
     */
    @XmlElement(name = "Event")
    private String event;
    /**
     * 事件KEY值，与自定义菜单接口中KEY值对应
     */
    @XmlElement(name = "EventKey")
    private String eventKey;

    // getters and setters
}
