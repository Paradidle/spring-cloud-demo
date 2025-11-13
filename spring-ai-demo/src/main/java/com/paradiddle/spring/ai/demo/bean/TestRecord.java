package com.paradiddle.spring.ai.demo.bean;

public record TestRecord(String type, String name) {

    public String getNameByType(String type){

        return switch (type) {
            case "1" -> "1";
            case "2" -> "2";
            default -> "3";
        };
    }

}
