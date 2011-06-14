//$Id: SchemaExport.java,v 1.4 2005/03/15 14:55:46 steveebersole Exp $
package org.hibernate.tool.hbm2ddl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.util.JDBCExceptionReporter;
import org.hibernate.util.ReflectHelper;

/**
 * Commandline tool to export table schema to the database. This
 * class may also be called from inside an application.
 *
 * @author Daniel Bradby, Gavin King
 */
public class SchemaExport {

	private static final Log log = LogFactory.getLog(SchemaExport.class);

	private ConnectionHelper connectionHelper;
	private String[] dropSQL;
	private String[] createSQL;
	private String outputFile = null;
	private Dialect dialect;
	private String delimiter;
    private List exceptions;

	/**
	 * Create a schema exporter for the given Configuration
	 */
	public SchemaExport(Configuration cfg) throws HibernateException {
		this( cfg, cfg.getProperties() );
	}

	/**
	 * Create a schema exporter for the given Configuration, with the given
	 * database connection properties.
	 */
	public SchemaExport(Configuration cfg, Properties connectionProperties) throws HibernateException {
		dialect = Dialect.getDialect( connectionProperties );

		Properties props = new Properties();
		props.putAll( dialect.getDefaultProperties() );
		props.putAll( connectionProperties );

		connectionHelper = new ProviderConnectionHelper( props );
		dropSQL = cfg.generateDropSchemaScript( dialect );
		createSQL = cfg.generateSchemaCreationScript( dialect );
        exceptions = new ArrayList();
	}

	public SchemaExport(Configuration cfg, Connection connection) {
		this.connectionHelper = new SuppliedConnectionHelper( connection );
		// TODO : this is another place where guessing the dialect based on connection metadata would be nice
		dialect = Dialect.getDialect( cfg.getProperties() );
		dropSQL = cfg.generateDropSchemaScript( dialect );
		createSQL = cfg.generateSchemaCreationScript( dialect );
        exceptions = new ArrayList();
	}

	/**
	 * Set an output filename. The generated script will be written to this file.
	 */
	public SchemaExport setOutputFile(String filename) {
		outputFile = filename;
		return this;
	}

