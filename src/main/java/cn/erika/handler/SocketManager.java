package cn.erika.handler;

import cn.erika.core.TcpSocket;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

class SocketManager {
    private Logger log = Logger.getLogger(this.getClass().getName());

    private HashMap<String, TcpSocket> links = new HashMap<>();
    private HashMap<TcpSocket, Cache> cachePool = new HashMap<>();
    private int order = 0;

    private boolean flag = false;
    private Timer timer = new Timer();
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            log.debug("开始运行定时清理程序");
            LinkedList<String> tmp = new LinkedList<>();
            for (String key : links.keySet()) {
                TcpSocket socket = links.get(key);
                if (socket.isClosed()) {
                    tmp.add(key);
                }
            }
            log.debug("清除无效连接[" + tmp.size() + "]");
            for (String key : tmp) {
                links.remove(key);
            }
            if (links.size() == 0) {
                timer.cancel();
                flag = false;
            }
        }
    };

    String add(TcpSocket socket, Cache cache) throws IOException {
        String id = "id" + this.order++;
        links.put(id, socket);
        cachePool.put(socket, cache);
        if (!flag) {
            timer.schedule(task, 0, 15000);
            flag = true;
        }
        return id;
    }

    TcpSocket get(String key) {
        return this.links.get(key);
    }

    TcpSocket getByNickname(String nickname) {
        for (String key : links.keySet()) {
            String _nickname = links.get(key).getAttr(ServerHandler.NICKNAME);
            if (_nickname.equals(nickname)) {
                return this.links.get(key);
            }
        }
        return null;
    }

    String get(TcpSocket socket) {
        for (String key : links.keySet()) {
            if (links.get(key) == socket) {
                return key;
            }
        }
        return null;
    }

    Cache getCache(TcpSocket socket) {
        return cachePool.get(socket);
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

    int size() {
        return links.size();
    }

    Set<String> linksInfo() {
        return links.keySet();
    }
}
