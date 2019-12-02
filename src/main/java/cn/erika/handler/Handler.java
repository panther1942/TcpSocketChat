package cn.erika.handler;

import cn.erika.core.TcpSocket;

import java.io.IOException;

public interface Handler {
    void read(TcpSocket socket, DataHead head, byte[] data) throws IOException;

    void write(TcpSocket socket, byte[] data) throws IOException;
}
