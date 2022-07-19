package org.nasaapis;

public class Pair<T, X> {
    private final T one;
    private final X two;

    public Pair(T one, X two) {
        this.one = one;
        this.two = two;
    }

    public T getFirst() {
        return this.one;
    }

    public X getSecond() {
        return this.two;
    }
}