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
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + 30));
    g.drawLine((x + 50), (y + 30), (x + 34), (y + 30));
    g.drawLine((x + 34), (y + 24), (x + 46), (y + 30));
    g.drawLine((x + 34), (y + 36), (x + 46), (y + 30));
    g.drawLine((x + 34), (y + 24), (x + 34), (y + 36));
    g.drawLine((x + 48), (y + 30), (x + 30), (y + 30));
    g.drawLine((x + 30), (y + 30), (x + 30), (y + height - 7));
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + 30));
    g.drawLine((x + 90), (y + 30), (x + 74), (y + 30));
    g.drawLine((x + 74), (y + 24), (x + 86), (y + 30));
    g.drawLine((x + 74), (y + 36), (x + 86), (y + 30));
    g.drawLine((x + 74), (y + 24), (x + 74), (y + 36));
    g.drawLine((x + 88), (y + 30), (x + 70), (y + 30));
    g.drawLine((x + 70), (y + 30), (x + 70), (y + height - 7));
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + 30));
    g.drawLine((x + 130), (y + 30), (x + 114), (y + 30));
    g.drawLine((x + 114), (y + 24), (x + 126), (y + 30));
    g.drawLine((x + 114), (y + 36), (x + 126), (y + 30));
    g.drawLine((x + 114), (y + 24), (x + 114), (y + 36));
    g.drawLine((x + 128), (y + 30), (x + 110), (y + 30));
    g.drawLine((x + 110), (y + 30), (x + 110), (y + height - 7));
    g.drawLine((x + 150), (y + 7), (x + 150), (y + 30));
    g.drawLine((x + 150), (y + 30), (x + 134), (y + 30));
    g.drawLine((x + 134), (y + 24), (x + 146), (y + 30));
    g.drawLine((x + 134), (y + 36), (x + 146), (y + 30));
    g.drawLine((x + 134), (y + 24), (x + 134), (y + 36));
    g.drawLine((x + 148), (y + 30), (x + 130), (y + 30));
    g.drawLine((x + 130), (y + 30), (x + 130), (y + 7));
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 30));
    g.drawLine((x + 110), (y + 30), (x + 94), (y + 30));
    g.drawLine((x + 94), (y + 24), (x + 106), (y + 30));
    g.drawLine((x + 94), (y + 36), (x + 106), (y + 30));
    g.drawLine((x + 94), (y + 24), (x + 94), (y + 36));
    g.drawLine((x + 108), (y + 30), (x + 90), (y + 30));
    g.drawLine((x + 90), (y + 30), (x + 90), (y + 7));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 30));
    g.drawLine((x + 50), (y + 30), (x + 34), (y + 30));
    g.drawLine((x + 34), (y + 24), (x + 46), (y + 30));
    g.drawLine((x + 34), (y + 36), (x + 46), (y + 30));
    g.drawLine((x + 34), (y + 24), (x + 34), (y + 36));
    g.drawLine((x + 48), (y + 30), (x + 30), (y + 30));
    g.drawLine((x + 30), (y + 30), (x + 30), (y + 7));
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
