package com.flipkart.krystal.vajram.codegen;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class VajramCodeGenTask {

  private final Path classesDir;
  private final Path javaDir;
  private final URLClassLoader urlClassLoader;

  public static void main(String[] args) throws Exception {
    Options options = new Options();

    Option classesDirOpt =
        new Option("c", "classesDir", true, "Root Directory for all .class files");
    classesDirOpt.setRequired(true);
    options.addOption(classesDirOpt);

    Option javaSrcDir = new Option("j", "javaDir", true, "Root Directory for all .java files");
    javaSrcDir.setRequired(true);
    options.addOption(javaSrcDir);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd = null; // not a good practice, it serves it purpose

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("Vajram Code Generator", options);
      System.exit(1);
    }

    codeGenModels(cmd.getOptionValue("classesDir"), cmd.getOptionValue("javaDir"));
  }

  public static void codeGenModels(String classesDir, String javaDir) throws Exception {
    new VajramCodeGenTask(Path.of(classesDir), Path.of(javaDir)).codeGenModels();
  }

  public VajramCodeGenTask(Path classesDir, Path javaDir) throws MalformedURLException {
    this.classesDir = classesDir;
    this.javaDir = javaDir;
    this.urlClassLoader =
        new URLClassLoader(
            new URL[] {classesDir.toFile().toURI().toURL()}, this.getClass().getClassLoader());
  }

  private void codeGenModels() throws Exception {
    ImmutableList<Vajram<?>> vajrams = getVajrams(urlClassLoader);
    for (Vajram<?> vajram : vajrams) {
      codeGenRequest(vajram);
      codeGenUtil(vajram);
    }
  }

  private void codeGenRequest(Vajram<?> vajram) throws IOException {
    VajramCodeGenerator vajramCodeGenerator = new VajramCodeGenerator(vajram, urlClassLoader);
    File vajramJavaDir =
        Paths.get(javaDir.toString(), vajramCodeGenerator.getPackageName().split("\\.")).toFile();
    if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
      String vajramRequestJavaCode = vajramCodeGenerator.codeGenVajramRequest();
      File vajramImplSourceFile =
          new File(vajramJavaDir, vajramCodeGenerator.getRequestClassName() + ".java");
      Files.writeString(
          vajramImplSourceFile.toPath(), vajramRequestJavaCode, CREATE, TRUNCATE_EXISTING, WRITE);
    }
  }

  private void codeGenUtil(Vajram<?> vajram) throws IOException {
    VajramCodeGenerator vajramCodeGenerator = new VajramCodeGenerator(vajram, urlClassLoader);
    File vajramJavaDir =
        Paths.get(javaDir.toString(), vajramCodeGenerator.getPackageName().split("\\.")).toFile();
    if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
      String vajramRequestJavaCode = vajramCodeGenerator.codeGenInputUtil();
      File vajramImplSourceFile =
          new File(vajramJavaDir, vajramCodeGenerator.getInputUtilClassName() + ".java");
      Files.writeString(
          vajramImplSourceFile.toPath(), vajramRequestJavaCode, CREATE, TRUNCATE_EXISTING, WRITE);
    }
  }

  ImmutableList<Vajram<?>> getVajrams(URLClassLoader urlClassLoader) throws Exception {
    List<Vajram<?>> vajrams = new ArrayList<>();
    Set<String> classNames = getClassNames();
    for (String className : classNames) {
      Class<?> clazz;
      try {
        clazz = urlClassLoader.loadClass(className);
      } catch (Throwable ignored) {
        continue;
      }
      if (Vajram.class.isAssignableFrom(clazz)) {
        if (IOVajram.class.equals(clazz.getSuperclass())
            || ComputeVajram.class.equals(clazz.getSuperclass())) {
          Vajram<?> vajram = (Vajram<?>) clazz.getConstructor().newInstance();
          vajrams.add(vajram);
        }
      }
    }
    return ImmutableList.copyOf(vajrams);
  }

  private Set<String> getClassNames() throws IOException {
    Set<String> classNames = new LinkedHashSet<>();
    try (Stream<Path> classFileStream =
        Files.find(
            classesDir,
            100,
            (path, fileAttributes) ->
                fileAttributes.isRegularFile() && path.toString().endsWith(".class"))) {
      classFileStream.forEach(
          path -> {
            Path relativePath = classesDir.relativize(path);
            String packageName =
                IntStream.range(0, relativePath.getNameCount())
                    .mapToObj(index -> relativePath.getName(index).toString())
                    .collect(Collectors.joining("."));
            classNames.add(packageName.substring(0, packageName.lastIndexOf(".class")));
          });
    }
    return classNames;
  }
}
