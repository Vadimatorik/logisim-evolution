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
 * TTL 74x122: retriggerable monostable multivibrator with clear.
 *
 * <p>Model based on the ON Semiconductor
 * <a href="https://www.onsemi.com/pdf/datasheet/sn74ls122-d.pdf">SN74LS122 datasheet</a>. A
 * separate 74HC122 datasheet is not published by Nexperia or TI; the HC and LS devices share this
 * pinout and function table. External timing pins are not logic nets: the simulator has no analog
 * RC model. Pulse width uses the 5 V formula {@code tW = 0.45 × Rext(kΩ) × Cext(pF)} nanoseconds,
 * for Cext at least 1000 pF, and is counted in simulator ticks: {@code max(1, round(tW ×
 * tickFrequency))}. The 5 V point Rext = 10 kΩ, Cext = 100 nF is 450 µs. Connecting the internal
 * timing resistor is the same as Rext = 10. The width is stored on the instance when the pulse
 * starts. A short pulse at a low tick rate therefore lasts one tick. There is no HDL model.
 */
public class Ttl74122 extends AbstractTtlGate implements TickAware {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74122";

  public static final int PORT_INDEX_A1 = 0;
  public static final int PORT_INDEX_A2 = 1;
  public static final int PORT_INDEX_B1 = 2;
  public static final int PORT_INDEX_B2 = 3;
  public static final int PORT_INDEX_CLR = 4;
  public static final int PORT_INDEX_QBAR = 5;
  public static final int PORT_INDEX_Q = 6;

  /** Timing factor from the datasheet: tW(ns) = K × Rext(kΩ) × Cext(pF), K = 0.45 at 5 V. */
  public static final double PULSE_WIDTH_FACTOR_SECONDS = 0.45e-9;

  /** External resistor in kiloohms. Datasheet range at 5 V is 5 to 260. */
  public static final Attribute<Integer> REXT =
      Attributes.forIntegerRange("Rext", S.getter("ttl74122Rext"), 5, 260);
  /** External capacitor in picofarads. Linear formula applies from 1000 pF. */
  public static final Attribute<Integer> CEXT =
      Attributes.forIntegerRange("Cext", S.getter("ttl74122Cext"), 1_000, 1_000_000_000);

  private static final int DELAY = 4;
  private static final int DEFAULT_REXT_KOHM = 10;
  private static final int DEFAULT_CEXT_PF = 100_000;
  private static final byte[] OUTPUT_PORTS = {6, 8};
  private static final byte[] UNUSED_PINS = {9, 10, 11, 12, 13};
  private static final String[] PORT_NAMES = {
    "A1 (negative-edge trigger)",
    "A2 (negative-edge trigger)",
    "B1 (positive-edge trigger)",
    "B2 (positive-edge trigger)",
    "CLR (clear, active low)",
    "Q\\ (active low)",
    "Q"
  };

