//$Id: AbstractBatcher.java,v 1.15 2005/03/21 09:54:25 maxcsaucdk Exp $
package org.hibernate.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.util.GetGeneratedKeysHelper;
import org.hibernate.util.JDBCExceptionReporter;

/**
 * Manages prepared statements and batching.
 *
 * @author Gavin King
 */
public abstract class AbstractBatcher implements Batcher {

	private static int globalOpenPreparedStatementCount;
	private static int globalOpenResultSetCount;

	private int openPreparedStatementCount;
	private int openResultSetCount;

	protected static final Log log = LogFactory.getLog(AbstractBatcher.class);
	protected static final Log SQL_LOG = LogFactory.getLog("org.hibernate.SQL");

	private final JDBCContext jdbcContext;
	private final SessionFactoryImplementor factory;

	private PreparedStatement batchUpdate;
	private String batchUpdateSQL;

	private HashSet statementsToClose = new HashSet();
	private HashSet resultSetsToClose = new HashSet();
	private PreparedStatement lastQuery;

	public AbstractBatcher(JDBCContext jdbcContext) {
		this.jdbcContext = jdbcContext;
		this.factory = jdbcContext.getFactory();
	}

	protected PreparedStatement getStatement() {
		return batchUpdate;
	}

	public CallableStatement prepareCallableStatement(String sql) 
	throws SQLException, HibernateException {
		executeBatch();
		logOpenPreparedStatement();
		return getCallableStatement( jdbcContext.connection(), sql, false);
	}

	public PreparedStatement prepareStatement(String sql) 
	throws SQLException, HibernateException {
		return prepareStatement(sql, false);
	}

	public PreparedStatement prepareStatement(String sql, boolean getGeneratedKeys) 
	throws SQLException, HibernateException {
		executeBatch();
		logOpenPreparedStatement();
		return getPreparedStatement( jdbcContext.connection(), sql, false, getGeneratedKeys, null, false );
	}

	public PreparedStatement prepareSelectStatement(String sql) 
	throws SQLException, HibernateException {
		logOpenPreparedStatement();
		return getPreparedStatement( jdbcContext.connection(), sql, false, false, null, false );
	}

	public PreparedStatement prepareQueryStatement(String sql, boolean scrollable, ScrollMode scrollMode) 
	throws SQLException, HibernateException {
		logOpenPreparedStatement();
		PreparedStatement ps = getPreparedStatement( jdbcContext.connection(), sql, scrollable, scrollMode );
		setStatementFetchSize(ps);
		statementsToClose.add(ps);
		lastQuery=ps;
		return ps;
	}

	public CallableStatement prepareCallableQueryStatement(String sql, boolean scrollable, ScrollMode scrollMode) 
	throws SQLException, HibernateException {
		logOpenPreparedStatement();
		CallableStatement ps = (CallableStatement) getPreparedStatement(jdbcContext.connection(), sql, scrollable, false, scrollMode, true);
		setStatementFetchSize(ps);
		statementsToClose.add(ps);
		lastQuery=ps;
		return ps;
	}

	public void abortBatch(SQLException sqle) {
		try {
			if (batchUpdate!=null) closeStatement(batchUpdate);
		}
		catch (SQLException e) {
			//noncritical, swallow and let the other propagate!
			JDBCExceptionReporter.logExceptions(e);
		}
		finally {
			batchUpdate=null;
			batchUpdateSQL=null;
		}
	}

	public ResultSet getResultSet(PreparedStatement ps) throws SQLException {
		ResultSet rs = ps.executeQuery();
		resultSetsToClose.add(rs);
		logOpenResults();
		return rs;
	}

	public ResultSet getResultSet(CallableStatement ps, Dialect dialect) throws SQLException {
		// TODO: maybe controlled by dialect...this works under oracle.
		ResultSet rs = dialect.getResultSet(ps);
		logOpenResults();
		return rs;
		
	}
	public void closeQueryStatement(PreparedStatement ps, ResultSet rs) throws SQLException {
		statementsToClose.remove(ps);
		if (rs!=null) resultSetsToClose.remove(rs);
		try {
			if (rs!=null) {
				logCloseResults();
				rs.close();
			}
		}
		finally {
			closeQueryStatement(ps);
		}
	}

