package cn.erika.plugins.io;

import java.nio.charset.Charset;
import java.util.ResourceBundle;

public class ConfigReader {

    private static ResourceBundle config = ResourceBundle.getBundle("config");

    public static String get(String key) {
        return config.getString(key);
    }

    public static Charset charset() {
        return Charset.forName(config.getString("charset"));
    }
}
