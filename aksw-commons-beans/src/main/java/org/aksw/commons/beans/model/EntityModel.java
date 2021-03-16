package org.aksw.commons.beans.model;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityModel
    implements EntityOps
{
    private static final Logger logger = LoggerFactory.getLogger(EntityModel.class);


    // Primitives can ONLY be initialized using .clone()
    protected boolean isPrimitive;

//	protected boolean isCollection;
//	protected Function<Object, Iterator<?>> getItemsFn;
//	protected BiConsumer<Object, Iterator<?>> setItemsFn;

    protected CollectionOps collectionOps = null;

    protected Class<?> associatedClass;

    protected Supplier<?> newInstance;
    protected Function<Object, ?> clone;
    protected Map<String, PropertyModel> propertyOps;

    //protected Map<String, PropertyModel> declaredPropertyModels;

    protected Function<Class<?>, Object> annotationFinder;
    //protected Set<Class<?>> annotationOverrides;

    protected Map<Class<?>, Object> classToInstance;

    protected ConversionService conversionService;
    //protected ClassToInstanceMap<Objcet

    public EntityModel() {
        this(null, null, null);
    }

    public EntityModel(Class<?> associatedClass, Supplier<?> newInstance,
            Map<String, PropertyModel> propertyOps) {
        super();
        this.associatedClass = associatedClass;
        this.newInstance = newInstance;
        this.propertyOps = propertyOps;

//        @SuppressWarnings("unchecked")
        this.annotationFinder = (annotationClass) -> MyAnnotationUtils.findAnnotation(this.associatedClass, (Class)annotationClass);
    }

    public ConversionService getConversionService() {
        return conversionService;
    }

    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    public Function<Class<?>, Object> getAnnotationFinder() {
        return annotationFinder;
    }

    public void setAnnotationFinder(Function<Class<?>, Object> annotationFinder) {
        this.annotationFinder = annotationFinder;
    }

    @Override
    public boolean isInstantiable() {
        boolean result = newInstance != null;
        return result;
    }

    @Override
    public Object newInstance() {
        Object result = newInstance == null ? null : newInstance.get();
        return result;
    }

    @Override
    public boolean isClonable() {
        boolean result = clone != null;
        return result;
    }

    public Object clone(Object o) {
        Object result = clone == null ? null : clone.apply(o);
        return result;
    }


    public Function<Object, ?> getClone() {
        return clone;
    }

    public void setClone(Function<Object, ?> clone) {
        this.clone = clone;
    }

    public Map<String, PropertyModel> getPropertyOps() {
        return propertyOps;
    }

    public Supplier<?> getNewInstance() {
        return newInstance;
    }

    public void setNewInstance(Supplier<?> newInstance) {
        this.newInstance = newInstance;
    }

    public void setPropertyOps(Map<String, PropertyModel> propertyOps) {
        this.propertyOps = propertyOps;
    }




    public static Constructor<?> tryGetCtor(Class<?> clazz, Class<?> ... args) {
        Constructor<?> result;
        try {
            result = clazz.getConstructor(args);
        } catch (NoSuchMethodException | SecurityException e) {
            result = null;
        }
        return result;
    }
    
    public static Collection<PropertyDescriptor> getAllPropertyDescriptors(Class<?> clazz) {
    	Set<Class<?>> classes = getAllInvolvedClasses(clazz);
    	
    	Set<PropertyDescriptor> result = new LinkedHashSet<>();
    	for(Class<?> c : classes) {
	        try {
	        	BeanInfo beanInfo = Introspector.getBeanInfo(c);
	        	PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
	        	result.addAll(Arrays.asList(pds));
	        } catch (IntrospectionException e) {
	        	throw new RuntimeException(e);
	        }
    	}
    	
    	return result;
    }

    @SuppressWarnings("unchecked")
	public static Set<Class<?>> getAllInvolvedClasses(Class<?> clazz) {
    	Set<Class<?>> result = new LinkedHashSet<>();
    	result.add(clazz);
    	
    	result.addAll(ClassUtils.getAllInterfaces(clazz));
    	result.addAll(ClassUtils.getAllSuperclasses(clazz));
    	

    	return result;
    }

    public static EntityModel createDefaultModel(Class<?> clazz, ConversionService conversionService) {
//    	PropertyDescriptor[] propertyDescriptors;
//		try {
//			propertyDescriptors = new ExtendedBeanInfoFactory().getBeanInfo(clazz).getPropertyDescriptors();
//		} catch (IntrospectionException e1) {
//			throw new RuntimeException(e1);
//		}
    	// The method for obtaining all property descriptors still sucks
    	// It seems even spring's BeanUtils and apache's BeanUtils do not provide
    	// a simple way to simply get all property descriptors of any class (even those
    	// that do not strictly satisfy the bean contract); but maybe I just overlooked something... 2019 Claus Stadler
    	Collection<PropertyDescriptor> propertyDescriptors = getAllPropertyDescriptors(clazz);
//               if(true) {
//        } else {
//	        try {
//	            beanInfo = Introspector.getBeanInfo(clazz);
//	        } catch (IntrospectionException e1) {
//	            throw new RuntimeException(e1);
//	        }
//            propertyDescriptors = beanInfo.getPropertyDescriptors();
//
//        	// Does not work for interfaces - BeanWrapper tries to create an instance
//        	//propertyDescriptors = new BeanWrapperImpl(clazz).getPropertyDescriptors();
//        }


        // Check if the entity can act as a collection (TODO: Delegate this check to a separate module)
        CollectionOps collectionOps = null;
        if(Map.class.isAssignableFrom(clazz)) {
            collectionOps = new CollectionOpsMap();
        } else if(Collection.class.isAssignableFrom(clazz)) {
            collectionOps = new CollectionOpsCollection();
        }


        boolean isSimple = clazz.isPrimitive();
        Function<Object, ?> copyCtorFn = null;
        Constructor<?> tmpCopyCtor = tryGetCtor(clazz);
        if(tmpCopyCtor == null) {
            Class<?> primitiveClass;
            try {
                primitiveClass = (Class<?>)clazz.getField("TYPE").get(null);
                isSimple = true;
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                primitiveClass = null;
            }

            if(primitiveClass != null) {
                tmpCopyCtor = tryGetCtor(clazz, primitiveClass);
            }
        }

        Constructor<?> copyCtor = tmpCopyCtor;
        if(copyCtor != null) {
            copyCtorFn = (x) -> {
                try {
                    Object result = copyCtor.newInstance(x);
                    return result;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }


        //long.class.


        //clazz.getConstructor(//parameterTypes)


        Map<String, PropertyModel> propertyOps = new HashMap<String, PropertyModel>();

        // TODO Add support for public fields

        for(PropertyDescriptor pd : propertyDescriptors) {
            Class<?> propertyType = pd.getPropertyType();
            String propertyName = pd.getName();

            Function<Object, Object> getter = null;
            Method readMethod = pd.getReadMethod();
            if(readMethod != null) {
                getter = (entity) -> {
                    try {
                        Object r = readMethod.invoke(entity);
                        return r;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }

            BiConsumer<Object, Object> setter = null;
            Method writeMethod = pd.getWriteMethod();
            
            // BeanInfo may only search for setters returning void
            // We allow setters returning arbiratry values
            if(writeMethod == null) {
            	try {
					writeMethod = clazz.getMethod("set" + StringUtils.capitalize(propertyName), propertyType);
				} catch (NoSuchMethodException | SecurityException e) {
					// Nothing to do
				}
            }
            
            if(writeMethod != null) {
            	Method tmp = writeMethod;
                setter = (entity, value) -> {
                    try {
                        tmp.invoke(entity, value);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to invoke " + tmp + " with " + (value == null ? null : value.getClass()) + " (" + value + ")", e);
                    }
                };
            }


            Function<Class<?>, Object> annotationFinder = (annotationClass) -> MyAnnotationUtils.findPropertyAnnotation(clazz, pd, (Class)annotationClass);

            PropertyModel p = new PropertyModel(propertyName, propertyType, getter, setter, conversionService, annotationFinder);
            p.setReadMethod(readMethod);
            p.setWriteMethod(writeMethod);

            propertyOps.put(propertyName, p);
        }

        EntityModel result = new EntityModel();
        result.setAssociatedClass(clazz);
        result.setClone(copyCtorFn);

        try {
            // Check if there is a defaultCtor
            Constructor<?> defaultCtor = clazz.getConstructor();

            result.setNewInstance(() -> {
                try {
                    return clazz.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (NoSuchMethodException e) {
            logger.debug("No constructor found on " + clazz.getName());
            // Ignore
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }

        result.setPropertyOps(propertyOps);
        result.setPrimitive(isSimple);

        result.setCollectionOps(collectionOps);


        return result;
    }

    @Override
    public String toString() {
        return "EntityOps [newInstance=" + newInstance + ", propertyOps="
                + propertyOps + "]";
    }

    @Override
    public Collection<? extends PropertyModel> getProperties() {
        Collection<? extends PropertyModel> result = propertyOps.values();
        return result;
    }

    @Override
    public PropertyModel getProperty(String name) {
        PropertyModel result = propertyOps.get(name);
        return result;
    }

    public void setAssociatedClass(Class<?> associatedClass) {
        this.associatedClass = associatedClass;
    }

    @Override
    public Class<?> getAssociatedClass() {
        return associatedClass;
    }

    @Override
    public <A> A findAnnotation(Class<A> annotationClass) {
        Object o = annotationFinder.apply(annotationClass);
        @SuppressWarnings("unchecked")
        A result = (A)o;
        return result;
    }

    @Override
    public <T> T getOps(Class<T> opsClass) {
        Object tmp = classToInstance.get(opsClass);

        T result = tmp == null ? null : (T)tmp;

        return result;
    }

    @Override
    public boolean isPrimitive() {
        return isPrimitive;
    }

    public void setPrimitive(boolean isSimple) {
        this.isPrimitive = isSimple;
    }

    @Override
    public boolean isCollection() {
        boolean result = collectionOps != null;
        return result;
    }

    public void setCollectionOps(CollectionOps collectionOps) {
        this.collectionOps = collectionOps;
    }

    public CollectionOps getCollectionOps() {
        return collectionOps;
    }

    @Override
    public Iterator<?> getItems(Object entity) {
        Iterator<?> result = collectionOps.getItems(entity);
        return result;
    }

    @Override
    public void setItems(Object entity, Iterator<?> items) {
        collectionOps.setItems(entity, items);
    }



}
