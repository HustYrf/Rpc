package hust.yrf.rpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @ClassName ConnectManage
 * @Descripition TODO
 * @Author rfYang
 * @Date 2018/11/1 13:27
 **/
public class ConnectManage {
    private static final Logger logger = LoggerFactory.getLogger(ConnectManage.class);
    private volatile static ConnectManage connectManage;
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlers = new CopyOnWriteArrayList<RpcClientHandler>();
    private Map<InetSocketAddress, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    private ReentrantLock reentrantLock = new ReentrantLock();
    private Condition connected = reentrantLock.newCondition();
    private long connectTimeoutMills = 6000;
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private volatile boolean isRuning = true;


    public ConnectManage() {
    }

    public static ConnectManage getInstance() {//同步处理，获得一个实例
        if (connectManage == null) {
            synchronized (ConnectManage.class) {
                if (connectManage == null) {
                    connectManage = new ConnectManage();
                }
            }
        }
        return connectManage;
    }

    public void updateConnectServer(List<String> allServerAddress) {
        if (allServerAddress != null) {
            if (allServerAddress.size() > 0) {
                //更新local ServerNodes Cache
                HashSet<InetSocketAddress> newAllServerNodeSet = new HashSet<>();
                for (int i = 0; i < allServerAddress.size(); i++) {
                    String[] array = allServerAddress.get(i).split(":");
                    if (array.length == 2) {
                        String host = array[0];
                        int port = Integer.parseInt(array[1]);
                        final InetSocketAddress socketAddress = new InetSocketAddress(host, port);
                        newAllServerNodeSet.add(socketAddress);
                    }
                }

                for (final InetSocketAddress socketAddress : newAllServerNodeSet) {
                    if (!connectedServerNodes.keySet().contains(socketAddress)) {
                        connectServerNode(socketAddress);
                    }
                }


                for (int i = 0; i < connectedHandlers.size(); i++) {
                    RpcClientHandler rpcClientHandler = connectedHandlers.get(i);
                    SocketAddress remotePeer = rpcClientHandler.getRemotePeer();
                    if (!newAllServerNodeSet.contains(remotePeer)) {
                        logger.info("Remove invalid server node " + remotePeer);
                        RpcClientHandler rpcClientHandler1 = connectedServerNodes.get(remotePeer);
                        if (rpcClientHandler1 != null) {
                            rpcClientHandler1.close();
                        }
                        connectedServerNodes.remove(remotePeer);
                        connectedHandlers.remove(rpcClientHandler);
                    }
                }

            } else {
                logger.error("No available server node. All server nodes are down !!!");
                for (final RpcClientHandler connectedServerHandler : connectedHandlers) {
                    SocketAddress remotePeer = connectedServerHandler.getRemotePeer();
                    RpcClientHandler handler = connectedServerNodes.get(remotePeer);
                    handler.close();
                    connectedServerNodes.remove(connectedServerHandler);
                }
                connectedHandlers.clear();
            }
        }
    }


    public void reconnect(final RpcClientHandler handler, final SocketAddress remoteAddress) {
        if (handler != null) {
            connectedHandlers.remove(handler);
            connectedServerNodes.remove(handler.getRemotePeer());
        }
        connectServerNode((InetSocketAddress) remoteAddress);
    }

    private void connectServerNode(final InetSocketAddress remotePeer) {
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new RpcClientInitializer());

                ChannelFuture channelFuture = b.connect(remotePeer);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            logger.debug("Successfully connect to remote server. remote peer = " + remotePeer);
                            RpcClientHandler rpcClientHandler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            addHandler(rpcClientHandler);
                        }
                    }
                });
            }
        });
    }

    private void addHandler(RpcClientHandler handler) {
        connectedHandlers.add(handler);
        InetSocketAddress remoteAddress = (InetSocketAddress) handler.getChannle().remoteAddress();
        connectedServerNodes.put(remoteAddress, handler);
        signalAvailableHandler();
    }

    public RpcClientHandler chooseHandler() {
        int size = connectedHandlers.size();
        while (isRuning && size <= 0) {
            try {
                boolean available = waitingForHandler();
                if (available) {
                    size = connectedHandlers.size();
                }
            } catch (InterruptedException e) {
                logger.error("Waiting for available node is interrupted! ", e);
                throw new RuntimeException("Can't connect any servers!", e);
            }
        }
        int index = (roundRobin.getAndAdd(1) + size) % size;
        return connectedHandlers.get(index);
    }

    private void signalAvailableHandler() {
        reentrantLock.lock();
        try {
            connected.signalAll();
        } finally {
            reentrantLock.unlock();
        }
    }

    private boolean waitingForHandler() throws InterruptedException {
        reentrantLock.lock();
        try {
            return connected.await(this.connectTimeoutMills, TimeUnit.MILLISECONDS);
        } finally {
            reentrantLock.unlock();
        }
    }

    public void stop() {
        isRuning = false;
        for (int i = 0; i < connectedHandlers.size(); ++i) {
            RpcClientHandler connectedServerHandler = connectedHandlers.get(i);
            connectedServerHandler.close();
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}
