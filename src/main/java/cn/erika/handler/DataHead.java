package cn.erika.handler;

import java.util.Date;

public class DataHead {
    // 时间戳
    private Date timestamp;
    // 数据长度
    private int len;
    // 如果发送文件 这里填文件名
    private String file;
    // 指令
    private Type order;

    public enum Type {
        NONE(0x00), // 不包含特殊含义
        ACCEPT(0x01), // 接受请求
        REJECT(0x02), // 拒绝请求
        DEBUG(0x03), // 调试信息
        INFO(0x04), // 通知信息
        WARN(0x05), // 警告信息
        ERROR(0x06), // 错误信息
        ENCRYPT(0x10), // 请求加密
        RSA(0x11), // 这次发送的是RSA公钥
        AES(0x12), // 这次发送的是AES秘钥
        BYE(0xFF); // 断开信息

        private int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public DataHead(int len, Type order) {
        this.timestamp = new Date();
        this.len = len;
        this.order = order;
    }

    public DataHead(int len, Type order, String file) {
        this(len, order);
        this.file = file;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Type getOrder() {
        return order;
    }

    public void setOrder(Type order) {
        this.order = order;
    }
}
