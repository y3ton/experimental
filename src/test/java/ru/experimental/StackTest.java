package ru.experimental;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class StackTest {

    @Test
    public void simpleTest() {
        Stack<String> stack = new Stack<>();
        stack.push("1");
        stack.push("2");
        Assert.assertEquals("2", stack.pop());
        Assert.assertEquals("1", stack.pop());
        Assert.assertNull(stack.pop());
        Assert.assertTrue(stack.isEmpty());
    }

    @Test
    public void emptyTest() {
        Stack<String> stack = new Stack<>();
        Assert.assertTrue(stack.isEmpty());
        Assert.assertNull(stack.pop());
        Assert.assertNull(stack.pop());
        Assert.assertTrue(stack.isEmpty());
        stack.push("1");
        Assert.assertFalse(stack.isEmpty());
        Assert.assertEquals("1", stack.pop());
        stack.push("2");
        Assert.assertEquals("2", stack.pop());
        Assert.assertNull(stack.pop());
        Assert.assertTrue(stack.isEmpty());
    }

    @Test
    public void consumerProducerTest() throws InterruptedException {
        final Stack<Integer> stack = new Stack<>();
        final AtomicInteger cnt = new AtomicInteger();

        Thread thProducer = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                stack.push(i);
            }
        });
        Thread thConsumer = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Optional.ofNullable(stack.pop()).map(cnt::addAndGet);
            }
        });

        thConsumer.start();
        thProducer.start();
        thProducer.join();
        while (!stack.isEmpty()) {
            Thread.sleep(100);
        }
        thConsumer.interrupt();
        thConsumer.join();
        Assert.assertFalse(thConsumer.isAlive());
        Assert.assertEquals(10000 * (10000 - 1) / 2, cnt.get());
    }

    @Test
    public void multiThreadTest() throws InterruptedException {
        final Stack<Integer> stack = new Stack<>();
        final AtomicInteger cntPush = new AtomicInteger();
        final AtomicInteger cntPop = new AtomicInteger();

        ThreadPoolExecutor consumersExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        ThreadPoolExecutor producersExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            consumersExecutorService.execute(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    Optional.ofNullable(stack.pop()).map(cntPop::addAndGet);
                }
            });
        }
        for (int i = 0; i < 30; i++) {
            producersExecutorService.execute(() -> {
                for (int j = 0; j < 10000; j++) {
                    cntPush.addAndGet(j);
                    stack.push(j);

                }
            });
        }
        do {
            Thread.sleep(100);
        }  while (producersExecutorService.getActiveCount() > 0 || !stack.isEmpty());
        producersExecutorService.shutdownNow();
        consumersExecutorService.shutdownNow();
        Thread.sleep(100);

        Assert.assertTrue(producersExecutorService.isTerminated());

        Assert.assertEquals(10000 * (10000 - 1) / 2 * 30, cntPush.get());
        Assert.assertEquals(10000 * (10000 - 1) / 2 * 30, cntPop.get());
    }
}