  /** Creates a 74122 retriggerable monostable. */
  public Ttl74122() {
    super(_ID, (byte) 14, OUTPUT_PORTS, UNUSED_PINS, PORT_NAMES, null);
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          TtlLibrary.VCC_GND,
          TtlLibrary.DRAW_INTERNAL_STRUCTURE,
          StdAttr.LABEL,
          REXT,
          CEXT
        },
        new Object[] {
          Direction.EAST, false, false, "", DEFAULT_REXT_KOHM, DEFAULT_CEXT_PF
        });
  }

  /**
   * Converts an external RC pair into a pulse length in simulator ticks.
   *
   * @param rextKOhm external resistor in kiloohms
   * @param cextPf external capacitor in picofarads
   * @param tickFrequencyHz simulator tick frequency in hertz
   * @return pulse length in ticks, at least one
   */
  public static int widthTicks(int rextKOhm, int cextPf, double tickFrequencyHz) {
    if (tickFrequencyHz <= 0) {
      return 1;
    }
    final var ticks =
        Math.round(PULSE_WIDTH_FACTOR_SECONDS * rextKOhm * cextPf * tickFrequencyHz);
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
    final var data = (MonostableData) state.getData();
    return data != null && data.expire(ticks);
  }

  @Override
  public boolean tick(CircuitState state, int ticks, Component comp) {
    final var data = (MonostableData) state.getData(comp);
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
          "A1", "A2", "B1", "B2", "CLR", "nQ", null, "Q", "Ri", null, "Cx", null, "RC", null
        });
    drawTiming(gfx, painter, x, y, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var inputA1 = state.getPortValue(PORT_INDEX_A1);
    final var inputA2 = state.getPortValue(PORT_INDEX_A2);
    final var inputB1 = state.getPortValue(PORT_INDEX_B1);
    final var inputB2 = state.getPortValue(PORT_INDEX_B2);
    final var clear = state.getPortValue(PORT_INDEX_CLR);
    if (clear == Value.FALSE) {
      data.active = false;
    } else if (clear == Value.TRUE && triggered(data, inputA1, inputA2, inputB1, inputB2, clear)) {
      data.active = true;
      data.startedAt = state.getTickCount();
      data.widthTicks =
          widthTicks(
              state.getAttributeValue(REXT),
              state.getAttributeValue(CEXT),
              tickFrequencyHz(state));
    }
    data.inputA1 = inputA1;
    data.inputA2 = inputA2;
    data.inputB1 = inputB1;
    data.inputB2 = inputB2;
    data.clear = clear;
    state.setPort(PORT_INDEX_Q, data.active ? Value.TRUE : Value.FALSE, DELAY);
    state.setPort(PORT_INDEX_QBAR, data.active ? Value.FALSE : Value.TRUE, DELAY);
  }

  private void drawTiming(Graphics2D gfx, InstancePainter painter, int x, int y, int height) {
    final var data = (MonostableData) painter.getData();
    final var ticks =
        widthTicks(
            painter.getAttributeValue(REXT),
            painter.getAttributeValue(CEXT),
            tickFrequencyHz(painter));
    final var active = data != null && data.active;
    gfx.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 9));
    gfx.setColor(active ? Value.TRUE.getColor() : Color.BLACK);
    GraphicsUtil.drawCenteredText(gfx, ticks + "t", x + 70, y + height / 2 + 8);
    gfx.setColor(Color.BLACK);
  }

  /**
   * Datasheet edges, and only a fully defined 0/1 transition counts. Rising B1 or B2 while the
   * other B is high and at least one A is low. Falling A only when both A inputs were high and
   * both B inputs are high, including both A inputs falling together. Rising clear while at least
   * one A is low and both B inputs are high. An undefined clear neither clears nor triggers, so a
   * pulse already in progress continues.
   */
  private static boolean triggered(
      MonostableData data,
      Value inputA1,
      Value inputA2,
      Value inputB1,
      Value inputB2,
      Value clear) {
    final var anALow = inputA1 == Value.FALSE || inputA2 == Value.FALSE;
    final var bothBHigh = inputB1 == Value.TRUE && inputB2 == Value.TRUE;
    final var risingB =
        anALow
            && ((data.inputB1 == Value.FALSE && inputB1 == Value.TRUE && inputB2 == Value.TRUE)
                || (data.inputB2 == Value.FALSE && inputB2 == Value.TRUE && inputB1 == Value.TRUE));
    final var fallingA =
        bothBHigh
            && data.inputA1 == Value.TRUE
            && data.inputA2 == Value.TRUE
            && (inputA1 == Value.FALSE || inputA2 == Value.FALSE);
    final var risingClear =
        data.clear == Value.FALSE && clear == Value.TRUE && anALow && bothBHigh;
    return risingB || fallingA || risingClear;
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

  private static MonostableData getStateData(InstanceState state) {
    var data = (MonostableData) state.getData();
    if (data == null) {
      data = new MonostableData();
      state.setData(data);
    }
    return data;
  }

  private static final class MonostableData implements InstanceData {
    private Value inputA1 = Value.UNKNOWN;
    private Value inputA2 = Value.UNKNOWN;
    private Value inputB1 = Value.UNKNOWN;
    private Value inputB2 = Value.UNKNOWN;
    private Value clear = Value.UNKNOWN;
    private boolean active;
    private int startedAt;
    private int widthTicks = 1;

    @Override
    public MonostableData clone() {
      final var copy = new MonostableData();
      copy.inputA1 = inputA1;
      copy.inputA2 = inputA2;
      copy.inputB1 = inputB1;
      copy.inputB2 = inputB2;
      copy.clear = clear;
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
      if (elapsed >= widthTicks) {
        active = false;
        return true;
      }
      return false;
    }
  }
}
