package cn.erika.plugins.io;

/**
 * 通用输入类
 */
public interface GeneralInput {
    // 本地编码格式 Linux下为UTF-8 国内中文Windows为GBK/GB2312/GB18030
    public static final String LOCAL_CODE = System.getProperty("file.encoding");

    public String read();

    public String read(String tip);
}

