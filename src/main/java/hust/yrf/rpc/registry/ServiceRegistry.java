package hust.yrf.rpc.registry;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @ClassName ServiceRegistry
 * @Descripition 服务注册
 * @Author rfYang
 * @Date 2018/10/31 15:57
 **/
public class ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);
    private CountDownLatch countDownLatch = new CountDownLatch(1);
    private String registryAddress;

    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public void register(String data) {
        if (data != null) {
            ZooKeeper zk = connectServer();
            if (zk != null) {
                AddRootNode(zk);
                createNode(zk, data);
            }
        }
    }

    private void createNode(ZooKeeper zk, String data) {
        try {
            byte[] bytes = data.getBytes();
            String path = zk.create(Constant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.debug("create zookeeper node ({} => {})", path, data);
        } catch (KeeperException e) {
            logger.error("", e);
        }
        catch (InterruptedException ex){
            logger.error("", ex);
        }
    }
//判断服务父节点是否存在，如果不存在则添加一个PERSISTENT的服务父节点
    private void AddRootNode(ZooKeeper zk) {
        try {
            Stat s = zk.exists(Constant.ZK_REGISTRY_PATH, false);
            if (s == null) {
                zk.create(Constant.ZK_REGISTRY_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            logger.error(e.toString());
        } catch (InterruptedException e) {
            logger.error(e.toString());
        }
    }

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
        } catch (IOException e) {
            logger.error("", e);
        }
        catch (InterruptedException ex){
            logger.error("", ex);
        }
        return zk;
    }
}
