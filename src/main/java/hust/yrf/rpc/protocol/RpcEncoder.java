package hust.yrf.rpc.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @ClassName RpcEncoder
 * @Descripition TODO
 * @Author rfYang
 * @Date 2018/10/31 16:56
 **/
public class RpcEncoder extends MessageToByteEncoder {
    private Class<?> cls;

    public RpcEncoder(Class<?> cls) {
        this.cls = cls;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object in, ByteBuf byteBuf) throws Exception {
        if (cls.isInstance(in)) {
            byte[] serialize = SerializationUtil.serialize(in);
            byteBuf.writeInt(serialize.length);
            byteBuf.readBytes(serialize);
        }
    }
}
