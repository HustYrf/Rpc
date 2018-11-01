package hust.yrf.rpc.client;

import hust.yrf.rpc.client.proxy.IAsyncObjectProxy;
import hust.yrf.rpc.client.proxy.ObjectProxy;
import hust.yrf.rpc.registry.ServiceDiscover;

import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName RpcClient
 * @Descripition TODO
 * @Author rfYang
 * @Date 2018/11/1 13:16
 **/
public class RpcClient {
    private String serverAddress;
    private ServiceDiscover serviceDiscover;

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    public RpcClient(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public RpcClient(ServiceDiscover serviceDiscover) {
        this.serviceDiscover = serviceDiscover;
    }

    public static <T> T create(Class<T> interfaceClass) {//动态代理
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T>(interfaceClass)
        );
    }

    public static <T> IAsyncObjectProxy createAsync(Class<T> interfaceClass) {//客户端异步请求
        return new ObjectProxy<T>(interfaceClass);
    }

    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }

    public void stop() {
        threadPoolExecutor.shutdown();
        serviceDiscover.stop();
        ConnectManage.getInstance().stop();
    }
}
