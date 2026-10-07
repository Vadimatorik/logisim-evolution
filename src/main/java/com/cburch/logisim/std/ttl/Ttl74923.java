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
 * TTL 74923: 20-key matrix encoder.
 *
 * <p>Model based on the Fairchild MM74C923 datasheet (DS006037). Parts marked 74C923 and 74HC923
 * use this 20-pin function. The key-bounce pin is a capacitor, not a logic net. There is no HDL
 * model: the scan outputs are open-drain, the data outputs are three-state, and the mask interval
 * is an RC time counted in simulator ticks.
 *
 * <p>A rising edge on {@code OSC} advances one column while the encoder is scanning, in the order
 * {@code X1}, {@code X2}, {@code X3}, {@code X4}. A driven {@code OSC} pin disables the capacitor
 * oscillator. When {@code OSC} is left floating and {@code Cosc} is above zero, the column step
 * rate follows {@code FSCAN = 1e-4 / Cosc(F)}, or {@code f(Hz) = 1e8 / Cosc(pF)}. {@code Cosc = 0}
 * leaves that oscillator off.
 *
 * <p>Press debounce is {@code T1 = R·C} with {@code R = 10 kΩ}. The release mask is {@code T3 =
 * 0.7·R·C}. Both are counted in simulator ticks, and a shorter interval lasts one tick. {@code
 * Ckbm = 0} removes the capacitor and defeats debounce. The datasheet interval T2 is the same
 * typical size as T1 and is not a separate timer. A floating row reads as not pressed. When
 * several rows are low together, the lowest row number wins; the datasheet does not define that
 * case. {@code nOE} must be low to drive the latched code. Any other level, including unknown,
 * releases {@code A} through {@code E}. {@code DAV} stays a push-pull output.
 */
public class Ttl74923 extends AbstractTtlGate implements TickAware {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74923";

  public static final byte Y1 = 1;
  public static final byte Y2 = 2;
  public static final byte Y3 = 3;
  public static final byte Y4 = 4;
  public static final byte Y5 = 5;
  public static final byte OSC = 6;
  public static final byte X4 = 8;
  public static final byte X3 = 9;
  public static final byte X2 = 11;
  public static final byte X1 = 12;
  public static final byte DAV = 13;
  public static final byte OE = 14;
  public static final byte DATA_E = 15;
  public static final byte DATA_D = 16;
  public static final byte DATA_C = 17;
  public static final byte DATA_B = 18;
  public static final byte DATA_A = 19;
  public static final byte GND = 10;
  public static final byte VCC = 20;

  /** {@code f(Hz) = SCAN_FREQUENCY_FACTOR / Cosc(pF)}, equal to {@code 1e-4 / Cosc(F)}. */
  public static final double SCAN_FREQUENCY_FACTOR = 1.0e8;

  /** {@code T1(s) = DEBOUNCE_SECONDS_PER_PF × Ckbm(pF)} for the internal 10 kΩ mask resistor. */
  public static final double DEBOUNCE_SECONDS_PER_PF = 1.0e-8;

  /** Release mask T3 is 0.7 of the press-debounce interval. */
  public static final double RELEASE_MASK_FACTOR = 0.7;

  /** Oscillator capacitor, in picofarads. Zero leaves the internal scan oscillator off. */
  public static final Attribute<Integer> COSC =
      Attributes.forIntegerRange("Cosc", S.getter("ttl74923Cosc"), 0, 10_000_000);

  /** Key-bounce mask capacitor, in picofarads. Zero omits the capacitor. */
  public static final Attribute<Integer> CKBM =
      Attributes.forIntegerRange("Ckbm", S.getter("ttl74923Ckbm"), 0, 1_000_000_000);

  private static final int DELAY = 1;
  private static final int ROW_COUNT = 5;
  private static final int COLUMN_COUNT = 4;
  private static final byte KBM = 7;
  private static final byte[] ROW_PINS = {Y1, Y2, Y3, Y4, Y5};
  private static final byte[] COLUMN_PINS = {X1, X2, X3, X4};
  private static final byte[] DATA_PINS = {DATA_A, DATA_B, DATA_C, DATA_D, DATA_E};
  private static final byte[] OUTPUT_PORTS = {
    X4, X3, X2, X1, DAV, DATA_E, DATA_D, DATA_C, DATA_B, DATA_A
  };
  private static final byte[] UNUSED_PINS = {KBM};
  private static final String[] PORT_NAMES = {
    "Y1 (row 1, internal pull-up)",
    "Y2 (row 2, internal pull-up)",
    "Y3 (row 3, internal pull-up)",
    "Y4 (row 4, internal pull-up)",
    "Y5 (row 5, internal pull-up)",
    "OSC (scan clock, rising edge)",
    "X4 (column 4, open drain)",
    "X3 (column 3, open drain)",
    "X2 (column 2, open drain)",
    "X1 (column 1, open drain)",
    "DAV (data available)",
    "nOE (output enable, active low)",
    "E (latched code, MSB)",
    "D (latched code)",
    "C (latched code)",
    "B (latched code)",
    "A (latched code, LSB)"
  };

