package cn.erika.handler;

import cn.erika.core.TcpSocket;

import java.io.IOException;

public interface Handler {
    void deal(TcpSocket socket, DataHead head, byte[] data) throws IOException;
}
