/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import java.awt.Color;
import java.awt.Graphics;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 744050: hex non-inverting high-to-low level shifter.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC4050.pdf">74HC4050</a> data sheet.
 * Each output copies its input. Pin 1 is VCC, pin 8 is GND, and pins 13 and 16 are not connected.
 * Inputs that tolerate a higher voltage than VCC, and the high-to-low level shift itself, are
 * electrical properties and are not part of this digital model. An unknown or error value on an
 * input is copied to the matching output.
 */
public class Ttl744050 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744050";

  public static final int DELAY = 1;

  /** Logical port of output 1Y, datasheet pin 2. */
  public static final int PORT_INDEX_Y1 = 0;

  /** Logical port of input 1A, datasheet pin 3. */
  public static final int PORT_INDEX_A1 = 1;

  /** Logical port of output 2Y, datasheet pin 4. */
  public static final int PORT_INDEX_Y2 = 2;

  /** Logical port of input 2A, datasheet pin 5. */
  public static final int PORT_INDEX_A2 = 3;

  /** Logical port of output 3Y, datasheet pin 6. */
  public static final int PORT_INDEX_Y3 = 4;

  /** Logical port of input 3A, datasheet pin 7. */
  public static final int PORT_INDEX_A3 = 5;

  /** Logical port of input 4A, datasheet pin 9. */
  public static final int PORT_INDEX_A4 = 6;

  /** Logical port of output 4Y, datasheet pin 10. */
  public static final int PORT_INDEX_Y4 = 7;

  /** Logical port of input 5A, datasheet pin 11. */
  public static final int PORT_INDEX_A5 = 8;

  /** Logical port of output 5Y, datasheet pin 12. */
  public static final int PORT_INDEX_Y5 = 9;

  /** Logical port of input 6A, datasheet pin 14. */
  public static final int PORT_INDEX_A6 = 10;

  /** Logical port of output 6Y, datasheet pin 15. */
  public static final int PORT_INDEX_Y6 = 11;

  /** Number of signal ports. Exposed GND and VCC follow these ports, in that order. */
  public static final int SIGNAL_PORT_COUNT = 12;

  /** Logical port of GND when the power-pin attribute is enabled. Datasheet pin 8. */
  public static final int PORT_INDEX_GND = SIGNAL_PORT_COUNT;

  /** Logical port of VCC when the power-pin attribute is enabled. Datasheet pin 1. */
  public static final int PORT_INDEX_VCC = SIGNAL_PORT_COUNT + 1;

  private static final byte[] OUTPUT_PINS = {2, 4, 6, 10, 12, 15};
  private static final byte[] UNUSED_PINS = {13, 16};
  private static final byte VCC_PIN = 1;
  private static final byte GND_PIN = 8;
  private static final int[] INPUTS = {
    PORT_INDEX_A1, PORT_INDEX_A2, PORT_INDEX_A3, PORT_INDEX_A4, PORT_INDEX_A5, PORT_INDEX_A6
  };
  private static final int[] OUTPUTS = {
    PORT_INDEX_Y1, PORT_INDEX_Y2, PORT_INDEX_Y3, PORT_INDEX_Y4, PORT_INDEX_Y5, PORT_INDEX_Y6
  };
  private static final String[] PORT_NAMES = {
    "1Y output",
    "1A input",
    "2Y output",
    "2A input",
    "3Y output",
    "3A input",
    "4A input",
    "4Y output",
    "5A input",
    "5Y output",
    "6A input",
    "6Y output"
  };

  /** Creates a 744050 hex non-inverting buffer. */
  public Ttl744050() {
    super(
        _ID,
        (byte) 16,
        OUTPUT_PINS,
        UNUSED_PINS,
        PORT_NAMES,
        VCC_PIN,
        GND_PIN,
        new Ttl744050HdlGenerator());
  }

    @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var bodyFont = g.getFont();
    g.setFont(bodyFont.deriveFont(9f));
    g.drawRect((x + 12), (y + 18), 142, 24);
    GraphicsUtil.drawCenteredText(g, "REG", (x + 83), (y + 30));
    g.setFont(bodyFont);
    final var top = y + PIN_HEIGHT;
    final var bottom = y + height - PIN_HEIGHT;
    g.drawLine((x + 30), bottom, (x + 30), (y + 42));
    g.drawLine((x + 50), bottom, (x + 50), (y + 42));
    g.drawLine((x + 70), bottom, (x + 70), (y + 42));
    g.drawLine((x + 90), bottom, (x + 90), (y + 42));
    g.drawLine((x + 110), bottom, (x + 110), (y + 42));
    g.drawLine((x + 130), bottom, (x + 130), (y + 42));
    g.drawLine((x + 150), top, (x + 150), (y + 18));
    g.drawLine((x + 130), top, (x + 130), (y + 18));
    g.drawLine((x + 110), top, (x + 110), (y + 18));
    g.drawLine((x + 90), top, (x + 90), (y + 18));
    g.drawLine((x + 50), top, (x + 50), (y + 18));
    g.drawLine((x + 30), top, (x + 30), (y + 18));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (var channel = 0; channel < INPUTS.length; channel++) {
      state.setPort(OUTPUTS[channel], state.getPortValue(INPUTS[channel]), DELAY);
    }
  }

  private static int structureX(int originX, int pins, int pin) {
    if (pin <= pins / 2) {
      return originX + (pin - 1) * 20 + 10;
    }
    return originX + (pins - pin) * 20 + 10;
  }

  private static int structureY(int originY, int bodyHeight, int pins, int pin) {
    if (pin <= pins / 2) {
      return originY + bodyHeight - AbstractTtlGate.PIN_HEIGHT;
    }
    return originY + AbstractTtlGate.PIN_HEIGHT;
  }

  private static void structureBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 2, centerY - 2, 4, 4);
  }

  private static void structureDot(
      Graphics g, int centerX, int centerY, TtlRegisterData data, int word, int bit) {
    if (data == null) {
      return;
    }
    final var value = data.getValue(word);
    if (bit < 0 || bit >= value.getWidth()) {
      return;
    }
    final var bitValue = value.get(bit);
    g.setColor(bitValue.getColor());
    g.fillOval(centerX - 3, centerY - 3, 6, 6);
    g.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(g, bitValue.toDisplayString(), centerX, centerY);
    g.setColor(Color.BLACK);
  }

}
