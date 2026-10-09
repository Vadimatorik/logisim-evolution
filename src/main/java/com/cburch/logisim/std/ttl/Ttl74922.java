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

/**
 * TTL 74922: 16-key matrix encoder.
 *
 * <p>Model based on the Fairchild MM74C922 datasheet (DS006037). Parts marked 74C922 and 74HC922
 * use this 18-pin function. The oscillator and key-bounce pins are capacitors, not logic nets, so
 * the external-clock mode is not modeled and there is no HDL model.
 *
 * <p>Column step rate follows the typical FSCAN curve: {@code f = 6e7 / Cosc(pF)} hertz (10 nF is
 * about 6 kHz, 100 nF about 600 Hz, 1 µF about 60 Hz). Press debounce is {@code T1 = R·C} with
 * {@code R = 10 kΩ}. The release mask is {@code T3 = 0.7·R·C}. Both are counted in simulator
 * ticks, and a shorter interval lasts one tick. {@code Ckbm = 0} removes the capacitor and
 * defeats debounce. The datasheet interval T2 is the same typical size as T1 and is not a separate
 * timer. A floating row reads as not pressed. When several rows are low together, the lowest row
 * number wins; the datasheet does not define that case.
 */
public class Ttl74922 extends AbstractTtlGate implements TickAware {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74922";

  public static final byte Y1 = 1;
  public static final byte Y2 = 2;
  public static final byte Y3 = 3;
  public static final byte Y4 = 4;
  public static final byte X4 = 7;
  public static final byte X3 = 8;
  public static final byte X2 = 10;
  public static final byte X1 = 11;
  public static final byte DAV = 12;
  public static final byte OE = 13;
  public static final byte DATA_D = 14;
  public static final byte DATA_C = 15;
  public static final byte DATA_B = 16;
  public static final byte DATA_A = 17;
  public static final byte GND = 9;
  public static final byte VCC = 18;

  /** {@code f(Hz) = SCAN_FREQUENCY_FACTOR / Cosc(pF)}, fitted to the typical FSCAN curve. */
  public static final double SCAN_FREQUENCY_FACTOR = 6.0e7;

  /** {@code T1(s) = DEBOUNCE_SECONDS_PER_PF × Ckbm(pF)} for the internal 10 kΩ mask resistor. */
  public static final double DEBOUNCE_SECONDS_PER_PF = 1.0e-8;

  /** Release mask T3 is 0.7 of the press-debounce interval. */
  public static final double RELEASE_MASK_FACTOR = 0.7;

  /** Oscillator capacitor, in picofarads. */
  public static final Attribute<Integer> COSC =
      Attributes.forIntegerRange("Cosc", S.getter("ttl74922Cosc"), 1_000, 10_000_000);

  /** Key-bounce mask capacitor, in picofarads. Zero omits the capacitor. */
  public static final Attribute<Integer> CKBM =
      Attributes.forIntegerRange("Ckbm", S.getter("ttl74922Ckbm"), 0, 1_000_000_000);

  private static final int DELAY = 1;
  private static final int ROWS = 4;
  private static final int DEFAULT_COSC_PF = 100_000;
  private static final int DEFAULT_CKBM_PF = 1_000_000;
  private static final byte OSC = 5;
  private static final byte KBM = 6;
  private static final byte[] ROW_PINS = {Y1, Y2, Y3, Y4};
  private static final byte[] COLUMN_PINS = {X1, X2, X3, X4};
  private static final byte[] DATA_PINS = {DATA_A, DATA_B, DATA_C, DATA_D};
  private static final byte[] OUTPUT_PORTS = {X4, X3, X2, X1, DAV, DATA_D, DATA_C, DATA_B, DATA_A};
  private static final byte[] UNUSED_PINS = {OSC, KBM};
  private static final String[] PORT_NAMES = {
    "Y1 (row 1, internal pull-up)",
    "Y2 (row 2, internal pull-up)",
    "Y3 (row 3, internal pull-up)",
    "Y4 (row 4, internal pull-up)",
    "X4 (column 4, open drain)",
    "X3 (column 3, open drain)",
    "X2 (column 2, open drain)",
    "X1 (column 1, open drain)",
    "DAV (data available)",
    "nOE (output enable, active low)",
    "D (latched code, MSB)",
    "C (latched code)",
    "B (latched code)",
    "A (latched code, LSB)"
  };

