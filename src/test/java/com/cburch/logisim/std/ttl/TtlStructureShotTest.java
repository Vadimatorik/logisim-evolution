/*
 * Local renderer for TTL internal diagrams. Not part of the library and not committed.
 */

package com.cburch.logisim.std.ttl;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TtlStructureShotTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  @Test
  void renderRequestedChip() throws Exception {
    final var name = System.getenv("TTL_SHOT");
    if (name == null || name.isBlank()) {
      return;
    }
    final var out = System.getenv().getOrDefault("TTL_OUT", "/tmp/ttl-png/" + name + ".png");
    TtlStructureShot.render(name, Path.of(out));
  }
}
