package com.flipkart.krystal.vajram.guice.executorscope;

import static com.flipkart.krystal.krystex.KrystalExecutor.getExecutorForCurrentThread;
import static com.google.common.base.Preconditions.checkState;

import com.flipkart.krystal.krystex.KrystalExecutor;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import java.util.HashMap;
import java.util.Map;

/**
 * Scopes a single execution of a block of code. Apply this scope with a try/finally block:
 *
 * <pre><code>
 *   try(var scopeInstance = scope.enter(krystalExecutor)){
 *     // explicitly seed some seed objects...
 *     scopeInstance.seed(Key.get(SomeObject.class), someObject);
 *     // create and access scoped objects
 *   }
 * </code></pre>
 *
 * The scope can be initialized with one or more seed values by calling <code>
 * seed(krystalExecutor, key, value)
 * </code> before the injector will be called upon to provide for this key. A typical use is for a
 * servlet filter to enter/exit the scope, representing a Request Scope, and seed HttpServletRequest
 * and HttpServletResponse. For each key inserted with seed(), you must include a corresponding
 * binding:
 *
 * <pre><code>
 *   bind(key)
 *       .toProvider(KrystalExecutorScope.&lt;KeyClass&gt;seededKeyProvider())
 *       .in(ScopeAnnotation.class);
 * </code></pre>
 *
 * @author Ram Anvesh
 */
public class KrystalExecutorScope implements Scope {

  private final Map<KrystalExecutor, Map<Key<?>, Object>> values = new HashMap<>();

  private static final Provider<Object> SEEDED_KEY_PROVIDER =
      () -> {
        throw new IllegalStateException(
            "If you got here then it means that"
                + " your code asked for scoped object which should have been"
                + " explicitly seeded in this scope by calling"
                + " KrystalExecutorScope.seed(), but was not.");
      };

  public ScopeInstance enter(KrystalExecutor krystalExecutor) {
    checkState(
        values.get(krystalExecutor) == null,
        "A krystal executor scoping block is already in progress");
    values.put(krystalExecutor, new HashMap<>());
    return new ScopeInstance(krystalExecutor);
  }

  public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
    return () -> {
      Map<Key<?>, Object> scopedObjects = getScopedObjectMap(getExecutor());

      @SuppressWarnings("unchecked")
      T current = (T) scopedObjects.get(key);
      if (current == null && !scopedObjects.containsKey(key)) {
        current = unscoped.get();

        // don't remember proxies; these exist only to serve circular dependencies
        if (Scopes.isCircularProxy(current)) {
          return current;
        }

        scopedObjects.put(key, current);
      }
      return current;
    };
  }

  /**
   * Returns a provider that always throws exception complaining that the object in question must be
   * seeded before it can be injected.
   *
   * @return typed provider
   */
  @SuppressWarnings("unchecked")
  public static <T> Provider<T> seededKeyProvider() {
    return (Provider<T>) SEEDED_KEY_PROVIDER;
  }

  private Map<Key<?>, Object> getScopedObjectMap(KrystalExecutor krystalExecutor) {
    return values.computeIfAbsent(krystalExecutor, _k -> new HashMap<>());
  }

  private static KrystalExecutor getExecutor() {
    KrystalExecutor executorForCurrentThread = getExecutorForCurrentThread();
    if (executorForCurrentThread == null) {
      throw new OutOfScopeException(
          "Could not retrieve krystal executor for current scope. This means we are not inside a Krystal executor scope");
    }
    return executorForCurrentThread;
  }

  public final class ScopeInstance implements AutoCloseable {

    private final KrystalExecutor krystalExecutor;

    public ScopeInstance(KrystalExecutor krystalExecutor) {
      this.krystalExecutor = krystalExecutor;
    }

    public <T> void seed(Key<T> key, T value) {
      Map<Key<?>, Object> scopedObjects = getScopedObjectMap(krystalExecutor);
      checkState(
          !scopedObjects.containsKey(key),
          "A value for the key %s was "
              + "already seeded in this scope. Old value: %s New value: %s",
          key,
          scopedObjects.get(key),
          value);
      scopedObjects.put(key, value);
    }

    public <T> void seed(Class<T> clazz, T value) {
      seed(Key.get(clazz), value);
    }

    @Override
    public void close() {
      checkState(
          values.remove(krystalExecutor) != null, "This krystal executor scope was already closed");
    }
  }
}
