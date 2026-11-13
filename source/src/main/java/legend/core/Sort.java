package legend.core;

import java.util.Arrays;
import java.util.Comparator;

public final class Sort {
  private Sort() { }

  /**
   * Stable range sort through the public Java API. The work-buffer parameters
   * remain for compatibility with QueuePool; the runtime owns scratch storage.
   * Avoids reflective access to private JDK classes, which Android cannot supply.
   */
  public static <T> void sort(final T[] a, final int lo, final int hi, final Comparator<? super T> c, final T[] work, final int workBase, final int workLen) {
    Arrays.sort(a, lo, hi, c);
  }
}
