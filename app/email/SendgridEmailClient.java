package email;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;

import models.Post;
import models.User;
import play.Logger;
import play.Play;

import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;

import common.schedule.JobScheduler;
import common.utils.HtmlUtil;
import controllers.Application;

public class SendgridEmailClient implements TransactionalEmailClient {
    private static final play.api.Logger logger = play.api.Logger.apply(SendgridEmailClient.class);
    
    public static final String SENDGRID_MAIL_FROM_NAME = 
            Play.application().configuration().getString("sendgrid.mail.from.name");
    
    public static final String SENDGRID_MAIL_FROM_ADDRESS = 
            Play.application().configuration().getString("sendgrid.mail.from.address");
    
    public static final String SENDGRID_AUTHEN_USERNAME = 
            Play.application().configuration().getString("sendgrid.authen.username");
    
    public static final String SENDGRID_AUTHEN_PASSWORD = 
            Play.application().configuration().getString("sendgrid.authen.password");
    
    public static final String POST_IMAGE_BY_ID_URL = Application.APPLICATION_BASE_URL + "/image/get-post-image-by-id/";
    
    private SendGrid sendgrid;
    
	private static SendgridEmailClient client = new SendgridEmailClient();
	
	public static SendgridEmailClient getInstatnce(){
		return client;
	}
	
	private SendgridEmailClient() {
	    sendgrid = new SendGrid(SENDGRID_AUTHEN_USERNAME, SENDGRID_AUTHEN_PASSWORD);
	}
	
	public void sendMailAsync(final String mailId, final String subject, final String body) {
	    JobScheduler.getInstance().run(
	            new Runnable() {
		            @Override
		            public void run() {
		                sendMail(mailId, subject, body);
		            }
		        });
	}
	
	@Override
	public String sendMail(String mailId, String subject, String body) {
	    SendGrid.Email email = new SendGrid.Email();
	    email.addTo(mailId);
	    email.setFromName(SENDGRID_MAIL_FROM_NAME);
	    email.setFrom(SENDGRID_MAIL_FROM_ADDRESS);
	    email.setSubject(subject);
	    email.setHtml(body);
	    try {
	        SendGrid.Response response = sendgrid.send(email);
	        logger.underlyingLogger().info("[email="+mailId+"] sendMail response="+response.getMessage()+" body="+body);
	        return response.getMessage();
	    } catch (SendGridException e) {
	        logger.underlyingLogger().error("[email="+mailId+"] sendMail body="+body+" error="+e.getMessage(), e);
	        return(e.getMessage());
	    }
	}
	
	public void sendMailOnFollow(User actor, User target) {
	    if (StringUtils.isEmpty(target.email)) {
            logger.underlyingLogger().warn("[recipient="+target.displayName+"] sendMailOnFollow recipient email is null");
            return;
        }
	    
		String template = getEmailTemplate(
				"views.html.account.email.sendgrid.follow_mail",
				actor.displayName,
				target.displayName);
		
		sendMail(
		        target.email, 
		        formatSubject("有人關注了你"), 
		        template);
	}
	
	public void sendMailOnComment(User actor, User target, String product, String comment) {
	    if (StringUtils.isEmpty(product) || StringUtils.isEmpty(comment)) {
            return;
        }
	    
        if (StringUtils.isEmpty(target.email)) {
            logger.underlyingLogger().warn("[recipient="+target.displayName+"] sendMailOnComment recipient email is null");
            return;
        }
        
        String template = getEmailTemplate(
                "views.html.account.email.sendgrid.comment_mail",
                actor.displayName,
                target.displayName,
                product,
                comment);
        
        sendMail(
                target.email, 
                formatSubject("你的商品有新留言 - "+product), 
                template);
    }
	
	public void sendMailOnConversation(User actor, User target, String product, String message) {
	    if (StringUtils.isEmpty(product) || StringUtils.isEmpty(message)) {
            return;
        }
	    
        if (StringUtils.isEmpty(target.email)) {
            logger.underlyingLogger().warn("[recipient="+target.displayName+"] sendMailOnConversation recipient email is null");
            return;
        }
        
        String template = getEmailTemplate(
                "views.html.account.email.sendgrid.conversation_mail",
                actor.displayName,
                target.displayName,
                product,
                message);
        
        sendMail(
                target.email, 
                formatSubject("你的商品有新訊息 - "+product), 
                template);
    }
	
	public void sendMailOnPost(Post post) {
	    User target = post.owner;
        if (StringUtils.isEmpty(target.email)) {
            logger.underlyingLogger().warn("[recipient="+target.displayName+"] sendMailOnPost recipient email is null");
            return;
        }
        
        String htmlBody = 
                HtmlUtil.appendImage(POST_IMAGE_BY_ID_URL+post.getImage(), 150, 150)+HtmlUtil.appendBr()+ 
                post.title+HtmlUtil.appendBr()+HtmlUtil.appendBr()+ 
                "價格: $" + post.price.intValue();
        
        String template = getEmailTemplate(
                "views.html.account.email.sendgrid.post_mail",
                post.owner.displayName,
                target.displayName,
                post.title,
                htmlBody);
        
        sendMail(
                target.email, 
                formatSubject("成功刊登商品 - "+post.title), 
                template);
    }
	
	protected String getEmailTemplate(final String template, final String actor, final String target) {
	    return getEmailTemplate(template, actor, target, "", "");
	}
	
	protected String getEmailTemplate(final String template, final String actor, final String target, final String product) {
        return getEmailTemplate(template, actor, target, product, "");
    }
	
	protected String getEmailTemplate(final String template, 
	        final String actor, final String target, final String product, final String body) {
	    
		Class<?> cls = null;
		String ret = null;
		try {
			cls = Class.forName(template);
		} catch (ClassNotFoundException e) {
			Logger.warn("Template: '"+ template + "' was not found!");
		}
		
		if (cls != null) {
			Method htmlRender = null;
			try {
				htmlRender = cls.getMethod("render",String.class,String.class,String.class,String.class);
				ret = htmlRender.invoke(null, actor, target, product, body).toString();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
	
	protected String formatSubject(String subject) {
	    //return "�"+subject;
	    return subject;
	}
}
