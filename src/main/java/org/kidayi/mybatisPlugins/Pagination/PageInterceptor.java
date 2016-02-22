package org.kidayi.mybatisPlugins.Pagination;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Distinct;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.kidayi.reflect.utils.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Intercepts({ @Signature(method = "query", type = Executor.class, 
	args = { MappedStatement.class,Object.class,RowBounds.class,ResultHandler.class}) })
public class PageInterceptor implements Interceptor {
	private static final Logger logger=LoggerFactory.getLogger(PageInterceptor.class);
	
	private Properties properties;
	/**
	 * 拦截后要执行的方法
	 */
	public Object intercept(Invocation invocation) throws Throwable {
		if (invocation.getTarget() instanceof Executor) {
			Executor be = (Executor) invocation.getTarget();
			Object[] objs = invocation.getArgs();
			MappedStatement ms = (MappedStatement) objs[0];
			Object parameter = objs[1];
			RowBounds rowBounds = (RowBounds) objs[2];
			ResultHandler resultHandler = (ResultHandler) objs[3];

			if (parameter instanceof IPage) {
				return processIPage(be, ms, (IPage) parameter, rowBounds,
						resultHandler);
			} else if (parameter instanceof IAppendLimit) {
				return processLimitSize(be, ms, (IAppendLimit) parameter,
						rowBounds, resultHandler);
			}

		}
		return invocation.proceed();
		
	}
	
	private Object processLimitSize(Executor be,MappedStatement ms,
			IAppendLimit parameter,RowBounds rowBounds,
			ResultHandler resultHandler) throws Throwable{
		BoundSql boundSql=ms.getBoundSql(parameter.getExample());
		String sqlOriginal=boundSql.getSql();
		StringBuilder sbd=new StringBuilder(sqlOriginal);
		String sqlLimit=sbd.append(" limit ").append(parameter.getLimitSize().getRows()).toString();
		FieldUtils.setFieldValue(boundSql, "sql", sqlLimit);
		CacheKey key = be.createCacheKey(ms, parameter.getExample(), rowBounds, boundSql);
	    return be.query(ms, parameter.getExample(), rowBounds, resultHandler, key, boundSql);
	}
	
	private Object processIPage(Executor be,MappedStatement ms,
			IPage parameter,RowBounds rowBounds,
			ResultHandler resultHandler)  throws Throwable {
		BoundSql boundSql=ms.getBoundSql(parameter.getExample());
		String sqlOriginal=boundSql.getSql();
		String sqlCount=getCountSql(sqlOriginal);
		
		Connection conn=be.getTransaction().getConnection();
		PreparedStatement ps=null;
		ResultSet rs=null;
		long count=0;
		try{
			ps=conn.prepareStatement(sqlCount);
			Tools.setParameters(ps, ms, boundSql, parameter.getExample());
			rs=ps.executeQuery();
			if(rs.next()){
				count=rs.getLong(1);
			}
		}catch(Throwable ex){
			logger.error("error", ex);
		}finally{
			DBUtils.close(rs);
			DBUtils.close(ps);
		}
		
		PageSize pageSize=parameter.getPageSize();
		pageSize.setCount(count);
		pageSize.pageCalculate();
		
		logger.debug("pageSize:"+pageSize);
		
		String pageSql=getPageSql(sqlOriginal, pageSize.getStart(), pageSize.getPageSize());
		
		FieldUtils.setFieldValue(boundSql, "sql", pageSql);
		
		CacheKey key = be.createCacheKey(ms, parameter.getExample(), rowBounds, boundSql);
	    return be.query(ms, parameter.getExample(), rowBounds, resultHandler, key, boundSql);
	}
	
	private String getPageSql(String sqlOriginal,long start,long pageSize){
		StringBuilder sbd=new StringBuilder(sqlOriginal);
		sbd.append(" limit ").append(start).append(",").append(pageSize);
		return sbd.toString();
	}
	
	private String getCountSql(String sqlOriginal){
		String countSql=null;
		try{
			countSql=parseCountSql(sqlOriginal);
		}catch(Throwable ex){
			logger.error("error", ex);
		}
		
		if(StringUtils.isBlank(countSql)){
			countSql=subCountSql(sqlOriginal);
		}
		return countSql;
	}
	
	private String parseCountSql(String sqlOriginal) throws JSQLParserException{
		Statement stmt = CCJSqlParserUtil.parse(sqlOriginal);
		if(stmt instanceof Select){
			Select selectStatement=(Select)stmt;
			SelectBody body=selectStatement.getSelectBody();
			if(body instanceof PlainSelect){
				PlainSelect ps=(PlainSelect)body;
				Distinct distinct=ps.getDistinct();
				List<SelectItem> itemList=ps.getSelectItems();
				
				SelectExpressionItem countSei=new SelectExpressionItem();
				Function f=new Function();
				f.setName("count");
				if(distinct==null){
					f.setAllColumns(true);
				}else if(CollectionUtils.isEmpty(itemList)){
					f.setAllColumns(true);
				}else{
					f.setDistinct(true);
					ExpressionList el=new ExpressionList();
					List<Expression> expList=new ArrayList<>(itemList.size());
					for (SelectItem item : itemList) {
						if(item instanceof SelectExpressionItem){
							SelectExpressionItem selectItem=(SelectExpressionItem)item;
							Column c=new Column();
							c.setColumnName(selectItem.getExpression().toString());
							expList.add(getIfNullExp(c));
						}else{
							Column c=new Column();
							c.setColumnName(item.toString());
							expList.add(c);
						}
					}
					el.setExpressions(expList);
					f.setParameters(el);
				}
				countSei.setExpression(f);
				List<SelectItem> countSelectList=new ArrayList<>(1);
				countSelectList.add(countSei);
				ps.setSelectItems(countSelectList);
				ps.setDistinct(null);
				String sqlCount=ps.toString();
				logger.debug("parseCountSql sqlCount:"+sqlCount);
				return sqlCount;
			}
		}
		return null;
	}
	
	private Function getIfNullExp(Column c){
		Function ifNullFuction=new Function();
		ifNullFuction.setName("ifnull");
		ExpressionList ifNullParam=new ExpressionList();
		List<Expression> ifNullExpList=new ArrayList<>(2);
		ifNullExpList.add(c);
		ifNullExpList.add(new LongValue(1));
		ifNullParam.setExpressions(ifNullExpList);
		ifNullFuction.setParameters(ifNullParam);
		return ifNullFuction;
	}
	
	private String subCountSql(String sqlOriginal){
		String sqlLowerCase = sqlOriginal.toLowerCase();
		int index=sqlLowerCase.indexOf("from");
		String sqlCount="select count(*) "+sqlOriginal.substring(index);
		logger.debug("subCountSql sqlCount:"+sqlCount);
		return sqlCount;
	}

	public Object plugin(Object target) {
		Object obj=Plugin.wrap(target, this);
		logger.debug(obj.getClass().toString());
		return obj;
	}


	@Override
	public void setProperties(Properties properties) {
		this.properties=properties;
		
	}

}
