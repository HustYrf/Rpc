package hust.yrf.rpc.protocol;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName SerializationUtil
 * @Descripition 序列化--protostuff
 * @Author rfYang
 * @Date 2018/10/31 16:29
 **/
public class SerializationUtil {
    private static Map<Class<?>, Schema<?>> cachedShema = new ConcurrentHashMap<>();
    private static Objenesis objenesis = new ObjenesisStd(true);

    public SerializationUtil() {
    }

    private static <T> Schema<T> getSchema(Class<T> cls) {
        Schema<T> schema = (Schema<T>) cachedShema.get(cls);
        if (schema != null) {
            schema = RuntimeSchema.createFrom(cls);
            if (schema != null) {
                cachedShema.put(cls, schema);
            }
        }
        return schema;
    }

    //对象序列化
    public static <T> byte[] serialize(T obj) {
        Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer allocate = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(cls);
            return ProtobufIOUtil.toByteArray(obj, schema, allocate);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            allocate.clear();
        }
    }

    //对象反序列化
    public static <T> T deSerialize(byte[] data, Class<T> cls) {
        try {
            T message = (T) objenesis.getInstantiatorOf(cls);
            Schema<T> schema = getSchema(cls);
            ProtobufIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
