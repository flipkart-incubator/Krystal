package com.flipkart.krystal.vajram.codegen.utils;

import static com.flipkart.krystal.vajram.codegen.utils.Constants.ALL_INPUTS;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.COMMON_INPUTS;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUTS_NEEDING_MODULATION;

import com.google.common.base.CaseFormat;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class CodegenUtils {

  public static final String DOT = ".";
  public static final String COMMA = ",";
  private static final Pattern COMPILE = Pattern.compile(".");
  public static final String REQUEST = "Request";
  public static final String IMPL = "Impl";
  public static final String INPUT_UTIL = "InputUtil";
  public static final String VAJRAM = "vajram";
  public static final String CONVERTER = "CONVERTER";

  private CodegenUtils() {}

  public static String getPackageFromPath(Path filePath) {
    Path parentDir = filePath.getParent();
    return IntStream.range(0, parentDir.getNameCount())
        .mapToObj(i -> parentDir.getName(i).toString())
        .collect(Collectors.joining(DOT));
  }

  public static String getInputUtilClassName(String vajramName) {
    return (vajramName.toLowerCase().endsWith(VAJRAM)
            ? vajramName.substring(0, vajramName.length() - 6)
            : vajramName)
        + INPUT_UTIL;
  }

  public static String getRequestClassName(String vajramName) {
    return (vajramName.toLowerCase().endsWith(VAJRAM)
            ? vajramName.substring(0, vajramName.length() - 6)
            : vajramName)
        + REQUEST;
  }

  public static String getVajramImplClassName(String vajramName) {
    return vajramName + IMPL;
  }

  public static String getVajramBaseName(String vajramName) {
    return (vajramName.toLowerCase().endsWith(VAJRAM)
        ? vajramName.substring(0, vajramName.length() - 6)
        : vajramName);
  }

  public static String getVajramNameFromClass(String vajramClassFullName) {
    String[] splits = COMPILE.split(vajramClassFullName);
    return splits[splits.length - 1];
  }

  public static String toJavaName(String input) {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, input);
  }

  public static String getAllInputsClassname(String vajramName) {
    return getVajramBaseName(vajramName) + ALL_INPUTS;
  }

  public static String getCommonInputsClassname(String vajramName) {
    return getVajramBaseName(vajramName) + COMMON_INPUTS;
  }

  public static String getInputModulationClassname(String vajramName) {
    return getVajramBaseName(vajramName) + INPUTS_NEEDING_MODULATION;
  }
}
