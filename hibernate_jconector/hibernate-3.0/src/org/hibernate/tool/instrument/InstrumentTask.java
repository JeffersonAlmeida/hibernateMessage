//$Id: InstrumentTask.java,v 1.3 2005/03/17 21:29:08 oneovthafew Exp $
package org.hibernate.tool.instrument;

import java.util.Arrays;

import net.sf.cglib.transform.AbstractTransformTask;
import net.sf.cglib.transform.ClassTransformer;
import net.sf.cglib.transform.impl.InterceptFieldEnabled;
import net.sf.cglib.transform.impl.InterceptFieldFilter;
import net.sf.cglib.transform.impl.InterceptFieldTransformer;

import org.objectweb.asm.Type;

/**
 * An Ant task for instrumenting persistent classes with
 * CGLIB field interception
 *  
 * @author Gavin King
 */
public class InstrumentTask extends AbstractTransformTask {

	protected ClassTransformer getClassTransformer(String[] classInfo) {
		
		if( Arrays.asList(classInfo).contains( InterceptFieldEnabled.class.getName() ) ){
			return null;
		}
		else {
			return new InterceptFieldTransformer(
				new InterceptFieldFilter() {
					public boolean acceptRead(Type owner, String name) {
						return true;
					}
					public boolean acceptWrite(Type owner, String name) {
						return true;
					}			
				}
			);
		}
		
	}
	
}
