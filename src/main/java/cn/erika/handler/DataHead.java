package cn.erika.handler;

import java.util.Date;

public class DataHead {
    public static final int ASC = 0x00;
    public static final int BIN = 0x01;
    public static final int READY = 0x02;
    public static final int DEBUG = 0x03;
    public static final int INFO = 0x04;
    public static final int WARN = 0x05;
    public static final int ERROR = 0x06;
    public static final int ENCRYPT = 0x10;
    public static final int RSA = 0x11;
    public static final int AES = 0x12;
    public static final int BYE = 0xFF;

    // 时间戳
    private Date timestamp;
    private long pos;
    // 数据长度
    private int len;
    // 指令
    private int order;

    public DataHead() {
        this.timestamp = new Date();
    }

    public DataHead(int order) {
        this();
        this.order = order;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public long getPos() {
        return pos;
    }

    public void setPos(long pos) {
        this.pos = pos;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public String toString() {
        String timestamp = String.format("%13s", this.timestamp.getTime()).replaceAll(" ", "0");
        String pos = String.format("%10s", this.pos).replaceAll(" ", "0");
        String len = String.format("%10s", this.len).replaceAll(" ", "0");
        String order = String.format("%4s", Integer.toString(this.order)).replaceAll(" ", "0");
        return timestamp + pos + len + order;
    }

    public String show() {
        return "DataHead{" +
                "timestamp=" + timestamp +
                ", pos=" + pos +
                ", len=" + len +
                ", order=" + order +
                '}';
    }
}
