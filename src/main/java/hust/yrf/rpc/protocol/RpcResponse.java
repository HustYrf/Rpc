package hust.yrf.rpc.protocol;

import lombok.Data;

/**
 * @ClassName RpcResponse
 * @Descripition RPC Response相应体
 * @Author rfYang
 * @Date 2018/10/31 16:48
 **/
@Data
public class RpcResponse {
    //使用lombok的@Data注解，省去生成getter和setter方法,ide需要装lombok插件
    private String requestId;
    private String error;
    private Object result;

    public boolean isError() {
        return error != null;
    }
}
