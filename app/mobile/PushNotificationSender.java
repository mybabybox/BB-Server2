package mobile;

import java.util.HashMap;
import java.util.Map;

import javapns.Push;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotifications;

import org.apache.commons.lang3.StringUtils;

import play.Play;
import play.libs.Json;
import models.PushNotificationToken;

import com.google.android.gcm.server.Sender;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;

import common.utils.StringUtil;
import controllers.Application;

public class PushNotificationSender {
    private static final play.api.Logger logger = play.api.Logger.apply(PushNotificationSender.class);

    public static final String API_SERVER_KEY = Play.application().configuration().getString("gcm.api.server.key");
    
    public static final String APN_CERT_DEV = Play.application().configuration().getString("apn.dev.cert");
    public static final String APN_CERT_PROD = Play.application().configuration().getString("apn.prod.cert");
    public static final String APN_API_PASS = Play.application().configuration().getString("apn.api.pass");
    public static final String APN_IS_PROD = Play.application().configuration().getString("apn.api.isprod");

    public static final String MESSAGE_KEY = "message";
    public static final String ACTOR = "actor";
    public static final String MESSAGE = "message";
    public static final String MESSAGE_TYPE = "messageType";
    public static final String POST_ID = "postId";
    
    private static final int TTL = 30;
    private static final int RETRIES = 2;
    
    public static enum NotificationType {
        CONVERSATION,
        COMMENT,
        FOLLOW
    }

    public static void sendNewCommentNotification(Long userId, String actor, String message, Long postId) {
        if (StringUtils.isEmpty(actor) || StringUtils.isEmpty(message)) {
            return;
        }
        
        Map<String, String> map = new HashMap<>();
        map.put(ACTOR, actor);
        map.put(MESSAGE, StringUtil.shortMessage(message));
        map.put(MESSAGE_TYPE, NotificationType.COMMENT.name());
        map.put(POST_ID, postId.toString());
        sendNotification(userId, map);
    }
    
    public static void sendNewMessageNotification(Long userId, String actor, String message) {
        if (StringUtils.isEmpty(actor) || StringUtils.isEmpty(message)) {
            return;
        }
        
        Map<String, String> map = new HashMap<>();
        map.put(ACTOR, actor);
        map.put(MESSAGE, StringUtil.shortMessage(message));
        map.put(MESSAGE_TYPE, NotificationType.CONVERSATION.name());
        sendNotification(userId, map);
    }
    
    public static void sendNewFollowNotification(Long userId, String actor) {
        if (StringUtils.isEmpty(actor)) {
            return;
        }
        
        Map<String, String> map = new HashMap<>();
        map.put(ACTOR, actor);
        map.put(MESSAGE, "");
        map.put(MESSAGE_TYPE, NotificationType.FOLLOW.name());
        sendNotification(userId, map);
    }
    
    private static void sendNotification(Long userId,  Map<String, String> map) {
        if (Application.isDev()) {
            return;
        }
        
        PushNotificationToken token = PushNotificationToken.findByUserId(userId);
        if (token != null) {
            if (Application.DeviceType.IOS.equals(token.deviceType)) {
                //sendToApn(userId, token.token, map);
            } else if (Application.DeviceType.ANDROID.equals(token.deviceType)) {
                sendToGcm(userId, token.token, map);
            }
        } else {
            logger.underlyingLogger().info("[u="+userId+"] User does not have push notification token");
        }
    }

    private static boolean sendToApn(Long userId, String token, Map<String, String> map) {
        try {
            String pass = APN_API_PASS;
            Boolean prod = Boolean.parseBoolean(APN_IS_PROD);
            String cert = null;

            if (prod) {
                cert = APN_CERT_PROD;
            } else {
                cert = APN_CERT_DEV;
            }
            
            String content = 
                    "{\"aps\":{\"content-available\":1,"+
                    "\"sound\":\"default"+"\","+
                    "\"actor\":\""+map.get(ACTOR)+"\","+
                    "\"postId\":\""+map.get(POST_ID)+"\","+
                    "\"messageType\":\""+map.get(MESSAGE_TYPE)+"\","+
                    "\"alert\":\""+map.get(MESSAGE)+"\"}}";
            
            PushNotificationPayload payload = PushNotificationPayload.fromJSON(content);
            PushedNotifications notifications = Push.payload(payload, cert, pass, prod, token);
            
            logger.underlyingLogger().info("[u="+userId+"][token="+token+"][content="+content+"] Apn send result: "+notifications.toString());
            return true;
        } catch (Exception e) {
            logger.underlyingLogger().error("[u="+userId+"] Error in Apn send", e);
            return false;
        }
    }
    
    private static boolean sendToGcm(Long userId, String token, Map<String, String> map) {
        try {
            String content = Json.stringify(Json.toJson(map));
            Sender sender = new Sender(API_SERVER_KEY);
            Message message = new Message.Builder().timeToLive(TTL)
                    .collapseKey(MESSAGE_KEY)
                    .delayWhileIdle(true)
                    .addData(MESSAGE_KEY, content).build();

            Result result = sender.send(message, token, RETRIES);
            logger.underlyingLogger().info("[u="+userId+"][token="+token+"][content="+content+"] Gcm send result: "+result);
            return true;
        } catch (Exception e) {
            logger.underlyingLogger().error("[u="+userId+"] Error in Gcm send", e);
            return false;
        }
    }
}
