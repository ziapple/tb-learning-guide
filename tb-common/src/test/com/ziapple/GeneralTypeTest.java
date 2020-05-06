package com.ziapple;

import java.util.ArrayList;
import java.util.List;

public class GeneralTypeTest {
    public static void main(String[] args) {
        List<? super Fruit> list = new ArrayList<>();
        list.add(new Apple("this is Apple"));
        list.add(new Fruit("this is Fruit"));
        list.forEach(t -> {
            Food f = (Food)t;
            System.out.println(f.name);
        });
    }

    public static class Food{
        public String name;
    }

    public static class Fruit extends Food{
        public Fruit(String name){
            super();
            super.name = name;
        }
    }

    public static class Apple extends Fruit{
        public Apple(String name){
            super(name);
        }
    }
}
