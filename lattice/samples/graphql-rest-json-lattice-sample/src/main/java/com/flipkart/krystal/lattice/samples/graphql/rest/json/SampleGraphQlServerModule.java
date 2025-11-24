package com.flipkart.krystal.lattice.samples.graphql.rest.json;

import com.flipkart.krystal.lattice.core.runtime.MainClassLoaderHolder;
import com.flipkart.krystal.lattice.graphql.rest.dopant.GraphQlOverRestDopant;
import com.flipkart.krystal.vajram.graphql.api.schema.GraphQlLoader;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import graphql.GraphQL;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SampleGraphQlServerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(GraphQlOverRestDopant.class)
        .to(SampleGraphQlServer_GraphQlOverRestDopant_Impl.class)
        .in(Singleton.class);
  }

  @Provides
  @Singleton
  GraphQlLoader graphQlLoader() {
    return new GraphQlLoader();
  }

  @Provides
  @Singleton
  MainClassLoaderHolder mainClassLoaderHolder() {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    log.info("Main class loader: {}", classLoader);
    return new MainClassLoaderHolder(classLoader);
  }

  @Provides
  @Singleton
  TypeDefinitionRegistry provideTypeDefinitionRegistry(
      GraphQlLoader graphQlLoader, MainClassLoaderHolder mainClassLoaderHolder) {
    return graphQlLoader.getTypeDefinitionRegistry(mainClassLoaderHolder.classLoader());
  }

  @Provides
  @Singleton
  GraphQL graphQL(GraphQlLoader graphQlLoader, TypeDefinitionRegistry typeDefinitionRegistry) {
    return graphQlLoader.loadGraphQl(typeDefinitionRegistry);
  }
}
