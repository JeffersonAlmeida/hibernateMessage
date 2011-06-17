package hibernate;

import org.hibernate.Session;
import org.hibernate.Transaction;


public class TesteInsert {
	
	
	public static void main(String[] args){
		
		Session session = MyHibernateSingleton.getInstance().openSession();
		Transaction transaction = session.beginTransaction();
		
		for(int i = 0 ; i< 10 ; i++){
			session.save(new Message(" Message "+ i));
		}
		
		transaction.commit();
		session.close();	
	}
	
}
