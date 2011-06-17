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
			//configuration.addResource("hibernate/Message.hbm.xml"); //addClass(Message.class);
			//configuration.addClass(hibernate.Message.class);
			//configuration.setProperties(System.getProperties());
			//factory = configuration.buildSessionFactory();
		}
		return factory;
	}
		
}
