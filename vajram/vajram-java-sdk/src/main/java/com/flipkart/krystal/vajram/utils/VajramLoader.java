package com.flipkart.krystal.vajram.utils;

import static java.lang.reflect.Modifier.isFinal;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.VajramInitData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

@UtilityClass
public final class VajramLoader {

  /**
   * Loads vajrams from the given packages and for the given types
   *
   * @param packages packages from which all vajrams are to be loaded (including sub-packages to
   *     full depth)
   * @param vajramDefClasses Vajram types of vajrams to be loaded
   * @return Loaded vajram objects
   */
  public static List<? extends VajramDefRoot<Object>> loadVajrams(
      Set<String> packages, Collection<Class<? extends VajramDefRoot>> vajramDefClasses) {
    List<VajramDefRoot<Object>> vajrams =
        new ArrayList<>(loadAllVajrams(new Reflections(ConfigurationBuilder.build(packages))));

    Set<String> containingPackages =
        vajramDefClasses.stream()
            .map(c -> requireNonNull(c.getPackage()).getName())
            .collect(Collectors.toSet());
    Reflections reflections = new Reflections(ConfigurationBuilder.build(containingPackages));
    vajramDefClasses.stream()
        .map(vajramDefClass -> loadVajram(vajramDefClass, reflections))
        .forEach(vajrams::add);

    return vajrams;
  }

  private static List<? extends VajramDefRoot<Object>> loadAllVajrams(Reflections reflections) {
    return reflections.getSubTypesOf(VajramDefRoot.class).stream()
        .filter(c -> isFinal(c.getModifiers()))
        .map(VajramLoader::initVajram)
        .toList();
  }

  private static VajramDefRoot<Object> loadVajram(
      Class<? extends VajramDefRoot> clazz, Reflections reflections) {
    List<Class<? extends VajramDefRoot>> impls =
        reflections.getSubTypesOf(clazz).stream()
            .filter(subclass -> isFinal(subclass.getModifiers()))
            .filter(
                // Since multiple vajrams can implement a Trait, we need to pick only those
                // class which are immediate subtypes of the requested class to get the actual
                // wrapper class which can used to create the vajram object
                subType ->
                    clazz.equals(subType.getSuperclass())
                        || Arrays.asList(subType.getInterfaces()).contains(clazz))
            .<Class<? extends VajramDefRoot>>map(subType -> subType.asSubclass(VajramDefRoot.class))
            .toList();
    if (impls.size() > 1) {
      throw new IllegalArgumentException(
          "Multiple Vajram Impl found in the package '%s' of the provided class: %s"
              .formatted(clazz.getPackageName(), impls));
    } else if (impls.isEmpty()) {
      throw new IllegalArgumentException(
          "No Vajram Impl found in the package '%s' of the provided class: '%s'"
              .formatted(clazz.getPackageName(), clazz));
    }
    return initVajram(impls.get(0));
  }

  @SuppressWarnings("unchecked")
  private static VajramDefRoot<Object> initVajram(Class<? extends VajramDefRoot> aClass) {
    if (!isFinal(aClass.getModifiers())) {
      throw new RuntimeException("Provided Vajram impl class should be final");
    }
    try {
      return aClass.getConstructor(VajramInitData.class).newInstance(new VajramInitData());
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
