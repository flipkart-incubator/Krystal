package com.flipkart.krystal.vajram.exec.codegen;

import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.squareup.javapoet.MethodSpec;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.util.Set;

public class codegen {
    @Test
    public void generateVajramImpl(String packageName) {
        packageName = "com.flipkart.krystal.vajram.exec.codegen.samplevajram";
        Reflections reflections = new Reflections("my.project.prefix");

        Set<Class<? extends Object>> allClasses = reflections.getSubTypesOf(Object.class);
        for (Class<?> aclass : allClasses) {
            if (aclass.isAssignableFrom(NonBlockingVajram.class)) {
                MethodSpec sumOfTen = MethodSpec
                            .methodBuilder("sumOfTen")
                        .addStatement("int sum = 0")
                        .beginControlFlow("for (int i = 0; i <= 10; i++)")
                        .addStatement("sum += i")
                        .endControlFlow()
                        .build();
            }
        }
    }
}
