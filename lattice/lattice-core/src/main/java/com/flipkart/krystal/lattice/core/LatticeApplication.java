package com.flipkart.krystal.lattice.core;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.lang.reflect.Modifier.isAbstract;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder;
import com.flipkart.krystal.lattice.core.di.DependencyInjector;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantConfig;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilder;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilderWithAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantSpecBuilderWithConfig;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopantSpecBuilder;
import com.flipkart.krystal.lattice.core.doping.SpecBuilders;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * A lattice application hosts a Krystal graph as a process. As part of the application setup, it
 * allows application owners to add functionality to the graph and control how the graph executes
 * (via {@link Dopant dopants})
 *
 * <p>For example, an application owner might choose to "dope" their application with a "Server
 * Dopant" which exposes some of the hosted vajrams for invocation from outside the application
 * process - thus converting the krystal graph into a web server.
 */
@Singleton
@Slf4j
public abstract class LatticeApplication {

  private final ObjectMapper configMapper =
      YAMLMapper.builder()
          .build()
          .registerModule(new GuavaModule())
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module())
          .setSerializationInclusion(NON_NULL)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
          .disable(SerializationFeature.FAIL_ON_SELF_REFERENCES);

  public abstract DependencyInjectionBinder getDependencyInjectionBinder();

  @SuppressWarnings("unchecked")
  public final void init(String[] args) throws Exception {
    DependencyInjectionBinder dependencyInjectionBinder = getDependencyInjectionBinder();
    dependencyInjectionBinder.bindToInstance(
        DependencyInjectionBinder.class, dependencyInjectionBinder);
    Option latticeConfigFileOption =
        new Option("l", "lattice_config_file", true, "Lattice app config file");
    CommandLine commandLine =
        new DefaultParser().parse(new Options().addOption(latticeConfigFileOption), args);
    String latticeConfigFile = commandLine.getOptionValue(latticeConfigFileOption);
    LatticeAppBootstrap bootstrap = new LatticeAppBootstrap();
    bootstrap(bootstrap);
    ImmutableMap<Class<? extends DopantSpecBuilder>, DopantSpecBuilder> allSpecBuilders =
        ImmutableMap.copyOf(bootstrap.specBuilders());

    BiMap<String, Class<? extends DopantConfig>> configTypesByDopantTypes =
        getConfigTypesByDopantTypes(allSpecBuilders);
    configMapper.registerSubtypes(
        configTypesByDopantTypes.entrySet().stream()
            .map(e -> new NamedType(e.getValue(), e.getKey()))
            .toArray(NamedType[]::new));

    ImmutableMap<String, Annotation> annotationsByDopantType = getAppAnnotationsByDopantType();
    annotationsByDopantType.forEach(
        (dopantType, annotation) ->
            dependencyInjectionBinder.bindToInstance(
                (Class<Annotation>) annotation.annotationType(), annotation));

    LatticeAppConfig latticeAppConfig;
    System.err.println("Lattice app args in APP: " + Arrays.deepToString(args));
    if (latticeConfigFile == null) {
      latticeAppConfig = new LatticeAppConfig();
    } else {
      URL configResource =
          requireNonNull(this.getClass().getClassLoader()).getResource(latticeConfigFile);
      if (configResource == null) {
        latticeAppConfig = new LatticeAppConfig();

      } else {
        latticeAppConfig = configMapper.readValue(configResource, LatticeAppConfig.class);
      }
    }
    latticeAppConfig
        .configsByDopantType()
        .forEach(
            (dopantType, config) -> {
              Class<DopantConfig> configType =
                  (Class<DopantConfig>) configTypesByDopantTypes.get(dopantType);
              if (configType != null) {
                dependencyInjectionBinder.bindToInstance(configType, config);
              }
            });
    Collection<DopantSpecBuilder> currentIteration = new ArrayList<>(allSpecBuilders.values());
    Map<Class<? extends DopantSpecBuilder>, DopantSpecBuilder> accumulator =
        new LinkedHashMap<>(allSpecBuilders);
    while (!currentIteration.isEmpty()) {
      Map<Class<? extends DopantSpecBuilder<?, ?, ?>>, DopantSpecBuilder> newBatch =
          new LinkedHashMap<>();
      for (DopantSpecBuilder<?, ?, ?> specBuilder : currentIteration) {
        var additionalDopants = specBuilder.getAdditionalDopants();
        for (DopantSpecBuilder<?, ?, ?> additionalDopant : additionalDopants) {
          Class<? extends DopantSpecBuilder<?, ?, ?>> newType =
              (Class<? extends DopantSpecBuilder<?, ?, ?>>) additionalDopant.getClass();
          if (!accumulator.containsKey(newType)) {
            newBatch.put(newType, additionalDopant);
            accumulator.put(newType, additionalDopant);
          }
        }
      }
      currentIteration = newBatch.values();
    }

    Collection<DopantSpecBuilder> values = accumulator.values();
    SpecBuilders specBuilders = new SpecBuilders(allSpecBuilders);
    values.forEach(builder -> builder._configure(specBuilders));
    var specs =
        values.stream()
            .<Optional<DopantSpec>>map(
                builder -> {
                  boolean noAnnotation =
                      NoAnnotation.class.isAssignableFrom(builder._annotationType());
                  Class<?> configurationType = builder._configurationType();
                  boolean noConfig = NoConfiguration.class.isAssignableFrom(configurationType);
                  if (noAnnotation && noConfig) {
                    if (builder instanceof SimpleDopantSpecBuilder simpleDSB) {
                      return Optional.of(simpleDSB._buildSpec());
                    } else {
                      log.error(
                          "Expected '{}' to extend 'SimpleDopantSpecBuilder' as dopantSpec has no annotation and no configuration",
                          builder.getClass());
                    }
                  } else {
                    String dopantType = builder._dopantType();
                    if (noAnnotation) {
                      if (builder instanceof DopantSpecBuilderWithConfig builderWithConfig) {
                        DopantConfig dopantConfig =
                            latticeAppConfig.configsByDopantType().get(dopantType);
                        return Optional.of(builderWithConfig._buildSpec(dopantConfig));
                      } else {
                        log.error(
                            "Expected '{}' to extend 'DopantSpecBuilderWithConfig' dopantSpec has no annotation and has a configuration",
                            builder.getClass());
                      }
                    } else if (noConfig) {
                      if (builder instanceof DopantSpecBuilderWithAnnotation builderWithAnno) {
                        Annotation annotation = annotationsByDopantType.get(dopantType);
                        return Optional.of(builderWithAnno._buildSpec(annotation));
                      } else {
                        log.error(
                            "Expected '{}' to extend 'DopantSpecBuilderWithAnnotation' dopantSpec has no configuration and has an annotation",
                            builder.getClass());
                      }
                    } else {
                      Annotation annotation = annotationsByDopantType.get(dopantType);
                      DopantConfig dopantConfig =
                          latticeAppConfig.configsByDopantType().get(dopantType);
                      return Optional.of(builder._buildSpec(annotation, dopantConfig));
                    }
                  }
                  return Optional.empty();
                })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    for (DopantSpec<?, ?, ?> spec : specs) {
      @SuppressWarnings("unchecked")
      Class<DopantSpec<?, ?, ?>> aClass = (Class<DopantSpec<?, ?, ?>>) spec.getClass();
      dependencyInjectionBinder.bindToInstance(aClass, spec);
      var dopantClass = spec.dopantClass();
      if (!isAbstract(dopantClass.getModifiers())) {
        // abstract classes would need to bound to their implementations by clients
        dependencyInjectionBinder.bindInSingleton(dopantClass);
      }
    }

    DependencyInjector injector = dependencyInjectionBinder.getInjector();
    List<? extends Dopant<?, ?>> dopants =
        specs.stream()
            .map(
                spec -> {
                  Class<? extends Dopant<?, ?>> clazz = spec.dopantClass();
                  Dopant<?, ?> instance = injector.getInstance(clazz);
                  return instance;
                })
            .toList();
    for (Dopant<?, ?> dopant : dopants) {
      dopant.start(args);
    }
    var itr = dopants.listIterator(dopants.size());
    while (itr.hasPrevious()) {
      itr.previous().tryMainMethodExit();
    }
  }

  private ImmutableMap<String, Annotation> getAppAnnotationsByDopantType() {
    Annotation[] annotations = this.getClass().getAnnotations();
    Map<String, Annotation> dopantAnnotations = new LinkedHashMap<>();
    for (Annotation annotation : annotations) {
      DopantType dopantTypeAnno = annotation.annotationType().getAnnotation(DopantType.class);
      if (dopantTypeAnno != null) {
        dopantAnnotations.put(dopantTypeAnno.value(), annotation);
      }
    }
    return ImmutableMap.copyOf(dopantAnnotations);
  }

  public abstract void bootstrap(LatticeAppBootstrap bootstrap);

  private BiMap<String, Class<? extends DopantConfig>> getConfigTypesByDopantTypes(
      ImmutableMap<Class<? extends DopantSpecBuilder>, DopantSpecBuilder> allSpecBuilders) {
    BiMap<String, Class<? extends DopantConfig>> configTypesByName = HashBiMap.create();
    for (DopantSpecBuilder<?, ?, ?> specBuilder : allSpecBuilders.values()) {
      Class<? extends DopantConfig> configurationType = specBuilder._configurationType();
      if (NoConfiguration.class.isAssignableFrom(configurationType)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      DopantType dopantTypeAnno = configurationType.getAnnotation(DopantType.class);
      if (dopantTypeAnno == null) {
        log.error(
            "Configuration type '{}' of SpecBuilder Type '{}' doesn't have the annotation '{}'. Ignoring this configuration.",
            configurationType,
            specBuilder.getClass(),
            DopantType.class);
      } else {
        String dopantType = dopantTypeAnno.value();
        if (configTypesByName.containsKey(dopantType)) {
          log.error(
              "Dopant type '{}' of config class '{}' clashes with dopant type of config class '{}'. Ignoring this config type",
              dopantType,
              configurationType,
              configTypesByName.get(dopantType));
        } else {
          configTypesByName.put(dopantType, configurationType);
        }
      }
    }
    return configTypesByName;
  }
}
