package ru.experimental;

import java.util.concurrent.atomic.AtomicReference;

public class Queue<T> {

    public static class Node<T> {
        private final T value;
        private final AtomicReference<Node<T>> next;

        public Node(T value) {
            this.value = value;
            this.next = new AtomicReference<>();
        }

        public T getValue() {
            return value;
        }

        public AtomicReference<Node<T>> getNext() {
            return next;
        }
    }

    private final AtomicReference<Node<T>> head = new AtomicReference<>(new Node<>(null));
    volatile Node<T> tail = head.get();

    public boolean isEmpty() {
        return head.get().getNext().get() == null;
    }

    public void add(T value) {
        Node<T> newNode = new Node<>(value);
        while (true) {
            Node<T> last = tail;
            while (last != null && last.getNext().get() != null) {
                last = last.getNext().get();
            }
            if (last != null && last.getNext().compareAndSet(null, newNode)) {
                tail = newNode;
                return;
            }
        }
    }

    public T remove() {
        while (true) {
            Node<T> oldHead = head.get();
            if (oldHead.getNext().get() == null) {
                return null;
            }
            if (head.compareAndSet(oldHead, oldHead.getNext().get())) {
                return oldHead.getNext().get().getValue();
            }
        }
    }
}