  /** Creates a 74922 16-key encoder. */
  public Ttl74922() {
    super(_ID, (byte) 18, OUTPUT_PORTS, UNUSED_PINS, PORT_NAMES, null);
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          TtlLibrary.VCC_GND,
          TtlLibrary.DRAW_INTERNAL_STRUCTURE,
          StdAttr.LABEL,
          COSC,
          CKBM
        },
        new Object[] {Direction.EAST, false, false, "", DEFAULT_COSC_PF, DEFAULT_CKBM_PF});
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Unused timing pins and power pins are omitted. Exposed power pins are appended after the
   * signal ports.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    byte port = 0;
    for (byte pin = 1; pin < dsPinNr; pin++) {
      if (pin == OSC || pin == KBM || pin == GND || pin == VCC) {
        continue;
      }
      port++;
    }
    return port;
  }

  /**
   * Column dwell time in simulator ticks.
   *
   * @param coscPf oscillator capacitor in picofarads
   * @param tickFrequencyHz simulator tick frequency in hertz
   * @return dwell time in ticks, at least one
   */
  public static int columnStepTicks(int coscPf, double tickFrequencyHz) {
    if (coscPf <= 0 || tickFrequencyHz <= 0) {
      return 1;
    }
    final var frequency = SCAN_FREQUENCY_FACTOR / coscPf;
    return clampTicks(Math.round(tickFrequencyHz / frequency));
  }

  /**
   * Press-debounce time T1 in simulator ticks. Zero means the mask capacitor is omitted.
   *
   * @param ckbmPf key-bounce capacitor in picofarads
   * @param tickFrequencyHz simulator tick frequency in hertz
   * @return debounce time in ticks, or zero when debounce is defeated
   */
  public static int pressDebounceTicks(int ckbmPf, double tickFrequencyHz) {
    if (ckbmPf <= 0) {
      return 0;
    }
    if (tickFrequencyHz <= 0) {
      return 1;
    }
    return clampTicks(Math.round(DEBOUNCE_SECONDS_PER_PF * ckbmPf * tickFrequencyHz));
  }

  /**
   * Post-release mask T3 in simulator ticks. Zero means the mask capacitor is omitted.
   *
   * @param ckbmPf key-bounce capacitor in picofarads
   * @param tickFrequencyHz simulator tick frequency in hertz
   * @return mask time in ticks, or zero when debounce is defeated
   */
  public static int releaseMaskTicks(int ckbmPf, double tickFrequencyHz) {
    if (ckbmPf <= 0) {
      return 0;
    }
    if (tickFrequencyHz <= 0) {
      return 1;
    }
    return clampTicks(
        Math.round(RELEASE_MASK_FACTOR * DEBOUNCE_SECONDS_PER_PF * ckbmPf * tickFrequencyHz));
  }

  @Override
  public boolean tick(CircuitState state, int ticks, Component comp) {
    final var data = (EncoderData) state.getData(comp);
    if (data == null) {
      return false;
    }
    final var hz = tickFrequencyHz(state.getProject());
    final var period = columnStepTicks(comp.getAttributeSet().getValue(COSC), hz);
    final var timersChanged = data.expireTimers(ticks);
    return timersChanged || data.columnStepDue(ticks, period);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "Y1", "Y2", "Y3", "Y4", "OSC", "KBM", "X4", "X3", null,
          "X2", "X1", "DAV", "nOE", "D", "C", "B", "A", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var now = state.getTickCount();
    if (data.phase == Phase.PRESS_DEBOUNCE && !isRowLow(state, data.candidateRow)) {
      data.phase = Phase.SCANNING;
      data.columnStarted = now;
    }
    if (data.phase == Phase.HELD && !isRowLow(state, data.candidateRow)) {
      data.dav = false;
      final var mask = data.maskTicks;
      if (mask == 0) {
        data.phase = Phase.SCANNING;
        data.columnStarted = now;
      } else {
        data.phase = Phase.RELEASE_MASK;
        data.maskStarted = now;
      }
    }
    final var hz = tickFrequencyHz(projectOf(state));
    final var period = columnStepTicks(state.getAttributeValue(COSC), hz);
    // Rows still show the column driven on the previous propagation. Settle timers, then accept
    // a press on that column, and only afterwards step to the next column.
    data.expireTimers(now);
    if (data.phase == Phase.SCANNING) {
      final var row = pressedRow(state);
      if (row >= 0) {
        final var press = pressDebounceTicks(state.getAttributeValue(CKBM), hz);
        data.candidateRow = row;
        data.maskTicks = releaseMaskTicks(state.getAttributeValue(CKBM), hz);
        if (press == 0) {
          data.accept();
        } else {
          data.phase = Phase.PRESS_DEBOUNCE;
          data.pressStarted = now;
          data.pressTicks = press;
        }
      }
    }
    data.takeColumnStep(now, period);
    driveOutputs(state, data);
  }

  private static int clampTicks(long ticks) {
    if (ticks < 1) {
      return 1;
    }
    if (ticks > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) ticks;
  }

  private static void driveOutputs(InstanceState state, EncoderData data) {
    for (var column = 0; column < ROWS; column++) {
      final var active = column == data.column;
      setPort(state, COLUMN_PINS[column], active ? Value.FALSE : Value.UNKNOWN);
    }
    setPort(state, DAV, data.dav ? Value.TRUE : Value.FALSE);
    final var enabled = state.getPortValue(pinNrToPortNr(OE)) == Value.FALSE;
    for (var bit = 0; bit < DATA_PINS.length; bit++) {
      if (!enabled) {
        setPort(state, DATA_PINS[bit], Value.UNKNOWN);
      } else {
        final var high = (data.latched & (1 << bit)) != 0;
        setPort(state, DATA_PINS[bit], high ? Value.TRUE : Value.FALSE);
      }
    }
  }

  private static int pressedRow(InstanceState state) {
    for (var row = 0; row < ROWS; row++) {
      if (isRowLow(state, row)) {
        return row;
      }
    }
    return -1;
  }

  private static boolean isRowLow(InstanceState state, int row) {
    return state.getPortValue(pinNrToPortNr(ROW_PINS[row])) == Value.FALSE;
  }

  private static void setPort(InstanceState state, byte dsPinNr, Value value) {
    state.setPort(pinNrToPortNr(dsPinNr), value, DELAY);
  }

  private static double tickFrequencyHz(Project project) {
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

  private static EncoderData getStateData(InstanceState state) {
    var data = (EncoderData) state.getData();
    if (data == null) {
      data = new EncoderData();
      state.setData(data);
    }
    return data;
  }

  private enum Phase {
    SCANNING,
    PRESS_DEBOUNCE,
    HELD,
    RELEASE_MASK
  }

  private static final class EncoderData implements InstanceData {
    private Phase phase = Phase.SCANNING;
    private int column;
    private int latched;
    private boolean dav;
    private int candidateRow;
    private int pressStarted;
    private int pressTicks = 1;
    private int maskStarted;
    private int maskTicks;
    private int columnStarted;
    private boolean timingStarted;

    @Override
    public EncoderData clone() {
      final var copy = new EncoderData();
      copy.phase = phase;
      copy.column = column;
      copy.latched = latched;
      copy.dav = dav;
      copy.candidateRow = candidateRow;
      copy.pressStarted = pressStarted;
      copy.pressTicks = pressTicks;
      copy.maskStarted = maskStarted;
      copy.maskTicks = maskTicks;
      copy.columnStarted = columnStarted;
      copy.timingStarted = timingStarted;
      return copy;
    }

    private void accept() {
      latched = candidateRow * ROWS + column;
      dav = true;
      phase = Phase.HELD;
    }

    private boolean expireTimers(int ticks) {
      if (!timingStarted) {
        timingStarted = true;
        columnStarted = ticks;
      }
      var dirty = false;
      if (phase == Phase.PRESS_DEBOUNCE && (long) ticks - pressStarted >= pressTicks) {
        accept();
        dirty = true;
      }
      if (phase == Phase.RELEASE_MASK && (long) ticks - maskStarted >= maskTicks) {
        phase = Phase.SCANNING;
        columnStarted = ticks;
        dirty = true;
      }
      return dirty;
    }

    private boolean columnStepDue(int ticks, int columnPeriod) {
      if (phase != Phase.SCANNING) {
        return false;
      }
      final var period = Math.max(1, columnPeriod);
      return (long) ticks - columnStarted >= period;
    }

    private void takeColumnStep(int ticks, int columnPeriod) {
      if (!columnStepDue(ticks, columnPeriod)) {
        return;
      }
      final var period = Math.max(1, columnPeriod);
      final var elapsed = (long) ticks - columnStarted;
      final var steps = elapsed / period;
      column = (int) ((column + steps) % ROWS);
      columnStarted = (int) (ticks - elapsed % period);
    }
  }
}
