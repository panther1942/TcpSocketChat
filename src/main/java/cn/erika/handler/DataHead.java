package cn.erika.handler;

import java.util.Date;

public class DataHead {
    static final int LEN = 13 + 10 + 10 + 4 + 256;

    public enum Order {
        HELLO_WORLD(0x00),
        ASC(0x01),
        BIN(0x02),
        FILE_RECEIVE_READY(0x03),
        FILE_RECEIVE_REFUSE(0x04),
        FILE_RECEIVE_FINISHED(0x05),
        DEBUG(0x10),
        INFO(0x11),
        WARN(0x12),
        ERROR(0x13),
        ENCRYPT(0x14),
        RSA(0x21),
        AES(0x22),
        NAME(0x23),
        FIND(0x24),
        TALK(0x25),
        HIDE(0x26),
        SEEK(0x27),
        DIRECT(0x29),
        REMOTE_ADDRESS(0x28),
        BYE(0xFF);

        public int value;

        Order(int value) {
            this.value = value;
        }

        public static Order getByValue(int value) {
            for (Order order : Order.values()) {
                if (order.value == value) {
                    return order;
                }
            }
            return null;
        }
    }

    // 时间戳 13
    private Date timestamp;
    // 数据偏移量 10
    private long pos = 0;
    // 数据长度 10
    private int len = 0;
    // 指令 4
    private Order order = Order.ASC;
    // 签名 256
    private byte[] sign;

    public DataHead() {
        this.timestamp = new Date();
    }

    public DataHead(Order order) {
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

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public void setOrder(int order) {
        this.order = Order.getByValue(order);
    }

    public byte[] getSign() {
        return sign;
    }

    public void setSign(byte[] sign) {
        this.sign = sign;
    }

    @Override
    public String toString() {
        String timestamp = String.format("%13s", this.timestamp.getTime()).replaceAll(" ", "0");
        String pos = String.format("%10s", this.pos).replaceAll(" ", "0");
        String len = String.format("%10s", this.len).replaceAll(" ", "0");
        String order = String.format("%4s", Integer.toString(this.order.value)).replaceAll(" ", "0");
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
