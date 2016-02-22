package org.kidayi.mybatisPlugins.Pagination;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

public class DBUtils {
	private static final Log log = LogFactory.getLog(DBUtils.class);
	
	public static void close(AutoCloseable obj){
		if(obj!=null){
			try {
				obj.close();
			} catch (Throwable e) {
				log.error("error", e);
			}
		}
	}
}
