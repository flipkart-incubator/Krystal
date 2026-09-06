package com.flipkart.krystal.lattice.core;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.lattice.core.di.DependencyInjectionFramework;
import com.flipkart.krystal.lattice.core.di.InjectionValueProvider;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantConfig;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.datatype.guava.GuavaModule;

/**
 * A lattice application hosts a Krystal graph as a process. As part of the application setup, it
 * allows application owners to add functionality to the graph and control how the graph executes
 * (via {@link Dopant dopants})
 *
 * <p>For example, an application owner might choose to "dope" their application with a "Server
 * Dopant" which exposes some of the hosted vajrams for invocation from outside the application
 * process - thus converting the krystal graph into a web server.
 */
@Slf4j
public abstract class LatticeApplication {

  private @MonotonicNonNull YAMLMapper configMapper;

  private ImmutableList<String> args = ImmutableList.of();

  public abstract DependencyInjectionFramework getDependencyInjector();

  @SuppressWarnings("unchecked")
  public final int run(String[] args) throws Exception {
    this.args = ImmutableList.copyOf(args);

    DependencyInjectionFramework dependencyInjectionFramework = getDependencyInjector();

    System.err.println("Lattice app args in APP: " + Arrays.deepToString(args));

    InjectionValueProvider injector = dependencyInjectionFramework.getValueProvider();
    List<Dopant> dopants = injector.getInstance(LatticeDopantSet.class).valueOrThrow().dopants();
    for (Dopant<?, ?> dopant : dopants) {
      dopant.start(args);
    }
    var itr = dopants.listIterator(dopants.size());
    int exitCode = 0;
    while (itr.hasPrevious()) {
      int dopantExitCode = itr.previous().tryApplicationExit();
      if (exitCode == 0 && dopantExitCode != 0) {
        exitCode = dopantExitCode;
      }
    }
    return exitCode;
  }

  public LatticeAppConfig loadConfig(LatticeAppBootstrap latticeAppBootstrap) {
    YAMLMapper yamlMapper = getYAMLMapper(latticeAppBootstrap);
    Option latticeConfigFileOption =
        new Option("l", "lattice_config_file", true, "Lattice app config file");
    CommandLine commandLine;
    try {
      commandLine =
          new DefaultParser()
              .parse(new Options().addOption(latticeConfigFileOption), args.toArray(String[]::new));
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    String latticeConfigFile = commandLine.getOptionValue(latticeConfigFileOption);
    LatticeAppConfig latticeAppConfig;
    ClassLoader classLoader = requireNonNull(this.getClass().getClassLoader());
    if (latticeConfigFile == null) {
      latticeAppConfig = new LatticeAppConfig();
    } else {
      InputStream configResource = classLoader.getResourceAsStream(latticeConfigFile);
      if (configResource == null) {
        latticeAppConfig = new LatticeAppConfig();
      } else {
        latticeAppConfig = yamlMapper.readValue(configResource, LatticeAppConfig.class);
      }
    }
    return latticeAppConfig;
  }

  private YAMLMapper getYAMLMapper(LatticeAppBootstrap latticeAppBootstrap) {
    if (configMapper == null) {
      configMapper =
          YAMLMapper.builder()
              .addModule(new GuavaModule())
              .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(NON_NULL))
              .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .disable(SerializationFeature.FAIL_ON_SELF_REFERENCES)
              .registerSubtypes(
                  getConfigTypesByDopantTypes(latticeAppBootstrap.configuredSpecs().values())
                      .entrySet()
                      .stream()
                      .map(e -> new NamedType(e.getValue(), e.getKey()))
                      .toArray(NamedType[]::new))
              .build();
    }
    return configMapper;
  }

  private BiMap<String, Class<? extends DopantConfig>> getConfigTypesByDopantTypes(
      Collection<DopantSpec> allSpecBuilders) {
    BiMap<String, Class<? extends DopantConfig>> configTypesByName = HashBiMap.create();
    for (DopantSpec specBuilder : allSpecBuilders) {
      @SuppressWarnings("unchecked")
      Class<? extends DopantConfig> configurationType = specBuilder._configurationType();
      if (NoConfiguration.class.isAssignableFrom(configurationType)) {
        continue;
      }
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
