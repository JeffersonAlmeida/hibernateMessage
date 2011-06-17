package hibernate;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class TestUpdate {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Session session = MyHibernateSingleton.getInstance().openSession();
		Transaction transaction = session.beginTransaction();
		
		Message message = (Message) session.load(Message.class, new Integer(1));
		System.out.println(message.toString());
		message.setText("testando update ");
		Message message2 = new Message("Take me to your leader AGAIN please");		
		message.setNextMessage(message2);
		
		transaction.commit();
		session.close();

	}

}
