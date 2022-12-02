package com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.tyrefactory;

import com.flipkart.krystal.vajram.exec.test_vajrams.carmaker.CarPart;

public class Tyre implements CarPart {
    private String maker;
    private Integer count;
    private Integer size;
    public Tyre(String maker, Integer count, Integer size) {
        this.maker = maker;
        this.count = count;
        this.size = size;
    }

    @Override
    public String getDetails() {
        return "Tyres: Brand - %s | Count - %s | size %s".formatted(maker, count, size);
    }
}
