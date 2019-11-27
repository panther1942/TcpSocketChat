package cn.erika.plugins.jdbc;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdbcConnector {
    private static ComboPooledDataSource pool = new ComboPooledDataSource("sqlite_user");
    private static ResourceBundle res;

    static {
        res = ResourceBundle.getBundle("sql");
    }

    public static String getSql(String name) {
        return res.getString(name);
    }

    public static Connection getConn() throws SQLException {
        Connection conn = pool.getConnection();
        conn.setAutoCommit(false);
        return conn;
    }

    public static List<Model> executeQuery(String sql, String... args) throws IOException {
        String tableName = getTableNameFromSql(sql);

        Connection conn;
        PreparedStatement pStmt = null;
        ResultSet rs = null;
        try {
            conn = getConn();
        } catch (SQLException e) {
            throw new IOException("无法连接到数据库");
        }
        try {
            pStmt = createStatement(conn, sql, args);
            rs = pStmt.executeQuery();
            return Model.convert(tableName, rs);
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                System.err.println("回滚操作的过程发生异常: " + e.getMessage());
            }
            System.err.println("与数据库断开连接: " + e.getMessage());
        } finally {
            try {
                close(conn, pStmt, rs);
            } catch (SQLException e) {
                System.err.println("断开数据库的过程发生异常: " + e.getMessage());
            }
        }
        return null;
    }

    public static int executeUpdate(String sql, String... args) throws SQLException, IOException {
        Connection conn;
        PreparedStatement pStmt = null;
        try {
            conn = getConn();
        } catch (SQLException e) {
            throw new IOException("无法连接到数据库");
        }
        try {
            pStmt = createStatement(conn, sql, args);
            int flag = pStmt.executeUpdate();
            conn.commit();
            return flag;
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                System.err.println("回滚操作的过程发生异常: " + e.getMessage());
            }
            System.err.println(e.getMessage());
        } finally {
            try {
                close(conn, pStmt);
            } catch (SQLException e) {
                System.err.println("断开数据库的过程发生异常: " + e.getMessage());
            }
        }
        return -1;
    }

    private static PreparedStatement createStatement(Connection conn, String sql, String[] args) throws SQLException {
        PreparedStatement pStmt = conn.prepareStatement(sql);
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                pStmt.setString(i + 1, args[i]);
            }
        }
        return pStmt;
    }

    private static void close(Connection conn, Statement stmt, ResultSet rs) throws SQLException {
        if (rs != null) {
            rs.close();
        }
        close(conn, stmt);
    }

    private static void close(Connection conn, Statement stmt) throws SQLException {
        if (stmt != null) {
            stmt.close();
        }
        if (conn != null) {
            conn.close();
        }
    }

    private static String getTableNameFromSql(String sql) {
        sql = sql.replaceAll("\\s+", " ").replaceAll("`", "");

        String regexA = "^SELECT.*FROM\\s(.+)\\sWHERE.*";
        String regexB = "^SELECT.*FROM\\s(.+)";

        Pattern p = Pattern.compile(regexA, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        if (m.find()) {
            String[] tables = m.group(1).split(",");
            if (tables.length == 1) {
                return getTableName(tables[0]);
            } else {
                return null;
            }
        } else {
            p = Pattern.compile(regexB, Pattern.CASE_INSENSITIVE);
            m = p.matcher(sql);
            if (m.find()) {
                String[] tables = m.group(1).split(",");
                if (tables.length == 1) {
                    return getTableName(tables[0]);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    private static String getTableName(String str) {
        String regex = "(\\w+)(?:\\sAS\\s\\w+)?";
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(str);
        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }
}
