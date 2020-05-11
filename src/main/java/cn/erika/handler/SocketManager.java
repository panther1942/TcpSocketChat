package cn.erika.handler;

import cn.erika.core.TcpSocket;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

class SocketManager {
    private Logger log = Logger.getLogger(this.getClass().getName());

    private HashMap<String, TcpSocket> links = new HashMap<>();
    private HashMap<TcpSocket, Reader> cachePool = new HashMap<>();
    private int order = 0;

    private Timer timer = new Timer();

    SocketManager() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                log.debug("开始运行定时清理程序");
                int count;
                synchronized (this) {
                    Iterator<Map.Entry<String, TcpSocket>> it = links.entrySet().iterator();
                    count = 0;
                    while (it.hasNext()) {
                        Map.Entry<String, TcpSocket> en = it.next();
                        TcpSocket socket = en.getValue();
                        if (socket.isClosed()) {
                            it.remove();
                            count++;
                        }
                    }
                }
                log.info("清除无效连接[" + count + "]");
            }
        };
        timer.schedule(task, 15000, 15000);
    }

    void shutdown() throws IOException {
        timer.cancel();
    }

    synchronized String add(TcpSocket socket, Reader reader) throws IOException {
        String id = "id" + this.order++;
        links.put(id, socket);
        cachePool.put(socket, reader);
        return id;
    }

    /**
     * 用于关闭客户端连接
     *
     * @param socket 客户端连接对应的TcpSocket对象 应该从Socket管理程序取出
     */
    synchronized void del(TcpSocket socket) {
        String target = get(socket);
        if (target != null) {
            links.remove(target);
        }
    }

    TcpSocket get(String key) {
        return this.links.get(key);
    }

    TcpSocket getByNickname(String nickname) {
        for (String key : links.keySet()) {
            String _nickname = links.get(key).getAttr(Extra.NICKNAME);
            if (_nickname.equals(nickname)) {
                return this.links.get(key);
            }
        }
        return null;
    }

    Set<String> get() {
        return links.keySet();
    }

    String get(TcpSocket socket) {
        for (String key : links.keySet()) {
            if (links.get(key) == socket) {
                return key;
            }
        }
        return null;
    }

    Reader getCache(TcpSocket socket) {
        return cachePool.get(socket);
    }

    int size() {
        return links.size();
    }

    Set<String> linksInfo() {
        return links.keySet();
    }
}
