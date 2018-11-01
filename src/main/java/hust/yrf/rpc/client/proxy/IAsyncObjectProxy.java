package hust.yrf.rpc.client.proxy;

import hust.yrf.rpc.client.RPCFuture;

public interface IAsyncObjectProxy {
    public RPCFuture call(String funcName, Object... args);
}
