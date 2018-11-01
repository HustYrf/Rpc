package hust.yrf.rpc.protocol;

import lombok.Data;

/**
 * @ClassName RpcRequest
 * @Descripition TODO
 * @Author rfYang
 * @Date 2018/10/31 16:53
 **/
@Data
public class RpcRequest {
    private String requestId;
    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
}
