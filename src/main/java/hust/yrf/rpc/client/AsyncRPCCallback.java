package hust.yrf.rpc.client;

public interface AsyncRPCCallback {
    void success(Object redult);
    void fail(Exception e);
}
