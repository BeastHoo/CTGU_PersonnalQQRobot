package netCrawler.test.trial.monitor;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.concurrent.TimeUnit;

public class HttpClientConnectionMonitor extends Thread {

    private final HttpClientConnectionManager connManager;
    private volatile boolean shutdown;

    public HttpClientConnectionMonitor(HttpClientConnectionManager connManager) {
        super();
        this.setName("http-connection-monitor");
        this.setDaemon(true);
        this.connManager = connManager;
        this.start();
    }

    @Override
    public void run() {
        try {
            while (!shutdown) {
                synchronized (this) {
                    wait(5000); // 等待5秒
                    // 关闭过期的链接
                    connManager.closeExpiredConnections();


                    // 选择关闭 空闲30秒的链接
                    connManager.closeIdleConnections(30, TimeUnit.SECONDS);
                }
            }
        } catch (InterruptedException ex) {
        }
    }

    /**
     * 方法描述: 停止 管理器 清理无效链接  (该方法当前暂时关闭)
     * @author andy 2017年8月28日 下午1:45:18
     */
    @Deprecated
    public void shutDownMonitor() {
        synchronized (this) {
            shutdown = true;
            notifyAll();
        }
    }

}
