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
 * TTL 74x4538: dual retriggerable precision monostable multivibrator.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC4538.pdf">74HC4538 datasheet</a>.
 * Each half follows that function table. A falling {@code nA} starts a pulse only while {@code nB}
 * is low, and a rising {@code nB} starts a pulse only while {@code nA} is high. A low {@code nCD}
 * clears the pulse immediately and releasing it does not start another. External timing pins are
 * not logic nets: the simulator has no analog RC model. Pulse width uses {@code tW = 0.7 ×
 * Rext(kΩ) × Cext(pF)} nanoseconds, the value the datasheet gives for Cext above 100 nF, and is
 * counted in simulator ticks: {@code max(1, round(tW × tickFrequency))}. The width is stored on
 * the instance when the pulse starts. A short pulse at a low tick rate therefore lasts one tick.
 * There is no HDL model.
 */
public class Ttl744538 extends AbstractTtlGate implements TickAware {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744538";

  public static final int PORT_INDEX_1CD = 0;
  public static final int PORT_INDEX_1B = 1;
  public static final int PORT_INDEX_1A = 2;
  public static final int PORT_INDEX_1Q = 3;
  public static final int PORT_INDEX_1QBAR = 4;
  public static final int PORT_INDEX_2QBAR = 5;
  public static final int PORT_INDEX_2Q = 6;
  public static final int PORT_INDEX_2A = 7;
  public static final int PORT_INDEX_2B = 8;
  public static final int PORT_INDEX_2CD = 9;

  /** Timing factor from the datasheet: tW(ns) = K × Rext(kΩ) × Cext(pF), K = 0.7. */
  public static final double PULSE_WIDTH_FACTOR_SECONDS = 0.7e-9;

  /** External resistor for section 1, in kiloohms. Datasheet range at 5 V is 2 to 1000. */
  public static final Attribute<Integer> REXT_1 =
      Attributes.forIntegerRange("1Rext", S.getter("ttl744538Rext1"), 2, 1000);
  /** External capacitor for section 1, in picofarads. The linear formula applies above 100 nF. */
  public static final Attribute<Integer> CEXT_1 =
      Attributes.forIntegerRange("1Cext", S.getter("ttl744538Cext1"), 100_000, 1_000_000_000);
  /** External resistor for section 2, in kiloohms. Datasheet range at 5 V is 2 to 1000. */
  public static final Attribute<Integer> REXT_2 =
      Attributes.forIntegerRange("2Rext", S.getter("ttl744538Rext2"), 2, 1000);
  /** External capacitor for section 2, in picofarads. The linear formula applies above 100 nF. */
  public static final Attribute<Integer> CEXT_2 =
      Attributes.forIntegerRange("2Cext", S.getter("ttl744538Cext2"), 100_000, 1_000_000_000);

  private static final int DELAY = 4;
  private static final int HALVES = 2;
  private static final int DEFAULT_REXT_KOHM = 10;
  private static final int DEFAULT_CEXT_PF = 100_000;
  private static final byte[] OUTPUT_PORTS = {6, 7, 9, 10};
  private static final byte[] UNUSED_PINS = {1, 2, 14, 15};
  private static final String[] PORT_NAMES = {
    "1CD (direct reset, active low)",
    "1B (positive-edge trigger)",
    "1A (negative-edge trigger)",
    "1Q",
    "1Q\\ (active low)",
    "2Q\\ (active low)",
    "2Q",
    "2A (negative-edge trigger)",
    "2B (positive-edge trigger)",
    "2CD (direct reset, active low)"
  };
  private static final int[] A_PORTS = {PORT_INDEX_1A, PORT_INDEX_2A};
  private static final int[] B_PORTS = {PORT_INDEX_1B, PORT_INDEX_2B};
  private static final int[] CD_PORTS = {PORT_INDEX_1CD, PORT_INDEX_2CD};
  private static final int[] Q_PORTS = {PORT_INDEX_1Q, PORT_INDEX_2Q};
  private static final int[] QBAR_PORTS = {PORT_INDEX_1QBAR, PORT_INDEX_2QBAR};

