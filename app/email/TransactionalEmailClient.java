package email;

public interface TransactionalEmailClient {

    public String sendMail(String mailId, String subject, String body);
}
