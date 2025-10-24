package com.flipkart.krystal.vajram.sql.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGeneratorProvider;
import com.google.auto.service.AutoService;

@AutoService(VajramCodeGeneratorProvider.class)
public class SqlTraitVajramGeneratorProvider implements VajramCodeGeneratorProvider {

  @Override
  public CodeGenerator create(VajramCodeGenContext codeGenContext) {
    return new DefaultSqlTraitVajramGenerator(codeGenContext);
  }
}
