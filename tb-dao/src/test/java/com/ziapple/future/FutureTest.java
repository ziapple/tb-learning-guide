package com.ziapple.future;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class FutureTest {
    @Test
    public void testImmediateFuture() throws ExecutionException, InterruptedException {
        FutureTest futureTest = new FutureTest();
        ListenableFuture<Integer> result = Futures.immediateFuture(futureTest.getResult());
        System.out.println(result.get());
    }

    public Integer getResult() throws InterruptedException {
        Thread.sleep(3000);
        return 3;
    }
}
