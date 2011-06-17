package hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class MyHibernateSingleton {
	
	private static SessionFactory factory;
	
	private MyHibernateSingleton(){
		super();
	}
	
	public static SessionFactory getInstance(){
		if(factory==null){
			factory = new Configuration().configure().buildSessionFactory();
		}
		return factory;
	}
		
}
