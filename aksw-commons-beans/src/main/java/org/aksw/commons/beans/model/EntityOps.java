package org.aksw.commons.beans.model;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface EntityOps {	
	
    // Entity ops may but need to be derived from a java class
    Class<?> getAssociatedClass();
    
    /**
     * Operations may be associated with an entity.
     * For example, an entity may support map operations (put, get, etc...)
     * While such an entity may e.g. have a (read-only) size property, the get / put operations
     * would be part of separate operations class associate with this entityOps instance.
     *   
     */
    <T> T getOps(Class<T> opsClass);
    
    <A> A findAnnotation(Class<A> annotationClass);
    
    
    // If false, newInstance() should not be called.
    boolean isInstantiable();
    
    Object newInstance();
    Collection<? extends PropertyOps> getProperties();
    
    
    boolean isClonable();
    Object clone(Object Entity);

    
    boolean isPrimitive();
    
    /**
     * Convenience method for getting read- and writable properties
     * 
     * @return
     */
    default Collection<? extends PropertyOps> getEditableProperties() {
    	List<? extends PropertyOps> result = getProperties().stream()
    			.filter(p -> p.isReadable() && p.isWritable())
    			.collect(Collectors.toList());
    	return result;
    }
    
    PropertyOps getProperty(String name);
    
    default Set<String> getPropertyNames() {
        Set<String> result = getProperties().stream().map(p -> p.getName()).collect(Collectors.toSet());
        return result;
    }

    
    public static Object deepCopy(Object entity, Function<Class<?>, EntityOps> classToOps) {
    	Map<Object, Object> map = new IdentityHashMap<>();

    	Object result = deepCopy(entity, classToOps, map::get, map::put);
    	return result;
    }
    
    
    

    /**
     * Entities may be container for items
     * 
     * One issue is the identity of the items:
     * E.g. a map is a collection of entry objects, but the getEntries method may create a copy of the entry objects.
     * So if we cloned a collection, we would also have to clone the entry objects - even thought they are only temporary.
     * 
     * 
     */
    // The main question is: should these items be part of entityOps, or should there be collection properties?
    boolean isCollection();
    Iterator<?> getItems(Object entity);
    void setItems(Object entity, Iterator<?> items);
    
    
    /**
     * Deep copy, with any entity in managedEntities does NOT cause the creation of a clone
     * For all entities not in the set a clone will be created and added to the set
     * 
     * @param entity
     * @param classToOps
     * @param managedEntities
     * @return
     */
    public static Object deepCopy(Object entity, Function<Class<?>, EntityOps> classToOps, Set<Object> managedEntities) {
    	Map<Object, Object> map = new IdentityHashMap<>();

    	Object result = deepCopy(
    			entity,
    			classToOps,
    			(k) ->  managedEntities.contains(k) ? k : map.get(k),
    			(k, v) -> { map.put(k, v); managedEntities.add(v); });
    	return result;
    }

    
    public static Object deepCopy(
    		Object entity, Function<Class<?>,
    		EntityOps> classToOps,
    		Function<Object, Object> getEntityToClone,
    		BiConsumer<Object, Object> putEntityToClone) {
    		
    	Set<Object> isCopied = (new IdentityHashMap<>()).keySet();// Sets.newIdentityHashSet();

    	Object result = deepCopy(
    			entity,
    			classToOps,
    			getEntityToClone,
    			putEntityToClone,
    			isCopied::contains,
    			isCopied::add);
    	return result;
    }

    public static Object deepCopy(
    		Object entity,
    		Function<Class<?>, EntityOps> classToOps,
    		Function<Object, Object> getEntityToClone,
    		BiConsumer<Object, Object> putEntityToClone,
    		Predicate<Object> getIsCopied,
    		Consumer<Object> setIsCopied)
    {
    	Object result;
    	if(entity == null) {
    		result = null;
    	} else {
	    	Class<?> entityClass = entity.getClass();
	    	EntityOps entityOps = classToOps.apply(entityClass);

	    	result = getEntityToClone.apply(entity);
    		if(result == null) {
    			if(entityOps.isPrimitive()) {
    				result = entityOps.clone(entity);
    			} else {    			
    				result = entityOps.newInstance();
    			}
    			
        		if(result == null) {
        			throw new RuntimeException("Could not obtain a clone for " + entity.getClass().getName() + ": " +  entity);
        		}
    			
    			putEntityToClone.accept(entity, result);
    		}
    		
    		
	    	boolean needsCopy = result != entity && (result == null || !getIsCopied.test(entity));
	
	    	if(needsCopy) {
	    		setIsCopied.accept(entity);
	    		    		
	        	for(PropertyOps propertyOps : entityOps.getEditableProperties()) {
	        		Object val = propertyOps.getValue(entity);
	        		
	        		Object newVal = deepCopy(val, classToOps, getEntityToClone, putEntityToClone, getIsCopied, setIsCopied);
	        		
	        		System.out.println("Setting " + result + "." + propertyOps.getName() + " := " + newVal);
	        		propertyOps.setValue(result, newVal);
	        	}
	        	
	        	if(entityOps.isCollection()) {
	        		Iterator<?> it = entityOps.getItems(entity);
	        		entityOps.setItems(result, it);
	        		
	        	}
	    	}
	    	
    	}
    	
    	return result;
    }
        
    

    public static void copy(EntityOps sourceOps, EntityOps targetOps, Object fromEntity, Object toEntity) {
        for(PropertyOps toOps : targetOps.getProperties()) {
            String name = toOps.getName();
            PropertyOps fromOps = sourceOps.getProperty(name);
            
            Object value = fromOps.getValue(fromEntity);
            toOps.setValue(toEntity, value);
        }
    }
}
