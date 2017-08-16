package edu.illinois.cs.cogcomp.nlp.util;

/**
 * Created by qning2 on 12/21/16.
 */
public class Triplet<T,F,K> {
    private T first;
    private F second;
    private K third;
    public Triplet(T first, F second, K third){
        this.first=first;
        this.second=second;
        this.third=third;
    }

    public T getFirst() {
        return first;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public F getSecond() {
        return second;
    }

    public void setSecond(F second) {
        this.second = second;
    }

    public K getThird() {
        return third;
    }

    public void setThird(K third) {
        this.third = third;
    }
}
