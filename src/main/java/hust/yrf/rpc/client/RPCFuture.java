package hust.yrf.rpc.client;


import com.sun.corba.se.impl.orbutil.concurrent.Sync;
import hust.yrf.rpc.protocol.RpcRequest;
import hust.yrf.rpc.protocol.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @ClassName RPCFuture
 * @Descripition TODO
 * @Author Administrator
 * @Date 2018/10/31 20:36
 **/
public class RPCFuture implements Future<Object> {
    private static final Logger logger = LoggerFactory.getLogger(RPCFuture.class);

    private Sync sync;
    private RpcRequest request;
    private RpcResponse response;
    private long startTime;
    private long responseTimeThreshold = 5000;

    private List<AsyncRPCCallback> pendingCallbacks = new ArrayList<>();
    private ReentrantLock reentrantLock = new ReentrantLock();

    public RPCFuture(RpcRequest request) {
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        if (this.response != null) {
            return this.response.getResult();
        } else {
            return null;
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (this.response != null) {
                return this.response.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
                    + ". Request class name: " + this.request.getClassName()
                    + ". Request method: " + this.request.getMethodName());
        }
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    public void done(RpcResponse response) {
        this.response = response;
        sync.release(1);
        invokeCallbacks();
        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            logger.warn("Service response time is too slow. Request id = " + response.getRequestId() + ". Response Time = " + responseTime + "ms");
        }
    }

    private void invokeCallbacks() {
        reentrantLock.lock();
        try {
            for (final AsyncRPCCallback callback : pendingCallbacks) {
                runCallback(callback);
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    private void runCallback(final AsyncRPCCallback callback) {
        final RpcResponse response = this.response;
        RpcClient.submit(new Runnable() {
            @Override
            public void run() {
                if (!response.isError()) {
                    callback.success(response.getResult());
                } else {
                    callback.fail(new RuntimeException("Response error", new Throwable(response.getError())));
                }
            }
        });
    }


    public RPCFuture addCallback(AsyncRPCCallback callback) {
        reentrantLock.lock();
        try {
            if(isDone()){
                runCallback(callback);
            }else{
                this.pendingCallbacks.add(callback);
            }
        } finally {
            reentrantLock.unlock();
        }
        return this;
    }


    static class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = 1L;

        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        public boolean isDone() {
            getState();
            return getState() == done;
        }
    }
}
