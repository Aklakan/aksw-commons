package org.aksw.commons.sparql.api.cache.extra;

import org.aksw.commons.sparql.api.cache.core.QueryString;
import org.aksw.commons.util.strings.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.*;
import java.util.Arrays;
import java.util.GregorianCalendar;



/**
 * The class is used to cache information about resources to a database.
 * Provides the connection to an H2 database in a light weight, configuration free
 * manner.
 *
 * Important: This implementation of the CacheCoreEx interface uses combined hashes of services and
 * queries.
 *
 *
 * @author Jens Lehmann, Claus Stadler
 *
 */
public class CacheCoreH2
    extends SqlDaoBase
    implements CacheCore, CacheCoreEx
{
    private static final Logger logger = LoggerFactory.getLogger(CacheCoreH2.class);

    
    private String defaultService = "";

    @Override
    public CacheEntry lookup(String queryString) {
        return lookup(defaultService, queryString);
    }

    @Override
    public void write(String queryString, InputStream in) {
        write(defaultService, queryString, in);
    }

    enum Query
        implements QueryString
    {
        CREATE("CREATE TABLE IF NOT EXISTS query_cache(query_hash BINARY PRIMARY KEY, query_string VARCHAR(15000), data CLOB, time TIMESTAMP)"),
        LOOKUP("SELECT * FROM query_cache WHERE query_hash=? LIMIT 1"),
        INSERT("INSERT INTO query_cache VALUES(?,?,?,?)"),
        UPDATE("UPDATE query_cache SET data=?, time=? WHERE query_hash=?"),
        ;

        private String queryString;

        Query(String queryString) { this.queryString = queryString; }
        public String getQueryString() { return queryString; }
    }


	private String databaseDirectory = "cache";
	private String databaseName = "extraction";
	private boolean autoServerMode = true;

	// specifies after how many milli seconds a cached result becomes invalid
	private long lifespan = 1 * 24 * 60 * 60 * 1000; // 1 day


	private Connection conn;


    public static CacheCoreH2 create(String dbName)
            throws ClassNotFoundException, SQLException
    {
        return create(true, "cache/sparql", dbName, 1 * 24 * 60 * 60 * 1000);
    }

    public static CacheCoreH2 create(String dbName, long lifespan)
            throws ClassNotFoundException, SQLException
    {
        return create(true, "cache/sparql", dbName, lifespan);
    }

    /**
     * Loads the driver
     */
    public static CacheCoreH2 create(boolean autoServerMode, String dbDir, String dbName, long lifespan)
            throws ClassNotFoundException, SQLException {
        return (CacheCoreH2)create(autoServerMode, dbDir, dbName, lifespan, false);
    }

    public static CacheCoreEx create(boolean autoServerMode, String dbDir, String dbName, long lifespan, boolean useCompression)
            throws ClassNotFoundException, SQLException
    {
            Class.forName("org.h2.Driver");

            String jdbcString = "";
            if(autoServerMode) {
                jdbcString = ";AUTO_SERVER=TRUE";
            }

            // connect to database (created automatically if not existing)
            Connection conn = DriverManager.getConnection("jdbc:h2:" + dbDir + "/" + dbName + jdbcString, "sa", "");

            // create cache table if it does not exist
            Statement stmt = conn.createStatement();

            CacheCoreH2 tmp = new CacheCoreH2(conn, lifespan);

        return useCompression
            ? new CacheCoreExBZip2(tmp)
            : tmp;
    }

    public static CacheCoreEx create(String dbName, long lifespan, boolean useCompression)
            throws ClassNotFoundException, SQLException
    {
        return create(true, "cache/sparql", dbName, lifespan, useCompression);
    }

    
	public CacheCoreH2(Connection conn, long lifespan)
            throws SQLException
    {
        super(Arrays.asList(Query.values()));

        try {
            conn.createStatement().executeUpdate(Query.CREATE.getQueryString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setConnection(conn);

        this.lifespan = lifespan;
    }

    public synchronized CacheEntry lookup(String service, String queryString)
    {
        try {
            return _lookup(service, queryString);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public CacheEntry _lookup(String service, String queryString)
            throws SQLException
    {
        String md5 = StringUtils.md5Hash(createHashRoot(service, queryString));
        //String md5 = StringUtils.md5Hash(queryString);

        ResultSet rs = executeQuery(Query.LOOKUP, md5);

        try {
            if(rs.next()) {
                Timestamp timestamp = rs.getTimestamp("time");
                Clob data = rs.getClob("data");

                return new CacheEntry(timestamp.getTime(), lifespan, new InputStreamProviderResultSetClob(rs, data));
            }

            if(rs.next()) {
                logger.warn("Multiple cache hits found, just one expected.");
            }
        } finally {
            SqlUtils.close(rs);
        }

        return null;
    }

    public synchronized void write(String service, String queryString, InputStream in) {
        try {
            _write(service, queryString, in);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void _write(String service, String queryString, InputStream in)
            throws SQLException
    {
        String md5 = StringUtils.md5Hash(createHashRoot(service, queryString));
        //String md5 = StringUtils.md5Hash(queryString);

        Timestamp timestamp = new Timestamp(new GregorianCalendar().getTimeInMillis());

        Reader reader = new InputStreamReader(in);

        ResultSet rs = null;
        try {
            rs = executeQuery(Query.LOOKUP, md5);

            if(rs != null && rs.next()) {
                execute(Query.UPDATE, null, reader, timestamp, md5);
            } else {
                execute(Query.INSERT, null, md5, queryString, reader, timestamp);
            }
        } finally {
            if(rs != null) {
                rs.close();
            }
        }

        //return lookup(queryString);
    }

    public String createHashRoot(String service, String queryString) {
        return service + queryString;
    }

}
