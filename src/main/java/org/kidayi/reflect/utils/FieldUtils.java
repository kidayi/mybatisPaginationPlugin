package org.kidayi.reflect.utils;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldUtils {
	private static final Logger logger=LoggerFactory.getLogger(FieldUtils.class);
	
	public static Object getFieldValue(Object obj, String fieldName) throws IllegalArgumentException, IllegalAccessException {  
       Field field=getField(obj,fieldName);
       
       if (field != null) {  
    	   field.setAccessible(true);
    	   return field.get(obj);
        }  
       
        return null;  
    } 
	
	private static Field getField(Object obj, String fieldName) {  
        Field field = null;  
       for (Class<?> clazz=obj.getClass(); clazz != Object.class; clazz=clazz.getSuperclass()) {  
           try {  
               field = clazz.getDeclaredField(fieldName);  
               break;  
           } catch (NoSuchFieldException e) {  
        	   logger.error("error", e);
           }  
        }  
        return field;  
    }  

    public static void setFieldValue(Object obj, String fieldName,  
           String fieldValue) {  
        Field field = getField(obj, fieldName);  
        if (field != null) {  
           try {  
               field.setAccessible(true);  
               field.set(obj, fieldValue);  
           } catch (IllegalArgumentException e) {  
               logger.error("error", e);
           } catch (IllegalAccessException e) {  
        	   logger.error("error", e);
           }  
        }  
     }
}
