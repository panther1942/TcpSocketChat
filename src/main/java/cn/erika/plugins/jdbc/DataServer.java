package cn.erika.plugins.jdbc;

import cn.erika.plugins.security.RSA;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class DataServer {
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final String LOCALHOST = "localhost";
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void initDb() throws IOException, SQLException, NoSuchAlgorithmException {
        if (JdbcConnector.executeQuery(JdbcConnector.getSql("query_table_user")).size() == 0) {
            JdbcConnector.executeUpdate(JdbcConnector.getSql("create_table_user"));
            System.out.println("数据库初始化完成");
        }
        if (JdbcConnector.executeQuery(JdbcConnector.getSql("query_user_localhost")).size() == 0) {
            byte[][] keyPair = RSA.initKey(2048);
            String pubKey = new String(Base64.getEncoder().encode(keyPair[0]), CHARSET);
            String priKey = new String(Base64.getEncoder().encode(keyPair[1]), CHARSET);
            JdbcConnector.executeUpdate(JdbcConnector.getSql("insert_user"), LOCALHOST, sdf.format(new Date()));
            JdbcConnector.executeUpdate(JdbcConnector.getSql("update_user_public_key"), pubKey, LOCALHOST);
            JdbcConnector.executeUpdate(JdbcConnector.getSql("update_user_private_key"), priKey, LOCALHOST);
            System.out.println("本地数据初始化完成");
        }
        System.out.println("存在本地数据库,无需初始化");
    }

    public static byte[][] getKeyPair() throws IOException {
        Model mod = JdbcConnector.executeQuery(JdbcConnector.getSql("query_user_rsa"), LOCALHOST).get(0);
        String pubKey = mod.get("public_key");
        String priKey = mod.get("private_key");
        return new byte[][]{
                Base64.getDecoder().decode(pubKey),
                Base64.getDecoder().decode(priKey)
        };
    }
}