	public PreparedStatement prepareBatchStatement(String sql) 
	throws SQLException, HibernateException {
		if ( !sql.equals(batchUpdateSQL) ) {
			batchUpdate=prepareStatement(sql); // calls executeBatch()
			batchUpdateSQL=sql;
		}
		else {
			log.debug("reusing prepared statement");
			log(sql);
		}
		return batchUpdate;
	}

	public CallableStatement prepareBatchCallableStatement(String sql) 
	throws SQLException, HibernateException {
		if ( !sql.equals(batchUpdateSQL) ) { // TODO: what if batchUpdate is a callablestatement ?
			batchUpdate=prepareCallableStatement(sql); // calls executeBatch()
			batchUpdateSQL=sql;
		}
		return (CallableStatement)batchUpdate;
	}


	public void executeBatch() throws HibernateException {
		if (batchUpdate!=null) {
			try {
				try {
					doExecuteBatch(batchUpdate);
				}
				finally {
					closeStatement(batchUpdate);
				}
			}
			catch (SQLException sqle) {
				throw JDBCExceptionHelper.convert(
				        factory.getSQLExceptionConverter(),
				        sqle,
				        "Could not execute JDBC batch update",
				        batchUpdateSQL
				);
			}
			finally {
				batchUpdate=null;
				batchUpdateSQL=null;
			}
		}
	}

	public void closeStatement(PreparedStatement ps) throws SQLException {
		logClosePreparedStatement();
		closePreparedStatement(ps);
	}

	private void closeQueryStatement(PreparedStatement ps) throws SQLException {

		try {
			//work around a bug in all known connection pools....
			if ( ps.getMaxRows()!=0 ) ps.setMaxRows(0);
			if ( ps.getQueryTimeout()!=0 ) ps.setQueryTimeout(0);
		}
		catch (Exception e) {
			log.warn("exception clearing maxRows/queryTimeout", e);
			ps.close(); //just close it; do NOT try to return it to the pool!
			return; //NOTE: early exit!
		}
		

		closeStatement(ps);
		if ( lastQuery==ps ) lastQuery = null;
		
	}

	public void closeStatements() {
		try {
			if (batchUpdate!=null) batchUpdate.close();
		}
		catch (SQLException sqle) {
			//no big deal
			log.warn("Could not close a JDBC prepared statement", sqle);
		}
		batchUpdate=null;
		batchUpdateSQL=null;

		Iterator iter = resultSetsToClose.iterator();
		while ( iter.hasNext() ) {
			try {
				logCloseResults();
				( (ResultSet) iter.next() ).close();
			}
			catch (SQLException e) {
				// no big deal
				log.warn("Could not close a JDBC result set", e);
			}
		}
		resultSetsToClose.clear();

		iter = statementsToClose.iterator();
		while ( iter.hasNext() ) {
			try {
				closeQueryStatement( (PreparedStatement) iter.next() );
			}
			catch (SQLException e) {
				// no big deal
				log.warn("Could not close a JDBC statement", e);
			}
		}
		statementsToClose.clear();
	}

	protected abstract void doExecuteBatch(PreparedStatement ps) throws SQLException, HibernateException;
	
	private String preparedStatementCountsToString() {
		return
				" (open PreparedStatements: " + 
				openPreparedStatementCount + 
				", globally: " +
				globalOpenPreparedStatementCount +
				")";
	}

	private String resultSetCountsToString() {
		return
				" (open ResultSets: " + 
				openResultSetCount + 
				", globally: " +
				globalOpenResultSetCount +
				")";
	}

	private void logOpenPreparedStatement() {
		if ( log.isDebugEnabled() ) {
			log.debug( "about to open PreparedStatement" + preparedStatementCountsToString() );
			openPreparedStatementCount++;
			globalOpenPreparedStatementCount++;
		}
	}

	private void logClosePreparedStatement() {
		if ( log.isDebugEnabled() ) {
			log.debug( "about to close PreparedStatement" + preparedStatementCountsToString() );
			openPreparedStatementCount--;
			globalOpenPreparedStatementCount--;
		}
	}

	private void logOpenResults() {
		if ( log.isDebugEnabled() ) {
			log.debug( "about to open ResultSet" + resultSetCountsToString() );
			openResultSetCount++;
			globalOpenResultSetCount++;
		}
	}
	private void logCloseResults() {
		if ( log.isDebugEnabled() ) {
			log.debug( "about to close ResultSet" + resultSetCountsToString() );
			openResultSetCount--;
			globalOpenResultSetCount--;
		}
	}

