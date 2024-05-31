package com.azoft.nusuth.core;

public class ClassOrJsp {

    String name;
    boolean isJsp;


    public ClassOrJsp(String name, boolean isJsp) {
        this.name = name;
        this.isJsp = isJsp;
    }


    public boolean isJsp() {
        return isJsp;
    }


    public String getName() {
        return name;
    }
}