  /** Creates a 744538 dual retriggerable precision monostable. */
  public Ttl744538() {
    super(_ID, (byte) 16, OUTPUT_PORTS, UNUSED_PINS, PORT_NAMES, null);
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          TtlLibrary.VCC_GND,
          TtlLibrary.DRAW_INTERNAL_STRUCTURE,
          StdAttr.LABEL,
          REXT_1,
          CEXT_1,
          REXT_2,
          CEXT_2
        },
        new Object[] {
          Direction.EAST,
          false,
          false,
          "",
          DEFAULT_REXT_KOHM,
          DEFAULT_CEXT_PF,
          DEFAULT_REXT_KOHM,
          DEFAULT_CEXT_PF
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
   * Ends any pulse whose captured width has elapsed. Returns true when an output must change.
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
          "1C", "1RC", "1CD", "1B", "1A", "1Q", "1nQ", null,
          "2nQ", "2Q", "2A", "2B", "2CD", "2RC", "2C", null
        });
    drawTiming(gfx, painter, x, y, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    for (var half = 0; half < HALVES; half++) {
      updateHalf(state, data, half);
    }
  }

  private void drawTiming(Graphics2D gfx, InstancePainter painter, int x, int y, int height) {
    final var data = (MonostableData) painter.getData();
    final var frequency = tickFrequencyHz(painter);
    gfx.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 9));
    for (var half = 0; half < HALVES; half++) {
      final var ticks =
          widthTicks(
              painter.getAttributeValue(rextAttribute(half)),
              painter.getAttributeValue(cextAttribute(half)),
              frequency);
      final var active = data != null && data.halves[half].active;
      gfx.setColor(active ? Value.TRUE.getColor() : Color.BLACK);
      GraphicsUtil.drawCenteredText(gfx, ticks + "t", x + 48 + half * 64, y + height / 2 + 8);
    }
    gfx.setColor(Color.BLACK);
  }

  private void updateHalf(InstanceState state, MonostableData data, int half) {
    final var section = data.halves[half];
    final var inputA = state.getPortValue(A_PORTS[half]);
    final var inputB = state.getPortValue(B_PORTS[half]);
    final var reset = state.getPortValue(CD_PORTS[half]);
    if (reset == Value.FALSE) {
      section.active = false;
    } else if (reset == Value.TRUE && triggered(section, inputA, inputB)) {
      section.active = true;
      section.startedAt = state.getTickCount();
      section.widthTicks =
          widthTicks(
              state.getAttributeValue(rextAttribute(half)),
              state.getAttributeValue(cextAttribute(half)),
              tickFrequencyHz(state));
    }
    section.inputA = inputA;
    section.inputB = inputB;
    state.setPort(Q_PORTS[half], section.active ? Value.TRUE : Value.FALSE, DELAY);
    state.setPort(QBAR_PORTS[half], section.active ? Value.FALSE : Value.TRUE, DELAY);
  }

  /**
   * Datasheet edges: falling A while B is low, or rising B while A is high. Only a fully defined
   * 0/1 transition counts. Releasing reset is not a trigger. An undefined reset neither clears nor
   * triggers, so a pulse already in progress continues.
   */
  private static boolean triggered(Half section, Value inputA, Value inputB) {
    final var fallingA =
        section.inputA == Value.TRUE && inputA == Value.FALSE && inputB == Value.FALSE;
    final var risingB =
        section.inputB == Value.FALSE && inputB == Value.TRUE && inputA == Value.TRUE;
    return fallingA || risingB;
  }

  private static Attribute<Integer> rextAttribute(int half) {
    return half == 0 ? REXT_1 : REXT_2;
  }

  private static Attribute<Integer> cextAttribute(int half) {
    return half == 0 ? CEXT_1 : CEXT_2;
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

  private static final class Half {
    private Value inputA = Value.UNKNOWN;
    private Value inputB = Value.UNKNOWN;
    private boolean active;
    private int startedAt;
    private int widthTicks = 1;

    private Half copy() {
      final var copy = new Half();
      copy.inputA = inputA;
      copy.inputB = inputB;
      copy.active = active;
      copy.startedAt = startedAt;
      copy.widthTicks = widthTicks;
      return copy;
    }
  }

  private static final class MonostableData implements InstanceData {
    private final Half[] halves = {new Half(), new Half()};

    @Override
    public MonostableData clone() {
      final var copy = new MonostableData();
      for (var half = 0; half < HALVES; half++) {
        copy.halves[half] = halves[half].copy();
      }
      return copy;
    }

    private boolean expire(int ticks) {
      var dirty = false;
      for (final var half : halves) {
        if (!half.active) {
          continue;
        }
        final var elapsed = (long) ticks - half.startedAt;
        if (elapsed >= half.widthTicks) {
          half.active = false;
          dirty = true;
        }
      }
      return dirty;
    }
  }
}
