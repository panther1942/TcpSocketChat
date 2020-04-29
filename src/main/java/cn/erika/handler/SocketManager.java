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

    private boolean flag = false;
    private Timer timer = new Timer();
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            log.debug("开始运行定时清理程序");
            Iterator<Map.Entry<String, TcpSocket>> it = links.entrySet().iterator();
            int count = 0;
            while (it.hasNext()) {
                Map.Entry<String, TcpSocket> en = it.next();
                TcpSocket socket = en.getValue();
                if (socket.isClosed()) {
                    it.remove();
                    count++;
                }
            }
            log.info("清除无效连接[" + count + "]");
        }
    };

    String add(TcpSocket socket, Reader reader) throws IOException {
        String id = "id" + this.order++;
        links.put(id, socket);
        cachePool.put(socket, reader);
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
            String _nickname = links.get(key).getAttr(Extra.NICKNAME);
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

    Reader getCache(TcpSocket socket) {
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
