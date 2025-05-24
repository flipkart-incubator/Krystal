package com.flipkart.krystal.datatypes;

/**
 * @see <a href="https://en.wikipedia.org/wiki/Three-valued_logic">Trilean Logic</a>
 */
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

  public static Trilean not(Trilean t) {
    return switch (t) {
      case UNKNOWN -> UNKNOWN;
      case TRUE -> FALSE;
      case FALSE -> TRUE;
    };
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
