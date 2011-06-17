package hibernate;

import java.util.ArrayList;
import java.util.Iterator;

import org.hibernate.Transaction;
import org.hibernate.classic.Session;

public class TestRetrieveAll {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Session session = MyHibernateSingleton.getInstance().openSession();
		Transaction transaction = session.beginTransaction();
		
		ArrayList<Message> messages = (ArrayList<Message>) session.find("From Message as m order by m.text asc");
		Iterator<Message> iterator = messages.iterator();
		System.out.println("\n\n************************************************");
		while(iterator.hasNext()){
			System.out.println(iterator.next());
		}		
		System.out.println("\n\n************************************************\n\n");
		transaction.commit();
		session.close();

	}

}
