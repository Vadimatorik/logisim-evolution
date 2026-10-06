/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.TickAware;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * TTL 74x121: non-retriggerable monostable multivibrator.
 *
 * <p>Model based on the Texas Instruments SN54121/SN74121 data sheet
 * (SDLS042, <a href="https://www.ti.com/lit/ds/symlink/sn74121.pdf">SN74121</a>). No separate
 * 74HC121 data sheet with a different timing constant was found, so a 74HC121 is simulated with
 * the same function table, pinout, and pulse-width formula. External timing pins are not logic
 * nets: the simulator has no analog RC model. For Cext above 1000 pF the width is {@code tW = 0.7
 * × R(kΩ) × C(pF)} nanoseconds, counted in simulator ticks as {@code max(1, round(tW ×
 * tickFrequency))}. The width is stored on the instance when the pulse starts. A short pulse at a
 * low tick rate therefore lasts one tick.
 *
 * <p>The device is not retriggerable and has no reset: a new edge during the pulse does not extend
 * it, and input levels cannot cut it short. Schmitt thresholds on B, the correction for a small
 * capacitor, and the roughly 30 ns pulse with no external capacitor are not modeled. The external
 * timing resistor is specified from 1.4 kΩ; the attribute is an integer number of kiloohms, so its
 * minimum is 2. The internal timing resistor is the typical 2 kΩ. There is no HDL model.
 */
public class Ttl74121 extends AbstractTtlGate implements TickAware {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74121";

  public static final int PORT_INDEX_QBAR = 0;
  public static final int PORT_INDEX_A1 = 1;
  public static final int PORT_INDEX_A2 = 2;
  public static final int PORT_INDEX_B = 3;
  public static final int PORT_INDEX_Q = 4;

  /** Timing factor from the data sheet: tW(ns) = 0.7 × R(kΩ) × C(pF) for Cext above 1000 pF. */
  public static final double PULSE_WIDTH_FACTOR_SECONDS = 0.7e-9;

  /** Typical internal timing resistor, in kiloohms, selected by tying pin 9 to VCC. */
  public static final int INTERNAL_REXT_KOHM = 2;

  /** External timing resistor, in kiloohms. The data-sheet range starts at 1.4. */
  public static final Attribute<Integer> REXT =
      Attributes.forIntegerRange("Rext", S.getter("ttl74121Rext"), 2, 40);
  /** External timing capacitor, in picofarads. The linear formula applies above 1000 pF. */
  public static final Attribute<Integer> CEXT =
      Attributes.forIntegerRange("Cext", S.getter("ttl74121Cext"), 1_000, 1_000_000_000);
  /** When true, the pulse uses the internal 2 kΩ resistor instead of {@link #REXT}. */
  public static final Attribute<Boolean> USE_RINT =
      Attributes.forBoolean("useRint", S.getter("ttl74121UseRint"));

  private static final int DELAY = 4;
  private static final int DEFAULT_REXT_KOHM = 10;
  private static final int DEFAULT_CEXT_PF = 100_000;
  private static final byte[] OUTPUT_PORTS = {1, 6};
  private static final byte[] UNUSED_PINS = {2, 8, 9, 10, 11, 12, 13};
  private static final String[] PORT_NAMES = {
    "Q\\ (active low)",
    "A1 (negative-edge trigger)",
    "A2 (negative-edge trigger)",
    "B (positive-edge trigger)",
    "Q"
  };

