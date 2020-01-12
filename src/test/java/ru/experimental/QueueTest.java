package ru.experimental;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class QueueTest {

    @Test
    public void simpleTest() {
        Queue<Integer> queue = new Queue<>();
        queue.add(1);
        queue.add(2);
        Assert.assertEquals(Integer.valueOf(1), queue.remove());
        Assert.assertEquals(Integer.valueOf(2), queue.remove());
        Assert.assertTrue(queue.isEmpty());
    }

    @Test
    public void emptyTest() {
        Queue<Integer> queue = new Queue<>();
        Assert.assertTrue(queue.isEmpty());
        Assert.assertNull(queue.remove());
        Assert.assertNull(queue.remove());
        queue.add(1);
        Assert.assertFalse(queue.isEmpty());
        Assert.assertEquals(Integer.valueOf(1), queue.remove());
        Assert.assertNull(queue.remove());
        Assert.assertTrue(queue.isEmpty());
    }

    @Test
    public void consumerProducerTest() throws InterruptedException {
        final Queue<Integer> queue = new Queue<>();
        final AtomicInteger cnt = new AtomicInteger();

        Thread thProducer = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                queue.add(i);
            }
        });
        Thread thConsumer = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Optional.ofNullable(queue.remove()).map(cnt::addAndGet);
            }
        });

        thConsumer.start();
        thProducer.start();
        thProducer.join();
        while (!queue.isEmpty()) {
            Thread.sleep(100);
        }
        thConsumer.interrupt();
        thConsumer.join();
        Assert.assertFalse(thConsumer.isAlive());
        Assert.assertEquals(10000 * (10000 - 1) / 2, cnt.get());
    }

    @Test
    public void sequenceTest() throws InterruptedException {
        final Queue<Integer> queue = new Queue<>();
        for (int i = 0; i < 10; i++) {
            queue.add(i);
        }
        Object lock = new Object();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        AtomicInteger last = new AtomicInteger(-10);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                while (!queue.isEmpty()) {
                    synchronized (lock) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        last.set(queue.remove());
                    }
                }
            });
        }
        Thread.sleep(100);
        for (int i = 0; i < 10; i++) {
            synchronized (lock) {
                lock.notify();
            }
            Thread.sleep(10);
            Assert.assertEquals(i, last.get());
        }
        synchronized (lock) {
            lock.notifyAll();
        }
        executor.shutdown();
        Thread.sleep(100);

        Assert.assertTrue(executor.isTerminated());
        Assert.assertTrue(queue.isEmpty());
    }

    @Test
    public void liveLockTest() throws InterruptedException {
        final Queue<Integer> queue = new Queue<>();
        final AtomicInteger cnt = new AtomicInteger();

        for (int i = 0; i < 1000000; i++) {
            queue.add(1);
        }

        Thread thProducer = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                queue.add(i);
            }
        });
        Thread thConsumer = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Optional.ofNullable(queue.remove()).map(cnt::addAndGet);
            }
        });

        thConsumer.start();
        thProducer.start();
        thProducer.join();
        while (!queue.isEmpty()) {
            Thread.sleep(100);
        }
        thConsumer.interrupt();
        thConsumer.join();
        Assert.assertFalse(thConsumer.isAlive());
        Assert.assertEquals(10000 * (10000 - 1) / 2 + 1000000, cnt.get());
    }

    @Test
    public void multiThreadTest() throws InterruptedException {
        final Queue<Integer> queue = new Queue<>();
        final AtomicInteger cntAdd = new AtomicInteger();
        final AtomicInteger cntRemove = new AtomicInteger();

        for (int i = 0; i < 1000000; i++) {
            queue.add(1);
        }

        ThreadPoolExecutor consumersExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        ThreadPoolExecutor producersExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            consumersExecutorService.execute(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    Optional.ofNullable(queue.remove()).map(cntRemove::addAndGet);
                }
            });
        }
        for (int i = 0; i < 30; i++) {
            producersExecutorService.execute(() -> {
                for (int j = 0; j < 10000; j++) {
                    cntAdd.addAndGet(j);
                    queue.add(j);

                }
            });
        }
        do {
            Thread.sleep(1000);
        }  while (producersExecutorService.getActiveCount() > 0 || !queue.isEmpty());
        producersExecutorService.shutdownNow();
        consumersExecutorService.shutdownNow();
        Thread.sleep(100);

        Assert.assertTrue(producersExecutorService.isTerminated());
        Assert.assertTrue(consumersExecutorService.isTerminated());
        Assert.assertTrue(queue.isEmpty());

        Assert.assertEquals(10000 * (10000 - 1) / 2 * 30, cntAdd.get());
        Assert.assertEquals(10000 * (10000 - 1) / 2 * 30 + 1000000, cntRemove.get());
    }

}
