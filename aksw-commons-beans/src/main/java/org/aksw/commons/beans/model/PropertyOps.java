package org.aksw.commons.beans.model;

import java.lang.reflect.Method;

public interface PropertyOps {
    String getName();
    Class<?> getType();

    boolean acceptsType(Class<?> clazz);
    //boolean acceptsValue(Object entity);
    
    void setValue(Object entity, Object value);
    Object getValue(Object entity);
    
    boolean isWritable();
    boolean isReadable();
    
    /**
     * If applicable, returns the write method associated with this property.
     * A return value of null only indicates, that no Java method has been associated
     * with the property ops. The property may still be writable.
     * 
     * @return
     */
    Method getWriteMethod();    
    Method getReadMethod();
    
    <A> A findAnnotation(Class<A> annotationClass);
}