  /** Creates a 74121 non-retriggerable monostable. */
  public Ttl74121() {
    super(_ID, (byte) 14, OUTPUT_PORTS, UNUSED_PINS, PORT_NAMES, null);
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          TtlLibrary.VCC_GND,
          TtlLibrary.DRAW_INTERNAL_STRUCTURE,
          StdAttr.LABEL,
          REXT,
          CEXT,
          USE_RINT
        },
        new Object[] {
          Direction.EAST, false, false, "", DEFAULT_REXT_KOHM, DEFAULT_CEXT_PF, false
        });
  }

  /**
   * Converts an RC pair into a pulse length in simulator ticks.
   *
   * @param rextKOhm timing resistor in kiloohms
   * @param cextPf timing capacitor in picofarads
   * @param tickFrequencyHz simulator tick frequency in hertz
   * @return pulse length in ticks, at least one
   */
  public static int widthTicks(int rextKOhm, int cextPf, double tickFrequencyHz) {
    if (tickFrequencyHz <= 0) {
      return 1;
    }
    final var ticks = Math.round(PULSE_WIDTH_FACTOR_SECONDS * rextKOhm * cextPf * tickFrequencyHz);
    if (ticks < 1) {
      return 1;
    }
    if (ticks > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) ticks;
  }

  /**
   * Ends a pulse whose captured width has elapsed. Returns true when an output must change.
   * {@link #tick} performs the same update for a placed component.
   */
  boolean expire(InstanceState state, int ticks) {
    final var data = (PulseData) state.getData();
    return data != null && data.expire(ticks);
  }

  @Override
  public boolean tick(CircuitState state, int ticks, Component comp) {
    final var data = (PulseData) state.getData(comp);
    return data != null && data.expire(ticks);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "nQ", null, "A1", "A2", "B", "Q", null, null, "Rint", "C", "RC", null, null, null
        });
    drawTiming(gfx, painter, x, y, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    update(state, getStateData(state));
  }

  private void drawTiming(Graphics2D gfx, InstancePainter painter, int x, int y, int height) {
    final var data = (PulseData) painter.getData();
    final var ticks =
        widthTicks(
            timingResistor(painter), painter.getAttributeValue(CEXT), tickFrequencyHz(painter));
    final var active = data != null && data.active;
    gfx.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 9));
    gfx.setColor(active ? Value.TRUE.getColor() : Color.BLACK);
    GraphicsUtil.drawCenteredText(gfx, ticks + "t", x + 70, y + height / 2 + 8);
    gfx.setColor(Color.BLACK);
  }

  private void update(InstanceState state, PulseData data) {
    final var inputA1 = state.getPortValue(PORT_INDEX_A1);
    final var inputA2 = state.getPortValue(PORT_INDEX_A2);
    final var inputB = state.getPortValue(PORT_INDEX_B);
    if (!data.active && triggered(data, inputA1, inputA2, inputB)) {
      data.active = true;
      data.startedAt = state.getTickCount();
      data.widthTicks =
          widthTicks(timingResistor(state), state.getAttributeValue(CEXT), tickFrequencyHz(state));
    }
    data.inputA1 = inputA1;
    data.inputA2 = inputA2;
    data.inputB = inputB;
    state.setPort(PORT_INDEX_Q, data.active ? Value.TRUE : Value.FALSE, DELAY);
    state.setPort(PORT_INDEX_QBAR, data.active ? Value.FALSE : Value.TRUE, DELAY);
  }

  /**
   * Data-sheet edges, and only a fully defined 0/1 transition: falling A1 while A2 and B are high,
   * falling A2 while A1 and B are high, both A inputs falling while B is high, or rising B while
   * A1 or A2 is low. An undefined level neither starts nor ends a pulse.
   */
  private static boolean triggered(PulseData data, Value inputA1, Value inputA2, Value inputB) {
    final var fallingA1 =
        data.inputA1 == Value.TRUE
            && inputA1 == Value.FALSE
            && inputA2 == Value.TRUE
            && inputB == Value.TRUE;
    final var fallingA2 =
        data.inputA2 == Value.TRUE
            && inputA2 == Value.FALSE
            && inputA1 == Value.TRUE
            && inputB == Value.TRUE;
    final var fallingBoth =
        data.inputA1 == Value.TRUE
            && data.inputA2 == Value.TRUE
            && inputA1 == Value.FALSE
            && inputA2 == Value.FALSE
            && inputB == Value.TRUE;
    final var risingB =
        data.inputB == Value.FALSE
            && inputB == Value.TRUE
            && (inputA1 == Value.FALSE || inputA2 == Value.FALSE);
    return fallingA1 || fallingA2 || fallingBoth || risingB;
  }

  private static int timingResistor(InstanceState state) {
    if (Boolean.TRUE.equals(state.getAttributeValue(USE_RINT))) {
      return INTERNAL_REXT_KOHM;
    }
    return state.getAttributeValue(REXT);
  }

  private double tickFrequencyHz(InstanceState state) {
    final var project = projectOf(state);
    if (project != null && project.getSimulator() != null) {
      final var frequency = project.getSimulator().getTickFrequency();
      if (frequency > 0) {
        return frequency;
      }
    }
    return 1.0;
  }

  private static Project projectOf(InstanceState state) {
    if (state instanceof InstancePainter painter) {
      final var circuitState = painter.getCircuitState();
      return circuitState == null ? null : circuitState.getProject();
    }
    return state.getProject();
  }

  private static PulseData getStateData(InstanceState state) {
    var data = (PulseData) state.getData();
    if (data == null) {
      data = new PulseData();
      state.setData(data);
    }
    return data;
  }

  private static final class PulseData implements InstanceData {
    private Value inputA1 = Value.UNKNOWN;
    private Value inputA2 = Value.UNKNOWN;
    private Value inputB = Value.UNKNOWN;
    private boolean active;
    private int startedAt;
    private int widthTicks = 1;

    @Override
    public PulseData clone() {
      final var copy = new PulseData();
      copy.inputA1 = inputA1;
      copy.inputA2 = inputA2;
      copy.inputB = inputB;
      copy.active = active;
      copy.startedAt = startedAt;
      copy.widthTicks = widthTicks;
      return copy;
    }

    private boolean expire(int ticks) {
      if (!active) {
        return false;
      }
      final var elapsed = (long) ticks - startedAt;
      if (elapsed < widthTicks) {
        return false;
      }
      active = false;
      return true;
    }
  }
}
