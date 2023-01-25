package com.flipkart.krystal.vajram.codegen;

import com.flipkart.krystal.vajram.codegen.models.VajramDef;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class Test {


    public static void main(String[] args) throws Exception {
        Path srcFile = Path.of("/Users/prateek.kumar/Projects/flipkart/Krystal/vajram/vajram-samples/src/main/java");
        VajramModelsCodeGen vajramModelsCodeGen = new VajramModelsCodeGen(List.of(srcFile), Path.of("/Users/prateek.kumar/Projects/flipkart/Krystal/vajram/vajram-samples/build/generated/sources/vajrams/main/java/com/flipkart/krystal/vajram/samples/"));

        VajramModelsCodeGen.codeGenModels(Set.of(new File("/Users/prateek.kumar/Projects/flipkart/Krystal/vajram/vajram-samples/src/main/java")),
                "/Users/prateek.kumar/Projects/flipkart/Krystal/vajram/vajram-samples/build/generated/sources/vajrams/main/java/");

//        final VajramDef vajramDef = VajramDef.fromVajram("GreetingVajram");
//        VajramCodeGenerator codeGenerator =  new VajramCodeGenerator(vajramDef);
//        codeGenerator.createResolvers();
    }

}
