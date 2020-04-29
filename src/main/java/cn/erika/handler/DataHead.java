package cn.erika.handler;

import java.util.Date;

public class DataHead {
    public static final int LEN = 13 + 10 + 10 + 4 + 256;

    public enum Order {
        ASC(0x00),
        BIN(0x01),
        READY(0x02),
        DEBUG(0x03),
        INFO(0x04),
        WARN(0x05),
        ERROR(0x06),
        ENCRYPT(0x10),
        RSA(0x11),
        AES(0x12),
        REG(0x13),
        FIND(0x14),
        TALK(0x15),
        HIDE(0x16),
        SEEK(0x17),
        ACCEPT(0x18),
        REJECT(0x19),
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
