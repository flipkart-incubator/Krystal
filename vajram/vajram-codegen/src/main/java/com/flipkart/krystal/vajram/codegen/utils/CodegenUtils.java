package com.flipkart.krystal.vajram.codegen.utils;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class CodegenUtils {

    private CodegenUtils() {
    }

    public static String getPackageFromPath(Path filePath) {
        Path parentDir = filePath.getParent();
        return IntStream.range(0, parentDir.getNameCount())
                .mapToObj(i -> parentDir.getName(i).toString())
                .collect(Collectors.joining("."));
    }
}
