/*
 * Local renderer for TTL internal diagrams. Not part of the library and not committed.
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstanceFactory;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import javax.imageio.ImageIO;

/** Paints one TTL tool with internal structure enabled. */
public final class TtlStructureShot {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private TtlStructureShot() {}

  /**
   * Renders {@code className} (for example {@code Ttl74393}) to {@code out}.
   *
   * @param className simple class name in this package
   * @param out png path
   */
  public static void render(String className, Path out) throws Exception {
    final var type = Class.forName("com.cburch.logisim.std.ttl." + className);
    final var factory = (InstanceFactory) type.getDeclaredConstructor().newInstance();
    final var attrs = factory.createAttributeSet();
    attrs.setValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE, Boolean.TRUE);
    final var bounds = factory.getOffsetBounds(attrs);
    final var pad = 8;
    final var scale = 3;
    final var image =
        new BufferedImage(
            (bounds.getWidth() + pad * 2) * scale,
            (bounds.getHeight() + pad * 2) * scale,
            BufferedImage.TYPE_INT_RGB);
    final var graphics = image.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    graphics.scale(scale, scale);
    graphics.translate(pad, pad - bounds.getY());
    graphics.setColor(Color.BLACK);
    final var comp = factory.createComponent(Location.create(0, 0, false), attrs);
    final var context = new ComponentDrawContext(null, null, null, graphics, graphics);
    context.setShowState(false);
    injectState(context);
    comp.draw(context);
    graphics.dispose();
    Files.createDirectories(out.getParent());
    ImageIO.write(image, "png", out.toFile());
  }

  private static void injectState(ComponentDrawContext context) throws Exception {
    final var unsafeClass = Class.forName("sun.misc.Unsafe");
    final var unsafeField = unsafeClass.getDeclaredField("theUnsafe");
    unsafeField.setAccessible(true);
    final var unsafe = unsafeField.get(null);
    final var allocate = unsafeClass.getMethod("allocateInstance", Class.class);
    final var state = (CircuitState) allocate.invoke(unsafe, CircuitState.class);
    final var dataField = CircuitState.class.getDeclaredField("componentData");
    final var offset = (long) unsafeClass.getMethod("objectFieldOffset", Field.class).invoke(unsafe, dataField);
    unsafeClass
        .getMethod("putObject", Object.class, long.class, Object.class)
        .invoke(unsafe, state, offset, new HashMap<Object, Object>());
    final var ctxField = ComponentDrawContext.class.getDeclaredField("circuitState");
    final var ctxOffset = (long) unsafeClass.getMethod("objectFieldOffset", Field.class).invoke(unsafe, ctxField);
    unsafeClass
        .getMethod("putObject", Object.class, long.class, Object.class)
        .invoke(unsafe, context, ctxOffset, state);
  }

  /**
   * Entry point used by the unit test. Reads {@code TTL_SHOT} and {@code TTL_OUT}.
   *
   * @param args unused
   */
  public static void main(String[] args) throws Exception {
    final var name = System.getenv("TTL_SHOT");
    final var out = System.getenv("TTL_OUT");
    if (name == null || out == null) {
      throw new IllegalStateException("TTL_SHOT and TTL_OUT are required");
    }
    render(name, Path.of(out));
  }
}
