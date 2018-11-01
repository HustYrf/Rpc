package hust.yrf.rpc.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @ClassName RpcDecoder
 * @Descripition TODO
 * @Author rfYang
 * @Date 2018/10/31 17:01
 **/
public class RpcDecoder extends ByteToMessageDecoder {
    private Class<?> cls;

    public RpcDecoder(Class<?> cls) {
        this.cls = cls;
    }

    @Override
    public final void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() < 4) {//消息体不满足要求
            return;
        }

        byteBuf.markReaderIndex();//marker一下读指针
        int dateLength = byteBuf.readInt();//读出消息体的总长度

        if (byteBuf.readableBytes() < dateLength) {//如果可读的字节数小于消息体的长度，也是消息体不全,复位读指针，返回
            byteBuf.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dateLength];
        byteBuf.readBytes(data);//取指定长度的内容到目标容器
        Object obj = SerializationUtil.deSerialize(data,cls);
        list.add(obj);
    }
}
