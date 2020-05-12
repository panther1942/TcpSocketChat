package cn.erika.plugins.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用于读取键盘操作
 */
public class KeyboardReader implements GeneralInput {
    // 单例模式，防止多次新建对象浪费内存
    private static KeyboardReader readerKB;
    // 输入对象，读取键盘属于读取字符
    private BufferedReader reader;

    private KeyboardReader() {
        try {
            reader = new BufferedReader(new InputStreamReader(System.in, LOCAL_CODE));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 单例模式获取键盘输入对象
     * 当前属于延迟加载模式
     *
     * @return 键盘输入对象
     */
    public static GeneralInput getInstance() {
        if (readerKB == null) {
            readerKB = new KeyboardReader();
        }
        return readerKB;
    }

    /**
     * 读取键盘输入，回车键确认输入
     *
     * @return 输入字符
     */
    @Override
    public String read() {
        if (reader != null) {
            String line;
            try {
                if ((line = reader.readLine()) != null) {
                    return line;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Failed to init input stream");
        }
        return null;
    }

    /**
     * 带提示符的读取键盘输入，回车键确认输入
     *
     * @param tip 提示符
     * @return 输入字符
     */
    @Override
    public String read(String tip) {
        System.out.print(tip);
        return read();
    }
}

