package cn.erika.core;

import java.io.IOException;

public interface Reader {
    public void process(byte[] data, int len) throws IOException;
}
