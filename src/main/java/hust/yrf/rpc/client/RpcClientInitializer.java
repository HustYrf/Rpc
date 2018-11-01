package hust.yrf.rpc.client;

import hust.yrf.rpc.protocol.RpcDecoder;
import hust.yrf.rpc.protocol.RpcEncoder;
import hust.yrf.rpc.protocol.RpcRequest;
import hust.yrf.rpc.protocol.RpcResponse;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @ClassName RpcClientInitializer
 * @Descripition TODO
 * @Author rfYang
 * @Date 2018/11/1 14:33
 **/
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline cp = channel.pipeline();
        cp.addLast(new RpcEncoder(RpcRequest.class));
        cp.addLast(new LengthFieldBasedFrameDecoder(65539, 0, 4, 0, 0));
        cp.addLast(new RpcDecoder(RpcResponse.class));
        cp.addLast(new RpcClientHandler());
    }
}
