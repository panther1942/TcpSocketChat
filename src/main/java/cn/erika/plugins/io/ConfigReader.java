package cn.erika.plugins.io;

import java.nio.charset.Charset;
import java.util.ResourceBundle;

public class ConfigReader {

    private static ResourceBundle res = ResourceBundle.getBundle("config");

    public static String get(String key) {
        return res.getString(key);
    }

    public static Charset charset() {
        return Charset.forName(res.getString("charset"));
    }
}
