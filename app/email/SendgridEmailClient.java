package email;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import models.User;
import play.Logger;

import com.feth.play.module.mail.Mailer.Mail.Body;
import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;
import com.sendgrid.smtpapi.SMTPAPI;

import common.utils.HtmlUtil;

public class SendgridEmailClient implements TransactionalEmailClient {
	
	protected final static String SENDGRID_USERNAME = "mybabybox";
	protected final static String SENDGRID_PASSWORD = "myBabyEd3";
	
	static SendgridEmailClient client = new SendgridEmailClient();
	
	public static SendgridEmailClient getInstatnce(){
		return client;
	}
	
	public String sendMail(String mailId, String subject, String htmlBody){
		SendGrid sendgrid = new SendGrid(SENDGRID_USERNAME, SENDGRID_PASSWORD);

	    SendGrid.Email email = new SendGrid.Email();
	    email.addTo(mailId);
	    email.setFrom("other@example.com");
	    email.setSubject(subject);
	    email.setHtml(htmlBody);
	    try {
	      SendGrid.Response response = sendgrid.send(email);
	      System.out.println(response.getMessage());
	      return response.getMessage();
	    }
	    catch (SendGridException e) {
	      System.err.println(e);
	      return(e.getMessage());
	    }
	}
	
	public String sendMailOnComment(User sender, User recipent, String body){
		return sendMail(recipent.email, "BabyBox Comment", HtmlUtil.appendTitle(sender.displayName+" commented '"+body+"' on your post"));
	}
	
	public String sendMailOnFollow(User sender, User recipent){
		final String text = getEmailTemplate(
				"views.html.account.email.follow_mail",
				recipent.displayName);
		
		final String html = getEmailTemplate(
				"views.html.account.email.follow_mail",
				recipent.displayName);

		//return sendMail(recipent.email, "BabyBox Follow", HtmlUtil.appendP(sender.name+" followd you"));
		return sendMail(recipent.email, "BabyBox Follow", html);
	}
	
	public String sendMailOnLike(User sender, User recipent, String postTitle){
		return sendMail(recipent.email, "BabyBox Liked", HtmlUtil.appendP(sender.name+" Liked your post "+postTitle));
	}
	
	protected String getEmailTemplate(final String template, final String name) {
		Class<?> cls = null;
		String ret = null;
		try {
			cls = Class.forName(template);
		} catch (ClassNotFoundException e) {
			Logger.warn("Template: '"
					+ template
					+ "' was not found! Trying to use English fallback template instead.");
		}
		if (cls == null) {
			try {
				cls = Class.forName(template);
			} catch (ClassNotFoundException e) {
				Logger.error("Fallback template: '" + template 
						+ "' was not found either!");
			}
		}
		if (cls != null) {
			Method htmlRender = null;
			try {
				htmlRender = cls.getMethod("render",String.class);
				ret = htmlRender.invoke(null, name)
						.toString();

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
	
}
