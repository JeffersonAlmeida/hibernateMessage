//$Id: Component.java,v 1.1 2004/09/26 05:18:25 oneovthafew Exp $
package org.hibernate.test.legacy;

/**
 * Component
 * 
 * @author Emmanuel Bernard
 */
public class Component {
    private String _name;
    
    private SubComponent _subComponent;

    /**
     * @return
     */
    public String getName() {
        return _name;
    }

    /**
     * @param string
     */
    public void setName(String string) {
        _name = string;
    }

    /**
     * @return
     */
    public SubComponent getSubComponent() {
        return _subComponent;
    }

    /**
     * @param component
     */
    public void setSubComponent(SubComponent component) {
        _subComponent = component;
    }

}
