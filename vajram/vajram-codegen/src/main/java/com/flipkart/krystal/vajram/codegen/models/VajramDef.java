package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.codegen.utils.CodegenUtils;
import com.flipkart.krystal.vajram.inputs.Resolve;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public record VajramDef(String vajramName, List<Method> resolveMethods, Method vajramLogic) {

    public static VajramDef fromVajram(VajramInputFile inputFile)  {
        String packageName = CodegenUtils.getPackageFromPath(inputFile.inputFilePath().relativeFilePath());
        Class<? extends Vajram> result = null;

        try {
            File f = new File("/Users/prateek.kumar/Projects/flipkart/Krystal/vajram/vajram-samples/");
            URL[] cp = {f.toURI().toURL()};
            try (URLClassLoader urlcl = new URLClassLoader(cp)) {
                result = (Class<? extends Vajram>) urlcl.loadClass(packageName + "." + inputFile.vajramName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (ClassNotFoundException | MalformedURLException e) {
            throw new RuntimeException(e);
        }

        List<Method> resolveMethods = new ArrayList<>();
        Method vajramLogic = null ;
        for(Method method : result.getMethods()) {
            if (method.isAnnotationPresent(Resolve.class)) {
                resolveMethods.add(method);
            } else if (method.isAnnotationPresent(VajramLogic.class) ) {
                if (vajramLogic == null) {
                    vajramLogic = method;
                } else {
                    throw new RuntimeException("Multiple VajramLogic annotated methods found in " + result.getClass().getSimpleName());
                }
            }
        }
        return new VajramDef(inputFile.vajramName(), resolveMethods, vajramLogic);
    }
}
