package cn.erika.handler;

import cn.erika.core.TcpSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

class ClientManager {
    private HashMap<String, TcpSocket> links = new HashMap<>();
    private int order = 0;

    void add(TcpSocket socket) throws IOException {
        links.put(("id" + this.order++), socket);
    }

    TcpSocket get(String key) {
        return this.links.get(key);
    }

    String get(TcpSocket socket){
        for (String key : links.keySet()) {
            if (links.get(key).equals(socket)) {
                return key;
            }
        }
        return null;
    }

    /**
     * 用于关闭客户端连接
     *
     * @param socket 客户端连接对应的TcpSocket对象 应该从Socket管理程序取出
     */
    void del(TcpSocket socket) {
        String target = get(socket);
        if (target != null) {
            links.remove(target);
        }
    }

    int size(){
        return links.size();
    }

    Set<String> linksInfo(){
        return links.keySet();
    }
}
