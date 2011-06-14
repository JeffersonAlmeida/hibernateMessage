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
			Configuration configuration = new Configuration();
			//configuration.addResource("hibernate/Message.hbm.xml"); //addClass(Message.class);
			configuration.addClass(Message.class);
			configuration.setProperties(System.getProperties());
			factory = configuration.buildSessionFactory();
		}
		return factory;
	}
		
}
