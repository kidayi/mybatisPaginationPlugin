package org.kidayi.mybatisPlugins.Pagination;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.jdbc.ConnectionLogger;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Intercepts({ @Signature(method = "update", type = Executor.class, 
args = { MappedStatement.class,Object.class}) })
public class BatchUpdateInterceptor implements Interceptor {
	private static final Logger logger=LoggerFactory.getLogger(BatchUpdateInterceptor.class);
	
	private Properties properties;
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if(invocation.getTarget() instanceof Executor){
			Object[] objs= invocation.getArgs();
			MappedStatement ms=(MappedStatement)objs[0];
			Object parameter=objs[1];
			
			if(parameter instanceof IBatchUpdate){
				SimpleExecutor se=(SimpleExecutor)invocation.getTarget();
				IBatchUpdate batchParamter=(IBatchUpdate)parameter;
				List<Object> exampleList=batchParamter.getExampleList();
				if(CollectionUtils.isEmpty(exampleList)){
					throw new Exception("batch update error : parameterList in empty");
				}
				
				Statement stmt = null;
				try {
					Configuration configuration = ms.getConfiguration();
					StatementHandler handler = configuration.newStatementHandler(
							se, ms, exampleList.get(0), RowBounds.DEFAULT, null, null);
					stmt =prepareStatement(handler, ms.getStatementLog(),se);
					logger.debug("batch update sql:"+handler.getBoundSql().getSql());
					
					if(stmt instanceof PreparedStatement){
						PreparedStatement ps=(PreparedStatement)stmt;
						ps.clearBatch();
						for (Object object : exampleList) {
							ps.clearParameters();
							logger.debug("batch update paramter:");
							Tools.setParameters(ps, ms, handler.getBoundSql(), object);
							ps.addBatch();
						}
						int[] rows=stmt.executeBatch();
						
						return Tools.getAllRows(rows);
					}else{
						throw new Exception("batch update paramterType error :"+stmt.getClass());
					}
				} finally {
					closeStatement(stmt);
				}
			}
		}

		return invocation.proceed();
	}
	
	private Statement prepareStatement(StatementHandler handler,
			Log statementLog,SimpleExecutor se) throws SQLException {
		Statement stmt;
		Connection connection = getConnection(statementLog,se);
		stmt = handler.prepare(connection);
		handler.parameterize(stmt);
		return stmt;
	}
	
	protected Connection getConnection(Log statementLog, SimpleExecutor se)
			throws SQLException {
		Connection connection = se.getTransaction().getConnection();
		if (statementLog.isDebugEnabled()) {
			return ConnectionLogger.newInstance(connection, statementLog);
		} else {
			return connection;
		}
	}
	
	private void closeStatement(Statement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				logger.error("error", e);
			}
		}
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