	protected SessionFactoryImplementor getFactory() {
		return factory;
	}
	
	private void log(String sql) {
		SQL_LOG.debug(sql);
		if ( factory.getSettings().isShowSqlEnabled() ) System.out.println("Hibernate: " + sql);
	}

	private PreparedStatement getPreparedStatement(
			final Connection conn, 
			final String sql, 
			final boolean scrollable, 
			final ScrollMode scrollMode)
	throws SQLException {
		return getPreparedStatement(conn, sql, scrollable, false, scrollMode, false);
	}

	private CallableStatement getCallableStatement( final Connection conn, 
			final String sql, 
			boolean scrollable)
	throws SQLException {

		if ( scrollable && !factory.getSettings().isScrollableResultSetsEnabled() ) {
			throw new AssertionFailure("scrollable result sets are not enabled");
		}

		log(sql);
		
		log.trace("preparing callable statement");
		if (scrollable) {
			return conn.prepareCall(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		}
		else {
			return conn.prepareCall(sql);
		}

	}

	private PreparedStatement getPreparedStatement(
			final Connection conn, 
			final String sql, 
			boolean scrollable, 
			final boolean useGetGeneratedKeys, 
			final ScrollMode scrollMode,
			final boolean callable)
	throws SQLException {

		if ( scrollable && !factory.getSettings().isScrollableResultSetsEnabled() ) {
			throw new AssertionFailure("scrollable result sets are not enabled");
		}

		if ( useGetGeneratedKeys && !factory.getSettings().isGetGeneratedKeysEnabled() ) {
			throw new AssertionFailure("getGeneratedKeys() support is not enabled");
		}

		log(sql);

		try {
			log.trace("preparing statement");
			if (scrollable) {
				if (callable) {
					return conn.prepareCall( sql, scrollMode.toResultSetType(), ResultSet.CONCUR_READ_ONLY );
				} else {
					return conn.prepareStatement( sql, scrollMode.toResultSetType(), ResultSet.CONCUR_READ_ONLY );
				}
			}
			else if (useGetGeneratedKeys) {
				return GetGeneratedKeysHelper.prepareStatement(conn, sql);
			}
			else {
				if(callable) {
					return conn.prepareCall(sql);
				} else {
					return conn.prepareStatement(sql);
				}
			}
		}
		catch (SQLException sqle) {
			JDBCExceptionReporter.logExceptions(sqle);
			throw sqle;
		}

	}

	private void closePreparedStatement(PreparedStatement ps) throws SQLException {
		//try {
			log.trace("closing statement");
			ps.close();
		/*}
		catch (SQLException sqle) {
			JDBCExceptionReporter.logExceptions(sqle);
			throw sqle;
		}*/

	}

	private void setStatementFetchSize(PreparedStatement statement) throws SQLException {
		Integer statementFetchSize = factory.getSettings().getJdbcFetchSize();
		if (statementFetchSize!=null) statement.setFetchSize( statementFetchSize.intValue() );
	}

	public Connection openConnection() throws HibernateException {
		log.debug("opening JDBC connection");
		try {
			return factory.getConnectionProvider().getConnection();
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					factory.getSQLExceptionConverter(),
					sqle,
					"Cannot open connection"
			);
		}
	}

	public void closeConnection(Connection conn) throws HibernateException {
		if ( log.isDebugEnabled() ) {
			log.debug( 
					"closing JDBC connection" + 
					preparedStatementCountsToString() + 
					resultSetCountsToString() 
			);
		}
		
		try {
			if ( !conn.isClosed() ) {
				try {
					JDBCExceptionReporter.logWarnings( conn.getWarnings() );
					conn.clearWarnings();
				}
				catch (SQLException sqle) {
					//workaround for WebLogic
					log.debug("could not log warnings", sqle);
				}
			}
			factory.getConnectionProvider().closeConnection(conn);
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					factory.getSQLExceptionConverter(),
					sqle,
					"Cannot close connection"
			);
		}
	}

	public void cancelLastQuery() throws HibernateException {
		try {
			if (lastQuery!=null) lastQuery.cancel();
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					factory.getSQLExceptionConverter(),
					sqle,
					"Cannot cancel query"
			);
		}
	}

}






