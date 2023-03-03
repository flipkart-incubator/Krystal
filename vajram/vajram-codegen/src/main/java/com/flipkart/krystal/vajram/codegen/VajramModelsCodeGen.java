package com.flipkart.krystal.vajram.codegen;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.codegen.models.ParsedVajramData;
import com.flipkart.krystal.vajram.codegen.models.VajramInputFile;
import com.flipkart.krystal.vajram.codegen.models.VajramInputsDef;
import com.flipkart.krystal.vajram.codegen.utils.CodegenUtils;
import com.flipkart.krystal.vajram.codegen.utils.Constants;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

@Slf4j
public class VajramModelsCodeGen {

    public static final String INPUTS_FILE_EXTENSION = ".vajram.yaml";
    private final List<Path> srcDirs;
    private final Path javaDir;
    private final Path compiledDir;

    //  private static VajramModelsCodeGen vajramModelsCodeGen = null;

    public static void main(String[] args) throws Exception {
        Options options = new Options();

        Option classesDirOpt = new Option("c", "classesDir", true, "Root Directory for all .class files");
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
            log.error("Command line options could not be parsed", e);
            formatter.printHelp("Vajram Code Generator", options);
            System.exit(1);
        }

        //        codeGenModels(cmd.getOptionValue("classesDir"), cmd.getOptionValue("javaDir"));
    }

    public static void codeGenModels(Set<File> srcDirs, String destinationDir) throws Exception {
        //      vajramModelsCodeGen = new VajramModelsCodeGen(srcDirs.stream().map(File::toPath).toList(), null, Path.of(destinationDir));
        new VajramModelsCodeGen(srcDirs.stream().map(File::toPath).toList(), null, Path.of(destinationDir)).codeGenModels();
    }

    public static void codeGenVajramImpl(Set<File> srcDirs, String compiledDir, String destinationDir) throws Exception {
        new VajramModelsCodeGen(srcDirs.stream().map(File::toPath).toList(), Path.of(compiledDir), Path.of(destinationDir)).codeGenVajramImpl();
    }

    public VajramModelsCodeGen(List<Path> srcDirs, Path compiledDir, Path javaDir) {
        this.compiledDir = compiledDir;
        this.srcDirs = Collections.unmodifiableList(srcDirs);
        this.javaDir = javaDir;
    }

    private void codeGenModels() throws Exception {
        ImmutableList<VajramInputFile> inputFiles = getInputDefinitions();
        for (VajramInputFile inputFile : inputFiles) {
            try {
                VajramCodeGenerator vajramCodeGenerator = new VajramCodeGenerator(inputFile, Collections.emptyMap(),
                        Collections.emptyMap());
                codeGenRequest(vajramCodeGenerator);
                codeGenUtil(vajramCodeGenerator);
            } catch (Exception e) {
                throw new RuntimeException("Could not generate code for file %s".formatted(inputFile.inputFilePath().relativeFilePath()),
                        e);
            }
        }
    }


    private void codeGenRequest(VajramCodeGenerator vajramCodeGenerator) throws IOException {
        File vajramJavaDir = Paths.get(javaDir.toString(), vajramCodeGenerator.getPackageName().split("\\.")).toFile();
        if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
            String vajramRequestJavaCode = vajramCodeGenerator.codeGenVajramRequest();
            File vajramImplSourceFile = new File(vajramJavaDir, vajramCodeGenerator.getRequestClassName() + Constants.JAVA_EXT);
            Files.writeString(vajramImplSourceFile.toPath(), vajramRequestJavaCode, CREATE, TRUNCATE_EXISTING, WRITE);
        }
    }

    private void codeGenVajramImpl() throws Exception {
        ImmutableList<VajramInputFile> inputFiles = getInputDefinitions();
        ClassLoader systemClassLoader = VajramID.class.getClassLoader();
        URL[] cp = new URL[srcDirs.size() + 1];
        int i = 0;
        for (Path srcDir : srcDirs) {
            cp[i] = srcDir.toFile().toURI().toURL();
            i++;
        }
        cp[i] = compiledDir.toFile().toURI().toURL();

        try (URLClassLoader urlcl = new URLClassLoader(cp, systemClassLoader)) {
            Map<String, ParsedVajramData> vajramDefs = new HashMap<>();
            Map<String, ImmutableList<VajramInputDefinition>> vajramInputsDef = new HashMap<>();
            inputFiles.forEach(vajramInputFile -> {
                Optional<ParsedVajramData> parsedVajramData = ParsedVajramData.fromVajram(urlcl, vajramInputFile);
                if (parsedVajramData.isEmpty()) {
                    log.warn("VajramImpl codegen will be skipped for {}. ", vajramInputFile.vajramName());
                } else {
                    vajramDefs.put(parsedVajramData.get().vajramName(), parsedVajramData.get());
                    vajramInputsDef.put(vajramInputFile.vajramName(),
                            vajramInputFile.vajramInputsDef().allInputsDefinitions());
                }
            });
            inputFiles.forEach(inputFile -> {
                // check to call VajramImpl codegen if Vajram class exists
                if (vajramDefs.containsKey(inputFile.vajramName())) {
                    try {
                        VajramCodeGenerator vajramCodeGenerator = new VajramCodeGenerator(inputFile, vajramDefs,
                                vajramInputsDef);
                        codeGenVajramImpl(vajramCodeGenerator, urlcl);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not generate vajram impl for file %s".formatted(
                                inputFile.inputFilePath().relativeFilePath()), e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Exception while generating vajram impl", e);
        }

    }

    private void codeGenVajramImpl(VajramCodeGenerator vajramCodeGenerator, ClassLoader classLoader) throws IOException {
        File vajramJavaDir = Paths.get(javaDir.toString(), vajramCodeGenerator.getPackageName().split("\\.")).toFile();
        if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
            String vajramImplCode = vajramCodeGenerator.codeGenVajramImpl(classLoader);
            File vajramImplSourceFile = new File(vajramJavaDir,
                    CodegenUtils.getVajramImplClassName(vajramCodeGenerator.getVajramName()) + Constants.JAVA_EXT);
            Files.writeString(vajramImplSourceFile.toPath(), vajramImplCode, CREATE, TRUNCATE_EXISTING, WRITE);
        }
    }

    private void codeGenUtil(VajramCodeGenerator vajramCodeGenerator) throws IOException {
        File vajramJavaDir = Paths.get(javaDir.toString(), vajramCodeGenerator.getPackageName().split("\\.")).toFile();
        if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
            String vajramRequestJavaCode = vajramCodeGenerator.codeGenInputUtil();
            File vajramImplSourceFile = new File(vajramJavaDir,
                    CodegenUtils.getInputUtilClassName(vajramCodeGenerator.getVajramName()) + Constants.JAVA_EXT);
            Files.writeString(vajramImplSourceFile.toPath(), vajramRequestJavaCode, CREATE, TRUNCATE_EXISTING, WRITE);
        }
    }

    ImmutableList<VajramInputFile> getInputDefinitions() throws Exception {
        List<VajramInputFile> vajramInputFiles = new ArrayList<>();
        Set<VajramInputsDef.InputFilePath> inputFiles = getInputsYamlFiles();
        for (VajramInputsDef.InputFilePath inputFile : inputFiles) {
            String fileName = inputFile.relativeFilePath().getFileName().toString();
            String vajramName = fileName.substring(0, fileName.length() - INPUTS_FILE_EXTENSION.length());
            vajramInputFiles.add(new VajramInputFile(vajramName, inputFile,
                    VajramInputsDef.from(inputFile.absolutePath().toFile())));
        }
        return ImmutableList.copyOf(vajramInputFiles);
    }

    private Set<VajramInputsDef.InputFilePath> getInputsYamlFiles() throws IOException {
        Set<VajramInputsDef.InputFilePath> inputFilePaths = new LinkedHashSet<>();
        for (Path srcDir : srcDirs) {
            if (srcDir.toFile().isDirectory()) {
                try (Stream<Path> vajramInputPathStream = Files.find(srcDir, 100, (path, fileAttributes) -> { // Get all vajram_input files
                    if (!fileAttributes.isRegularFile()) {
                        return false;
                    }
                    // All vajram inputs files should have '.vajram_inputs.yaml' extension
                    return path.getFileName().toString().endsWith(INPUTS_FILE_EXTENSION);
                })) {
                    vajramInputPathStream.forEach(
                            p -> inputFilePaths.add(new VajramInputsDef.InputFilePath(srcDir, srcDir.relativize(p))));
                }
            }
        }
        return inputFilePaths;
    }
}
