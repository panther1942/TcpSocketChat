package cn.erika.plugins.io;

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class ConfigReader {
    private static Logger log = Logger.getLogger(ConfigReader.class.getName());
    private static String configPath = System.getProperty("user.dir") + "/config.properties";
    private static ResourceBundle config;

    static {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(configPath))) {
            config = new PropertyResourceBundle(in);
            log.info("找到外部配置文件: " + configPath + " 忽略内置配置文件");
        } catch (IOException e) {
            log.warn("未找到外部配置文件: " + configPath + " 使用内置配置文件运行");
            config = ResourceBundle.getBundle("config");
        }
    }

    public static String get(String key) {
        return config.getString(key);
    }

    public static Charset charset() {
        return Charset.forName(config.getString("charset"));
    }
}
