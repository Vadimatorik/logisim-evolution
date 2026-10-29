/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

/**
 * TTL 74x290: decade ripple counter.
 *
 * <p>Simulation follows the Philips/NXP 74HC/HCT290 product specification. {@code CP0} toggles
 * {@code Q0} on a high-to-low edge. {@code CP1} advances {@code Q1}, {@code Q2} and {@code Q3}
 * through 0, 1, 2, 3 and 4 on a high-to-low edge. {@code Q1} is the least significant bit of that
 * section. Connecting {@code Q0} to {@code CP1} counts BCD 0 through 9. Connecting {@code Q3} to
 * {@code CP0} produces the bi-quinary sequence. Both {@code MS} inputs high asynchronously force
 * {@code Q3 Q2 Q1 Q0} to 1001 and override the clocks and the reset. Otherwise both {@code MR}
 * inputs high clear every output. A single input of either pair does nothing. Codes 5, 6 and 7 of
 * the divide-by-five section are outside the published table; the next {@code CP1} edge returns
 * that section to 0. VCC is pin 5 and GND is pin 10. The corner-power SN74LS290, with VCC on pin
 * 14 and GND on pin 7, is a different package and is not this model. Ripple delay is not modeled.
 */
public class Ttl74290 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74290";

  public static final int PORT_INDEX_CP1 = 0;
  public static final int PORT_INDEX_MR1 = 1;
  public static final int PORT_INDEX_MR2 = 2;
  public static final int PORT_INDEX_MS1 = 3;
  public static final int PORT_INDEX_MS2 = 4;
  public static final int PORT_INDEX_Q2 = 5;
  public static final int PORT_INDEX_Q1 = 6;
  public static final int PORT_INDEX_Q3 = 7;
  public static final int PORT_INDEX_Q0 = 8;
  public static final int PORT_INDEX_CP0 = 9;

  private static final int BIT_Q0 = 0;
  private static final int BIT_Q1 = 1;
  private static final int BIT_Q2 = 2;
  private static final int BIT_Q3 = 3;
  private static final int BITS = 4;
  private static final int SET_TO_NINE = 9;
  private static final int DIVIDE_BY_FIVE_TERMINAL = 4;
  private static final int DELAY = 4;
  private static final int CLOCK_0 = 0;
  private static final int CLOCK_1 = 1;
  private static final BitWidth WIDTH = BitWidth.create(BITS);
  private static final byte[] OUTPUT_PORTS = {8, 9, 11, 12};
  private static final byte[] UNUSED_PINS = {4, 13};
  private static final String[] PORT_NAMES = {
    "CP1 (Clock, divide-by-5)",
    "MR1 (Master reset, active high)",
    "MR2 (Master reset, active high)",
    "MS1 (Master set to 9, active high)",
    "MS2 (Master set to 9, active high)",
    "Q2",
    "Q1",
    "Q3",
    "Q0",
    "CP0 (Clock, divide-by-2)"
  };

  /** Creates a 74290 decade ripple counter. */
  public Ttl74290() {
    super(
        _ID,
        (byte) 14,
        OUTPUT_PORTS,
        UNUSED_PINS,
        PORT_NAMES,
        (byte) 5,
        (byte) 10,
        new Ttl74290HdlGenerator());
  }

  private static final int PINS = 14;
  private static final int BUBBLE_SIZE = 4;

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored =
        rawState instanceof TtlRegisterData register ? register : null;
    // Divide-by-2: CP0 (14) clocks Q0 (12). Divide-by-5: CP1 (1) clocks Q1, Q2, Q3 (9, 8, 11).
    drawSingleStage(
        g, x, y, height, true, 12, 14, BIT_Q0, y + 14, stored);
    drawRippleChain(
        g,
        x,
        y,
        height,
        true,
        new int[] {9, 8, 11},
        new int[] {BIT_Q1, BIT_Q2, BIT_Q3},
        1,
        y + 30,
        stored);
    drawMasterReset(g, x, y, height, y + 14, y + 30);
    drawMasterSet(g, x, y, height, y + 14, y + 30);
  }

  /** One flip-flop row. {@code outputPin} and {@code clockPin} use package pin numbers. */
  private static void drawSingleStage(
      Graphics g,
      int x,
      int y,
      int height,
      boolean alongTop,
      int outputPin,
      int clockPin,
      int bit,
      int rowTop,
      TtlRegisterData stored) {
    final var boxWidth = 10;
    final var boxHeight = 10;
    final var center = structureX(x, PINS, outputPin);
    final var left = center - boxWidth / 2;
    final var right = left + boxWidth;
    final var mid = rowTop + boxHeight / 2;
    final var pinY = structureY(y, height, PINS, outputPin);
    final var towardPins = alongTop ? pinY + 2 : pinY - 2;
    g.drawRect(left, rowTop, boxWidth, boxHeight);
    drawClockMark(g, left, mid);
    drawBit(g, center, mid, stored, 0, bit);
    final var nodeX = right + 3;
    g.drawLine(right, mid, nodeX, mid);
    g.drawLine(nodeX, mid, nodeX, towardPins);
    g.drawLine(nodeX, towardPins, center, towardPins);
    g.drawLine(center, towardPins, center, pinY);

    final var clockX = structureX(x, PINS, clockPin);
    final var clockPinY = structureY(y, height, PINS, clockPin);
    g.drawLine(clockX, clockPinY, clockX, mid);
    g.drawLine(clockX, mid, left - BUBBLE_SIZE, mid);
    drawClockBubble(g, left, mid);
  }

  /** Ripple chain with outputs on the top or bottom edge and clock on the opposite edge. */
  private static void drawRippleChain(
      Graphics g,
      int x,
      int y,
      int height,
      boolean outputsAlongTop,
      int[] outputPins,
      int[] bits,
      int clockPin,
      int rowTop,
      TtlRegisterData stored) {
    final var boxWidth = 10;
    final var boxHeight = 10;
    final var mid = rowTop + boxHeight / 2;
    final var towardPins = outputsAlongTop ? y + PIN_HEIGHT + 2 : y + height - PIN_HEIGHT - 2;
    for (var stage = 0; stage < outputPins.length; stage++) {
      final var center = structureX(x, PINS, outputPins[stage]);
      final var left = center - boxWidth / 2;
      final var right = left + boxWidth;
      final var pinY = structureY(y, height, PINS, outputPins[stage]);
      g.drawRect(left, rowTop, boxWidth, boxHeight);
      drawClockMark(g, left, mid);
      drawBit(g, center, mid, stored, 0, bits[stage]);
      final var nodeX = right + 3;
      g.drawLine(right, mid, nodeX, mid);
      g.drawLine(nodeX, mid, nodeX, towardPins);
      g.drawLine(nodeX, towardPins, center, towardPins);
      g.drawLine(center, towardPins, center, pinY);
      if (stage + 1 < outputPins.length) {
        final var nextLeft = structureX(x, PINS, outputPins[stage + 1]) - boxWidth / 2;
        g.drawLine(nodeX, mid, nextLeft - BUBBLE_SIZE, mid);
        drawClockBubble(g, nextLeft, mid);
        drawNode(g, nodeX, mid);
      }
    }
    final var firstLeft = structureX(x, PINS, outputPins[0]) - boxWidth / 2;
    final var clockX = structureX(x, PINS, clockPin);
    final var clockPinY = structureY(y, height, PINS, clockPin);
    g.drawLine(clockX, clockPinY, clockX, mid);
    g.drawLine(clockX, mid, firstLeft - BUBBLE_SIZE, mid);
    drawClockBubble(g, firstLeft, mid);
  }

  /** MR1 and MR2 must both be high. The reset rail reaches every stage. */
  private static void drawMasterReset(
      Graphics g, int x, int y, int height, int rowTopA, int rowTopB) {
    final var mr1 = structureX(x, PINS, 2);
    final var mr2 = structureX(x, PINS, 3);
    final var bottomPinY = y + height - PIN_HEIGHT;
    final var gateMid = y + height - 22;
    g.drawLine(mr1, bottomPinY, mr1, gateMid);
    g.drawLine(mr2, bottomPinY, mr2, gateMid);
    g.drawRect(mr1, gateMid - 4, mr2 - mr1, 8);
    final var mergeX = (mr1 + mr2) / 2;
    final var railA = rowTopA - 3;
    final var railB = rowTopB - 3;
    g.drawLine(mergeX, gateMid - 4, mergeX, railA);
    g.drawLine(mergeX, railA, structureX(x, PINS, 12), railA);
    g.drawLine(structureX(x, PINS, 12), railA, structureX(x, PINS, 12), rowTopA);
    g.drawLine(mergeX, railA, mergeX, railB);
    for (final var outputPin : new int[] {9, 8, 11}) {
      final var px = structureX(x, PINS, outputPin);
      g.drawLine(mergeX, railB, px, railB);
      g.drawLine(px, railB, px, rowTopB);
      drawNode(g, px, railB);
    }
    drawNode(g, mergeX, railA);
    drawNode(g, (mr1 + mr2) / 2, gateMid - 4);
  }

  /** MS1 and MS2 force 1001 when both are high. */
  private static void drawMasterSet(
      Graphics g, int x, int y, int height, int rowTopA, int rowTopB) {
    final var ms1 = structureX(x, PINS, 6);
    final var ms2 = structureX(x, PINS, 7);
    final var bottomPinY = y + height - PIN_HEIGHT;
    final var gateMid = y + height - 22;
    g.drawLine(ms1, bottomPinY, ms1, gateMid);
    g.drawLine(ms2, bottomPinY, ms2, gateMid);
    g.drawRect(ms1, gateMid - 4, ms2 - ms1, 8);
    final var labelY = gateMid - 10;
    GraphicsUtil.drawCenteredText(g, "9", ms1, labelY);
    GraphicsUtil.drawCenteredText(g, "9", ms2, labelY);
    final var mergeX = (ms1 + ms2) / 2;
    final var presetA = rowTopA + 12;
    final var presetB = rowTopB + 12;
    g.drawLine(mergeX, gateMid + 4, mergeX, presetB);
    g.drawLine(mergeX, presetB, structureX(x, PINS, 11), presetB);
    g.drawLine(structureX(x, PINS, 11), presetB, structureX(x, PINS, 11), rowTopB + 10);
    g.drawLine(mergeX, presetB, mergeX, presetA);
    g.drawLine(mergeX, presetA, structureX(x, PINS, 12), presetA);
    g.drawLine(structureX(x, PINS, 12), presetA, structureX(x, PINS, 12), rowTopA + 10);
    drawNode(g, mergeX, gateMid + 4);
  }

  private static void drawClockMark(Graphics g, int left, int mid) {
    g.drawLine(left, mid - 2, left + 3, mid);
    g.drawLine(left, mid + 2, left + 3, mid);
  }

  private static void drawClockBubble(Graphics g, int inputX, int mid) {
    g.drawOval(inputX - BUBBLE_SIZE, mid - BUBBLE_SIZE / 2, BUBBLE_SIZE, BUBBLE_SIZE);
  }

  private static void drawNode(Graphics g, int nodeX, int nodeY) {
    g.fillOval(nodeX - 2, nodeY - 2, 4, 4);
  }

  private static void drawBit(
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
    g.fillOval(centerX - 2, centerY - 2, 4, 4);
    g.setColor(Color.BLACK);
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var clock0 =
        data.updateClock(state.getPortValue(PORT_INDEX_CP0), CLOCK_0, StdAttr.TRIG_FALLING);
    final var clock1 =
        data.updateClock(state.getPortValue(PORT_INDEX_CP1), CLOCK_1, StdAttr.TRIG_FALLING);
    var value = data.getValue();
    if (bothHigh(state, PORT_INDEX_MS1, PORT_INDEX_MS2)) {
      value = Value.createKnown(WIDTH, SET_TO_NINE);
    } else if (bothHigh(state, PORT_INDEX_MR1, PORT_INDEX_MR2)) {
      value = Value.createKnown(WIDTH, 0);
    } else {
      if (clock0) value = toggleQ0(value);
      if (clock1) value = advanceDivideByFive(value);
    }
    data.setValue(value);
    state.setPort(PORT_INDEX_Q0, value.get(BIT_Q0), DELAY);
    state.setPort(PORT_INDEX_Q1, value.get(BIT_Q1), DELAY);
    state.setPort(PORT_INDEX_Q2, value.get(BIT_Q2), DELAY);
    state.setPort(PORT_INDEX_Q3, value.get(BIT_Q3), DELAY);
  }

  private static boolean bothHigh(InstanceState state, int firstPort, int secondPort) {
    return state.getPortValue(firstPort) == Value.TRUE
        && state.getPortValue(secondPort) == Value.TRUE;
  }

  private static Value toggleQ0(Value value) {
    return value.set(BIT_Q0, toggled(value.get(BIT_Q0)));
  }

  private static Value toggled(Value bit) {
    if (bit == Value.TRUE) return Value.FALSE;
    if (bit == Value.FALSE) return Value.TRUE;
    if (bit == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  /**
   * Advances {@code Q1}, {@code Q2} and {@code Q3}. An error in the section makes the next section
   * entirely error. A section that is not fully defined becomes unknown. A defined code below 4
   * increments, and every other defined code returns to 0.
   */
  private static Value advanceDivideByFive(Value value) {
    if (sectionContains(value, Value.ERROR)) return setDivideByFive(value, Value.ERROR);
    if (!divideByFiveKnown(value)) return setDivideByFive(value, Value.UNKNOWN);
    final var code = divideByFiveCode(value);
    final var next = code >= DIVIDE_BY_FIVE_TERMINAL ? 0 : code + 1;
    return value
        .set(BIT_Q1, bit(next, 0))
        .set(BIT_Q2, bit(next, 1))
        .set(BIT_Q3, bit(next, 2));
  }

  private static boolean sectionContains(Value value, Value bit) {
    return value.get(BIT_Q1) == bit || value.get(BIT_Q2) == bit || value.get(BIT_Q3) == bit;
  }

  private static boolean divideByFiveKnown(Value value) {
    return isKnown(value.get(BIT_Q1)) && isKnown(value.get(BIT_Q2)) && isKnown(value.get(BIT_Q3));
  }

  private static boolean isKnown(Value bit) {
    return bit == Value.TRUE || bit == Value.FALSE;
  }

  private static int divideByFiveCode(Value value) {
    return (value.get(BIT_Q3) == Value.TRUE ? 4 : 0)
        + (value.get(BIT_Q2) == Value.TRUE ? 2 : 0)
        + (value.get(BIT_Q1) == Value.TRUE ? 1 : 0);
  }

  private static Value setDivideByFive(Value value, Value bit) {
    return value.set(BIT_Q1, bit).set(BIT_Q2, bit).set(BIT_Q3, bit);
  }

  private static Value bit(int code, int shift) {
    return ((code >> shift) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CP1, PORT_INDEX_CP0};
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

}
