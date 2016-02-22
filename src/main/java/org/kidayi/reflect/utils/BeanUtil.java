package org.kidayi.reflect.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeanUtil {
	private static final Logger logger=LoggerFactory.getLogger(BeanUtil.class);
	
	public static <T,V> Map<T,V> listToMap(List<V> list,String keyFieldName,Class<T> keyFieldClass){
		Map<T,V> map=new HashMap<T,V>();
		
		if(CollectionUtils.isEmpty(list)){
			return map;
		}
	
		for (V v : list) {
			try {
				Object key=FieldUtils.getFieldValue(v, keyFieldName);
				map.put((T)key, v);
			} catch (Throwable e) {
				logger.error("error", e);
			}
			
		}
		return map;
	}
	
	public static <T,V> Map<T,List<V>> listToMapAndList(List<V> list,String keyFieldName,Class<T> keyFieldClass){
		Map<T,List<V>> map=new HashMap<T,List<V>>();
		
		if(CollectionUtils.isEmpty(list)){
			return map;
		}
	
		for (V v : list) {
			try {
				Object key=FieldUtils.getFieldValue(v, keyFieldName);
				List<V> valueList=map.get(key);
				if(valueList==null){
					valueList=new ArrayList<>();
					map.put((T)key, valueList);
				}
				valueList.add(v);
			} catch (Throwable e) {
				logger.error("error", e);
			}
			
		}
		return map;
	}
	
	public static <T,V> List<T> getFieldUniqueValueListByName (List<V> list,String fieldName,Class<T> fieldClass){
		Set<T> set=new HashSet<>();
		if(CollectionUtils.isEmpty(list)){
			return new ArrayList<>();
		}
		for (V v : list) {
			try {
				Object value=FieldUtils.getFieldValue(v, fieldName);
				if(!set.contains(value)){
					set.add((T)value);
				}
			} catch (Throwable e) {
				logger.error("error", e);
			}
		}
		return new ArrayList<>(set);
	}
}
