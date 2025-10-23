package com.flipkart.krystal.datatypes;

import org.checkerframework.checker.nullness.qual.Nullable;

/** Implements <a href="https://en.wikipedia.org/wiki/Three-valued_logic">Trilean Logic</a> */
public enum Trilean {
  UNKNOWN {
    @Override
    public Trilean or(Trilean other) {
      return switch (other) {
        case TRUE -> TRUE;
        case UNKNOWN, FALSE -> UNKNOWN;
      };
    }

    @Override
    public Trilean and(Trilean other) {
      return switch (other) {
        case UNKNOWN, TRUE -> UNKNOWN;
        case FALSE -> FALSE;
      };
    }

    @Override
    public Trilean negation() {
      return UNKNOWN;
    }

    @Override
    public String toString() {
      return "unknown";
    }
  },

  TRUE {
    @Override
    public Trilean or(Trilean other) {
      return TRUE;
    }

    @Override
    public Trilean and(Trilean other) {
      return other;
    }

    @Override
    public Trilean negation() {
      return FALSE;
    }

    @Override
    public String toString() {
      return "true";
    }
  },

  FALSE {
    @Override
    public Trilean or(Trilean other) {
      return other;
    }

    @Override
    public Trilean and(Trilean other) {
      return FALSE;
    }

    @Override
    public Trilean negation() {
      return TRUE;
    }

    @Override
    public String toString() {
      return "false";
    }
  };

  public static Trilean toTrilean(boolean bool) {
    if (bool) {
      return TRUE;
    } else {
      return FALSE;
    }
  }

  public static Trilean toTrilean(@Nullable Boolean b) {
    if (b == null) {
      return UNKNOWN;
    }
    return toTrilean((boolean) b);
  }

  public abstract Trilean or(Trilean other);

  public Trilean or(boolean other) {
    return or(toTrilean(other));
  }

  public abstract Trilean and(Trilean other);

  public Trilean and(boolean other) {
    return and(toTrilean(other));
  }

  public abstract Trilean negation();

  public static Trilean not(Trilean t) {
    return t.negation();
  }

  public static Trilean orOf(Trilean... trileans) {
    Trilean value = FALSE;
    for (Trilean t : trileans) {
      if (value == TRUE) {
        return value;
      }
      value = value.or(t);
    }
    return value;
  }

  public static Trilean andOf(Trilean... trileans) {
    Trilean value = TRUE;
    for (Trilean t : trileans) {
      if (value == FALSE) {
        return value;
      }
      value = value.or(t);
    }
    return value;
  }
}
