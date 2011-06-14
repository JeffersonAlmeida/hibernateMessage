package hibernate;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class Test {

	public static void main(String[] args){
		
		Session session = MyHibernateSingleton.getInstance().openSession();
		Transaction transaction = session.beginTransaction();
		
		Message message = new Message("Hello World!");
		System.out.println("Message :: " + message.getText());
		
		session.save(message);
		transaction.commit();
		session.close();
				
		
	}

}
