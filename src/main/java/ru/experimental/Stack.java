package ru.experimental;

import java.util.concurrent.atomic.AtomicReference;

public class Stack<T> {

    public static class Node<T> {

        final Node<T> next;
        final T value;

        public Node<T> getNext() {
            return next;
        }

        public T getValue() {
            return value;
        }

        public Node(Node<T> next, T value) {
            this.next = next;
            this.value = value;
        }

    }

    private final AtomicReference<Node<T>> head = new AtomicReference<>();

    public boolean isEmpty() {
        return head.get() == null;
    }

    public void push(T value) {
        Node<T> oldHead = head.get();
        Node<T> n = new Node<T>(oldHead, value);
        while (!head.compareAndSet(oldHead, n)) {
            oldHead = head.get();
            n = new Node<T>(oldHead, value);
        }

    }

    public T pop() {
        Node<T> n = head.get();
        while (n != null && !head.compareAndSet(n, n.getNext())) {
            n = head.get();
        }
        return n == null ? null : n.getValue();
    }
}
