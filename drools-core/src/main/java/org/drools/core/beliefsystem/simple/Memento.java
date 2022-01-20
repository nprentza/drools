package org.drools.core.beliefsystem.simple;

import org.drools.core.common.DefaultFactHandle;
import org.kie.api.runtime.rule.FactHandle;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Memento {

    private DefaultFactHandle instance;
    private Map<String, Object> stateRestore;
    private Map<String, Object> stateRequired;

    public Memento(FactHandle instance, Map<String, Object> stateRequired) {
        this.instance = (DefaultFactHandle) instance;
        this.stateRestore = new HashMap<>();
        this.stateRequired = stateRequired;
    }

    public DefaultFactHandle getFactHandle() {
        return this.instance;
    }

    public void store() {
        // save the current state of the object in stateRestore
        try {
            // get all properties defined in the instance.getClass()
            BeanInfo info = Introspector.getBeanInfo(instance.getObject().getClass(), Object.class);
            for ( PropertyDescriptor pd : info.getPropertyDescriptors() ) {
                if (pd.getReadMethod() != null)
                    stateRestore.put(pd.getName().toString(),pd.getReadMethod().invoke(instance.getObject()));
                else
                    stateRestore.put(pd.getName(),null);
            }
        } catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // the command the BS will call to do the updates
    public void update() {
        this.store();
        // apply the state required (stateRequired) to the instance.getObject()
        try {
            BeanInfo info = Introspector.getBeanInfo(instance.getObject().getClass(), Object.class);
            for ( PropertyDescriptor pd : info.getPropertyDescriptors() ) {
                //we only update the properties that a corresponding key exists in the stateRequired map
                if (pd.getWriteMethod() != null && stateRequired.get(pd.getName())!=null)
                    pd.getWriteMethod().invoke(instance.getObject(), stateRequired.get(pd.getName()));
            }
        } catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void restore() {
        try {
            BeanInfo info = Introspector.getBeanInfo(instance.getObject().getClass(), Object.class);
            for ( PropertyDescriptor pd : info.getPropertyDescriptors() ) {

                if (pd.getWriteMethod() != null)
                    pd.getWriteMethod().invoke(instance.getObject(), stateRestore.get(pd.getName()));
                else
                    pd.getWriteMethod().invoke(instance.getObject(), null);
            }
        } catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    //
    public int hashCode() {
        int result = instance.getClass().hashCode();
        result = 31 * result + stateRequired.keySet().hashCode();
        return result;
    }

    //
    public boolean equals(Object o) {

        if (!(o instanceof Memento)) {
            return false;
        }

        Memento m = (Memento)o;
        if (m.instance.getClass() != this.instance.getClass()) {
            return false;
        }

        return m.stateRequired.keySet().equals(this.stateRequired.keySet());
    }

}