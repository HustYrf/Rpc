package hust.yrf.rpc.registry;

import hust.yrf.rpc.client.ConnectManage;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @ClassName ServiceDiscover
 * @Descripition TODO
 * @Author rfYang
 * @Date 2018/10/31 16:10
 **/
public class ServiceDiscover {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscover.class);

    private CountDownLatch countDownLatch = new CountDownLatch(1);
    private volatile List<String> dataList = new ArrayList<>();

    private String registryAddress;
    private ZooKeeper zooKeeper;

    public ServiceDiscover(String registryAddress) {
        this.registryAddress = registryAddress;
        zooKeeper = connectServer();
        if (zooKeeper != null) {
            watchNode(zooKeeper);
        }
    }

    private void watchNode(final ZooKeeper zooKeeper) {
        try {
            List<String> nodeList = zooKeeper.getChildren(Constant.ZK_REGISTRY_PATH, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                        watchNode(zooKeeper);
                    }
                }
            });
            List<String> dataList = new ArrayList<>();
            for (String node : nodeList) {
                byte[] data = zooKeeper.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
                dataList.add(new String(data));
            }
            logger.debug("node data: {}", dataList);
            this.dataList = dataList;

            logger.debug("Service discovery triggered updating connected server node.");
            UpdateConnectedServer();
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void UpdateConnectedServer() {
        ConnectManage.getInstance().updateConnectServer(this.dataList);
    }

    //同注册代码区
    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        countDownLatch.countDown();
                    }
                }
            });
            countDownLatch.await();
        } catch (IOException | InterruptedException e) {
            logger.error("", e);
        }
        return zk;
    }

    public void stop() {
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                logger.error("", e);
            }
        }
    }
}
