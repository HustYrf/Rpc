package com.nettyrpc.test.app;


import com.nettyrpc.test.client.Person;
import hust.yrf.rpc.client.AsyncRPCCallback;
import hust.yrf.rpc.client.RPCFuture;
import hust.yrf.rpc.client.RpcClient;
import hust.yrf.rpc.client.proxy.IAsyncObjectProxy;
import hust.yrf.rpc.registry.ServiceDiscover;
import com.nettyrpc.test.client.PersonService;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class PersonCallbackTest {
    public static void main(String[] args) {
        ServiceDiscover serviceDiscover = new ServiceDiscover("127.0.0.1:2181");
        final RpcClient rpcClient = new RpcClient(serviceDiscover);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        try {
            IAsyncObjectProxy client = rpcClient.createAsync(PersonService.class);
            int num = 5;
            RPCFuture helloPersonFuture = client.call("GetTestPerson", "xiaoming", num);
            helloPersonFuture.addCallback(new AsyncRPCCallback() {
                @Override
                public void success(Object result) {
                    List<Person> persons = (List<Person>) result;
                    for (int i = 0; i < persons.size(); ++i) {
                        System.out.println(persons.get(i));
                    }
                    countDownLatch.countDown();
                }

                @Override
                public void fail(Exception e) {
                    System.out.println(e);
                    countDownLatch.countDown();
                }
            });

        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        rpcClient.stop();

        System.out.println("End");
    }
}
