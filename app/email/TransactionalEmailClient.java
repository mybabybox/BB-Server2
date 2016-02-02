package email;

public interface TransactionalEmailClient {

    public String sendMail(String to, String from, String fromName, String subject, String body);
}
