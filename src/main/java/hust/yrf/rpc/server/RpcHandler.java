package hust.yrf.rpc.server;

import hust.yrf.rpc.protocol.RpcRequest;
import hust.yrf.rpc.protocol.RpcResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @ClassName RpcHandler
 * @Descripition TODO
 * @Author rfYang
 * @Date 2018/10/31 19:50
 **/
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private static final Logger logger = LoggerFactory.getLogger(RpcHandler.class);

    private final Map<String, Object> handlerMap;

    public RpcHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        RpcServer.submit(new Runnable() {
            @Override
            public void run() {
                logger.debug("Receive request " + rpcRequest.getRequestId());
                RpcResponse rpcResponse = new RpcResponse();
                rpcResponse.setRequestId(rpcRequest.getRequestId());
                try {
                    Object result = handle(rpcRequest);
                    rpcResponse.setResult(result);
                } catch (Throwable t) {
                    logger.error("RPC Server handle request error", t);
                    rpcResponse.setError(t.toString());
                }
                channelHandlerContext.writeAndFlush(rpcResponse).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        logger.debug("Send response for request " + rpcRequest.getRequestId());
                    }
                });
            }
        });

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("server caught exception", cause);
        ctx.close();
    }

    private Object handle(RpcRequest rpcRequest) throws Throwable {
        String className = rpcRequest.getClassName();
        Object serviceBean = handlerMap.get(className);

        Class<?> serviceClass = serviceBean.getClass();
        String methodName = rpcRequest.getMethodName();
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        Object[] parameters = rpcRequest.getParameters();

        //CGlib
        FastClass fastClass = FastClass.create(serviceClass);
        FastMethod method = fastClass.getMethod(methodName, parameterTypes);
        return method.invoke(serviceBean, parameters);
    }
}