  /** Creates a 74923 20-key encoder. */
  public Ttl74923() {
    super(_ID, (byte) 20, OUTPUT_PORTS, UNUSED_PINS, PORT_NAMES, null);
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          TtlLibrary.VCC_GND,
          TtlLibrary.DRAW_INTERNAL_STRUCTURE,
          StdAttr.LABEL,
          COSC,
          CKBM
        },
        new Object[] {Direction.EAST, false, false, "", 0, 0});
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>The key-bounce pin and the power pins are omitted. Exposed power pins are appended after
   * the signal ports.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    byte port = 0;
    for (byte pin = 1; pin < dsPinNr; pin++) {
      if (pin == KBM || pin == GND || pin == VCC) {
        continue;
      }
      port++;
    }
    return port;
  }

  /**
   * Column dwell time in simulator ticks. Zero means the capacitor oscillator is off.
   *
   * @param coscPf oscillator capacitor in picofarads
   * @param tickFrequencyHz simulator tick frequency in hertz
   * @return dwell time in ticks, or zero when the oscillator is off
   */
  public static int columnStepTicks(int coscPf, double tickFrequencyHz) {
    if (coscPf <= 0) {
      return 0;
    }
    if (tickFrequencyHz <= 0) {
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
    final var period =
        data.externalClock ? 0 : columnStepTicks(comp.getAttributeSet().getValue(COSC), hz);
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
          "Y1", "Y2", "Y3", "Y4", "Y5", "OSC", "KBM", "X4", "X3", null,
          "X2", "X1", "DAV", "nOE", "E", "D", "C", "B", "A", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var now = state.getTickCount();
    final var osc = state.getPortValue(pinNrToPortNr(OSC));
    final var driven = osc == Value.TRUE || osc == Value.FALSE;
    final var rising = data.osc == Value.FALSE && osc == Value.TRUE;
    data.externalClock = driven;
    data.osc = osc;
    noteRowRelease(state, data, now);
    final var hz = tickFrequencyHz(projectOf(state));
    final var period = driven ? 0 : columnStepTicks(state.getAttributeValue(COSC), hz);
    // Rows still show the column driven on the previous propagation. Settle timers, then accept
    // a press on that column, and only afterwards step to the next column.
    data.expireTimers(now);
    considerPress(state, data, now, hz);
    if (driven) {
      if (rising) {
        data.advanceColumn();
      }
    } else {
      data.takeColumnStep(now, period);
    }
    driveOutputs(state, data);
  }

  private static void noteRowRelease(InstanceState state, EncoderData data, int now) {
    if (data.phase == Phase.PRESS_DEBOUNCE && !isRowLow(state, data.candidateRow)) {
      data.phase = Phase.SCANNING;
      data.columnStarted = now;
    }
    if (data.phase == Phase.HELD && !isRowLow(state, data.candidateRow)) {
      data.dav = false;
      if (data.maskTicks == 0) {
        data.phase = Phase.SCANNING;
        data.columnStarted = now;
      } else {
        data.phase = Phase.RELEASE_MASK;
        data.maskStarted = now;
      }
    }
  }

  private static void considerPress(InstanceState state, EncoderData data, int now, double hz) {
    if (data.phase != Phase.SCANNING) {
      return;
    }
    final var row = pressedRow(state);
    if (row < 0) {
      return;
    }
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
    for (var column = 0; column < COLUMN_COUNT; column++) {
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
    for (var row = 0; row < ROW_COUNT; row++) {
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
    private boolean externalClock;
    private Value osc = Value.UNKNOWN;

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
      copy.externalClock = externalClock;
      copy.osc = osc;
      return copy;
    }

    private void accept() {
      latched = candidateRow * COLUMN_COUNT + column;
      dav = true;
      phase = Phase.HELD;
    }

    private void advanceColumn() {
      if (phase != Phase.SCANNING) {
        return;
      }
      column = (column + 1) % COLUMN_COUNT;
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
      if (phase != Phase.SCANNING || columnPeriod <= 0) {
        return false;
      }
      return (long) ticks - columnStarted >= columnPeriod;
    }

    private void takeColumnStep(int ticks, int columnPeriod) {
      if (!columnStepDue(ticks, columnPeriod)) {
        return;
      }
      final var elapsed = (long) ticks - columnStarted;
      final var steps = elapsed / columnPeriod;
      column = (int) ((column + steps) % COLUMN_COUNT);
      columnStarted = (int) (ticks - elapsed % columnPeriod);
    }
  }
}