	/**
	 * Set the end of statement delimiter
	 */
	public SchemaExport setDelimiter(String delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	/**
	 * Run the schema creation script.
	 * @param script print the DDL to the console
	 * @param export export the script to the database
	 */
	public void create(boolean script, boolean export) {
		execute(script, export, false, true);
	}

	/**
	 * Run the drop schema script.
	 * @param script print the DDL to the console
	 * @param export export the script to the database
	 */
	public void drop(boolean script, boolean export) {
		execute(script, export, true, true);
	}

	private void execute(boolean script, boolean export, boolean justDrop, boolean format) {

		log.info("Running hbm2ddl schema export");

		Connection connection = null;
		FileWriter fileOutput = null;
		Statement statement = null;

        exceptions.clear();

		try {

			if(outputFile != null) {
				log.info("writing generated schema to file: " + outputFile);
				fileOutput = new FileWriter(outputFile);
			}

			if (export) {
				log.info("exporting generated schema to database");
				connection = connectionHelper.getConnection();
				if ( !connection.getAutoCommit() ) {
					connection.commit();
					connection.setAutoCommit(true);
				}
				statement = connection.createStatement();
			}

			for (int i = 0; i < dropSQL.length; i++) {
				try {
					String formatted = dropSQL[i];
					if (delimiter!=null) formatted += delimiter;
					if (script) System.out.println(formatted);
					log.debug(formatted);
					if (outputFile != null) fileOutput.write( formatted + "\n" );
					if (export) statement.executeUpdate( dropSQL[i] );
				}
				catch(SQLException e) {
                    exceptions.add(e);
					log.debug( "Unsuccessful: " + dropSQL[i] );
					log.debug( e.getMessage() );
				}

			}

			if (!justDrop) {
				for(int j = 0; j < createSQL.length; j++) {
					try {
						String formatted = format ? format( createSQL[j] ) : createSQL[j];
						if (delimiter!=null) formatted += delimiter;
						if (script) System.out.println(formatted);
						log.debug(formatted);
						if (outputFile != null) fileOutput.write( formatted + "\n" );
						if (export) statement.executeUpdate( createSQL[j] );
					}
					catch (SQLException e) {
                        exceptions.add(e);
						log.error( "Unsuccessful: " + createSQL[j] );
						log.error( e.getMessage() );
					}
				}
			}

			log.info("schema export complete");

		}

		catch(Exception e) {
            exceptions.add(e);
			log.error("schema export unsuccessful", e);
		}

		finally {

			try {
				if (statement!=null) statement.close();
				if (connection!=null) connectionHelper.release();
			}
			catch(Exception e) {
                exceptions.add(e);
				log.error( "Could not close connection", e );
			}

			try {
				if (fileOutput!=null) fileOutput.close();
			}
			catch (IOException ioe) {
                exceptions.add(ioe);
				log.error( "Error closing output file: " + outputFile, ioe );
			}

		}
	}

	/**
	 * Format an SQL statement using simple rules:
	 *  a) Insert newline after each comma;
	 *  b) Indent three spaces after each inserted newline;
	 * If the statement contains single/double quotes return unchanged,
	 * it is too complex and could be broken by simple formatting.
	 */
	private static String format(String sql) {

		if ( sql.indexOf("\"") > 0 || sql.indexOf("'") > 0) {
			return sql;
		}

		String formatted;

		if ( sql.toLowerCase().startsWith("create table") ) {

			StringBuffer result = new StringBuffer(60);
			StringTokenizer tokens = new StringTokenizer( sql, "(,)", true );

			int depth = 0;

			while ( tokens.hasMoreTokens() ) {
				String tok = tokens.nextToken();
				if ( ")".equals(tok) ) {
					depth--;
					if (depth==0) result.append("\n");
				}
				result.append(tok);
				if ( ",".equals(tok) && depth==1 ) result.append("\n  ");
				if ( "(".equals(tok) ) {
					depth++;
					if (depth==1) result.append("\n   ");
				}
			}

			formatted = result.toString();

		}
		else {
			formatted = sql;
		}

		return formatted;
	}

	public static void main(String[] args) {
		try {
			Configuration cfg = new Configuration();

			boolean script = true;
			boolean drop = false;
			boolean export = true;
			String outFile = null;
			String propFile = null;
			boolean formatSQL = false;
			String delim = null;

			for ( int i=0; i<args.length; i++ )  {
				if( args[i].startsWith("--") ) {
					if( args[i].equals("--quiet") ) {
						script = false;
					}
					else if( args[i].equals("--drop") ) {
						drop = true;
					}
					else if( args[i].equals("--text") ) {
						export = false;
					}
					else if( args[i].startsWith("--output=") ) {
						outFile = args[i].substring(9);
					}
					else if( args[i].startsWith("--properties=") ) {
						propFile = args[i].substring(13);
					}
					else if( args[i].equals("--format") ) {
						formatSQL = true;
					}
					else if ( args[i].startsWith("--delimiter=") ) {
						delim = args[i].substring(12);
					}
					else if ( args[i].startsWith("--config=") ) {
						cfg.configure( args[i].substring(9) );
					}
					else if ( args[i].startsWith("--naming=") ) {
						cfg.setNamingStrategy(
							(NamingStrategy) ReflectHelper.classForName( args[i].substring(9) ).newInstance()
						);
					}
				}
				else {
					String filename = args[i];
					if ( filename.endsWith( ".jar" ) ) {
						cfg.addJar( new File(filename) );
					}
					else {
						cfg.addFile(filename);
					}
				}

			}
			if(propFile!=null) {
				Properties props = new Properties();
				props.load( new FileInputStream(propFile) );
				new SchemaExport(cfg, props)
					.setOutputFile(outFile)
					.setDelimiter(delim)
					.execute(script, export, drop, formatSQL);
			}
			else {
				new SchemaExport(cfg)
					.setOutputFile(outFile)
					.setDelimiter(delim)
					.execute(script, export, drop, formatSQL);
			}
		}
		catch(Exception e) {
			log.error( "Error creating schema ", e );
			e.printStackTrace();
		}
	}

    /**
     * Returns a List of all Exceptions which occured during the export.
     * @return A List containig the Exceptions occured during the export
     */
    public List getExceptions() {
        return exceptions;
    }

	private abstract class ConnectionHelper {
		abstract Connection getConnection() throws SQLException;
		abstract void release() throws SQLException;

		protected final void logAnyWarnings(Connection connection) {
			try {
				JDBCExceptionReporter.logWarnings( connection.getWarnings() );
				connection.clearWarnings();
			}
			catch(SQLException sqle) {
				exceptions.add( sqle );
			}
		}
	}

	private class SuppliedConnectionHelper extends ConnectionHelper {
		private Connection connection;

		public SuppliedConnectionHelper(Connection connection) {
			this.connection = connection;
		}

		Connection getConnection() {
			return connection;
		}

		void release() {
			logAnyWarnings( connection );
			connection = null;
		}
	}

	private class ProviderConnectionHelper extends ConnectionHelper {
		private Properties cfgProperties;
		private ConnectionProvider connectionProvider;
		private Connection connection;

		public ProviderConnectionHelper(Properties cfgProperties) {
			this.cfgProperties = cfgProperties;
		}

		public Connection getConnection() throws SQLException {
			if ( connection == null ) {
				connectionProvider = ConnectionProviderFactory.newConnectionProvider( cfgProperties );
				connection = connectionProvider.getConnection();
				if ( !connection.getAutoCommit() ) {
					connection.commit();
					connection.setAutoCommit( true );
				}
			}
			return connection;
		}

		public void release() throws SQLException {
			if ( connection!=null ) {
				logAnyWarnings( connection );
				connectionProvider.closeConnection( connection );
				connectionProvider.close();
			}
		}
	}
}







