package com.flipkart.krystal.datatypes;

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
  };

  public static Trilean toTrilean(boolean b) {
    return b ? TRUE : FALSE;
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
