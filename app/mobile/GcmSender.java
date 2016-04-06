package mobile;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import play.Play;
import play.libs.Json;
import models.PushNotificationToken;

import com.google.android.gcm.server.Sender;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;

import common.utils.StringUtil;
import controllers.Application;

public class GcmSender {
    private static final play.api.Logger logger = play.api.Logger.apply(GcmSender.class);

    public static final String MESSAGE_KEY = "message";
    
    public static final String API_SERVER_KEY = Play.application().configuration().getString("gcm.api.server.key");
    
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
        map.put("actor", actor);
        map.put("message", StringUtil.shortMessage(message));
        map.put("messageType", NotificationType.COMMENT.name());
        map.put("postId", postId.toString());
        sendNotification(userId, Json.stringify(Json.toJson(map)));
    }
    
    public static void sendNewMessageNotification(Long userId, String actor, String message) {
        if (StringUtils.isEmpty(actor) || StringUtils.isEmpty(message)) {
            return;
        }
        
        Map<String, String> map = new HashMap<>();
        map.put("actor", actor);
        map.put("message", StringUtil.shortMessage(message));
        map.put("messageType", NotificationType.CONVERSATION.name());
        sendNotification(userId, Json.stringify(Json.toJson(map)));
    }
    
    public static void sendNewFollowNotification(Long userId, String actor) {
        if (StringUtils.isEmpty(actor)) {
            return;
        }
        
        Map<String, String> map = new HashMap<>();
        map.put("actor", actor);
        map.put("message", "");
        map.put("messageType", NotificationType.FOLLOW.name());
        sendNotification(userId, Json.stringify(Json.toJson(map)));
    }
    
    private static void sendNotification(Long userId, String message) {
        if (Application.isDev()) {
            return;
        }
        
        PushNotificationToken token = PushNotificationToken.findByUserId(userId);
        if (token != null) {
            if (Application.DeviceType.IOS.equals(token.deviceType)) {
                sendToApn(userId, token.token, message);
            } else if (Application.DeviceType.ANDROID.equals(token.deviceType)) {
                sendToGcm(userId, token.token, message);
            }
        } else {
            logger.underlyingLogger().info("[u="+userId+"] User does not have push notification token");
        }
    }

    private static boolean sendToApn(Long userId, String token, String msg) {
        return true;
    }
    
    private static boolean sendToGcm(Long userId, String token, String msg) {
        try {
            Sender sender = new Sender(API_SERVER_KEY);
            Message message = new Message.Builder().timeToLive(TTL)
                    .collapseKey(MESSAGE_KEY)
                    .delayWhileIdle(true)
                    .addData(MESSAGE_KEY, msg).build();

            Result result = sender.send(message, token, RETRIES);
            logger.underlyingLogger().info("[u="+userId+"][token="+token+"][msg="+msg+"] Gcm send result: "+result);
            return true;
        } catch (Exception e) {
            logger.underlyingLogger().error("[u="+userId+"] Error in Gcm send", e);
            return false;
        }
    }
}