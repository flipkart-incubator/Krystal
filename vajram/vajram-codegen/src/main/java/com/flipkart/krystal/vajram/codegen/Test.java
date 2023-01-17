package com.flipkart.krystal.vajram.codegen;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class Test {


    public static void main(String[] args) throws Exception {
        Path srcFile = Path.of("/Users/prateek.kumar/Projects/flipkart/Krystal/vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/samples");
        VajramModelsCodeGen vajramModelsCodeGen = new VajramModelsCodeGen(List.of(srcFile), Path.of("/Users/prateek.kumar/Projects/flipkart/Krystal/vajram/vajram-samples/build/generated/sources/vajrams/main/java/"));

        VajramModelsCodeGen.codeGenModels(Set.of(new File("/Users/prateek.kumar/Projects/flipkart/Krystal/vajram/vajram-samples/src/main/java/com/flipkart/krystal/vajram/")),
                "/Users/prateek.kumar/Projects/flipkart/Krystal/vajram/vajram-samples/build/generated/sources/vajrams/main/java/");
    }

}
