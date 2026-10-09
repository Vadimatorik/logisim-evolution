/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.prefs.AppPreferences;

/**
 * TTL 74HC544: octal inverting registered transceiver with three-state outputs.
 *
 * <p>Simulation follows the inverting 74x544 function table published for the Philips 74F544 and the
 * <a href="https://www.nxp.com/docs/en/data-sheet/74LVC544A.pdf">NXP 74LVC544A</a>. ON
 * Semiconductor's MC74HC544A is pin-compatible with the LS544, and that DIP-24 pinout matches both
 * of those sheets: pin 12 is GND and pin 24 is VCC. Each direction has its own latch. The latch is
 * transparent while {@code nE} and {@code nLE} are both low, and it stores the bus on a rising
 * {@code nLE} while {@code nE} stays low or a rising {@code nE} while {@code nLE} stays low. The
 * opposite bus is driven with the inverted stored word only while {@code nE} and {@code nOE} are
 * both low. Any other level releases that bus. High impedance is reported as unknown. A bus this
 * chip is already driving is not sampled back into its latch.
 */
public class Ttl74544 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74544";

  public static final int PORT_INDEX_nLEBA = 0;
  public static final int PORT_INDEX_nOEBA = 1;
  public static final int PORT_INDEX_A0 = 2;
  public static final int PORT_INDEX_A1 = 3;
  public static final int PORT_INDEX_A2 = 4;
  public static final int PORT_INDEX_A3 = 5;
  public static final int PORT_INDEX_A4 = 6;
  public static final int PORT_INDEX_A5 = 7;
  public static final int PORT_INDEX_A6 = 8;
  public static final int PORT_INDEX_A7 = 9;
  public static final int PORT_INDEX_nEAB = 10;
  public static final int PORT_INDEX_nOEAB = 11;
  public static final int PORT_INDEX_nLEAB = 12;
  public static final int PORT_INDEX_B7 = 13;
  public static final int PORT_INDEX_B6 = 14;
  public static final int PORT_INDEX_B5 = 15;
  public static final int PORT_INDEX_B4 = 16;
  public static final int PORT_INDEX_B3 = 17;
  public static final int PORT_INDEX_B2 = 18;
  public static final int PORT_INDEX_B1 = 19;
  public static final int PORT_INDEX_B0 = 20;
  public static final int PORT_INDEX_nEBA = 21;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 22;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 23;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  /** A-bus port of each bit. Bit 0 is A0, on physical pin 3. */
  private static final int[] BUS_A = {
    PORT_INDEX_A0,
    PORT_INDEX_A1,
    PORT_INDEX_A2,
    PORT_INDEX_A3,
    PORT_INDEX_A4,
    PORT_INDEX_A5,
    PORT_INDEX_A6,
    PORT_INDEX_A7
  };
  /** B-bus port of each bit. Bit 0 is B0, on physical pin 22. */
  private static final int[] BUS_B = {
    PORT_INDEX_B0,
    PORT_INDEX_B1,
    PORT_INDEX_B2,
    PORT_INDEX_B3,
    PORT_INDEX_B4,
    PORT_INDEX_B5,
    PORT_INDEX_B6,
    PORT_INDEX_B7
  };
  private static final byte[] INOUT_PINS = {
    3, 4, 5, 6, 7, 8, 9, 10, 15, 16, 17, 18, 19, 20, 21, 22
  };
  private static final String[] PORT_NAMES = {
    "nLEBA (B-to-A latch enable, active low)",
    "nOEBA (B-to-A output enable, active low)",
    "A0",
    "A1",
    "A2",
    "A3",
    "A4",
    "A5",
    "A6",
    "A7",
    "nEAB (A-to-B enable, active low)",
    "nOEAB (A-to-B output enable, active low)",
    "nLEAB (A-to-B latch enable, active low)",
    "B7",
    "B6",
    "B5",
    "B4",
    "B3",
    "B2",
    "B1",
    "B0",
    "nEBA (B-to-A enable, active low)"
  };

  /** Creates a 74544 octal inverting registered transceiver. */
  public Ttl74544() {
    super(
        _ID,
        (byte) 24,
        new byte[] {},
        new byte[] {},
        INOUT_PINS,
        PORT_NAMES,
        new Ttl74544HdlGenerator());
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
          "nLEBA", "nOEBA", "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "nEAB", null,
          "nOEAB", "nLEAB", "B7", "B6", "B5", "B4", "B3", "B2", "B1", "B0", "nEBA", null
        });
  }

  /**
   * Releases both buses when the exposed supply pins are not a valid ground and positive rail. The
   * base implementation only clears output ports, and these data pins are bidirectional.
   */
  @Override
  public void propagate(InstanceState state) {
    if (exposedPowerIsInvalid(state)) {
      releaseBothBuses(state);
      return;
    }
    propagateTtl(state);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    final var fromA = readBus(state, BUS_A);
    final var fromB = readBus(state, BUS_B);
    final var enableAb = state.getPortValue(PORT_INDEX_nEAB);
    final var latchAb = state.getPortValue(PORT_INDEX_nLEAB);
    final var enableBa = state.getPortValue(PORT_INDEX_nEBA);
    final var latchBa = state.getPortValue(PORT_INDEX_nLEBA);
    final var driveB = enabled(enableAb, state.getPortValue(PORT_INDEX_nOEAB));
    final var driveA = enabled(enableBa, state.getPortValue(PORT_INDEX_nOEBA));
    data.aToB.update(fromA, enableAb, latchAb, driveA);
    data.bToA.update(fromB, enableBa, latchBa, driveB);
    if (driveB) drive(state, BUS_B, data.aToB.stored);
    else release(state, BUS_B);
    if (driveA) drive(state, BUS_A, data.bToA.stored);
    else release(state, BUS_A);
  }

  private static StateData stateData(InstanceState state) {
    var data = (StateData) state.getData();
    if (data == null) {
      data = new StateData();
      state.setData(data);
    }
    return data;
  }

  /** A direction drives only while both its active-low enables are solid lows. */
  private static boolean enabled(Value pathEnable, Value outputEnable) {
    return pathEnable == Value.FALSE && outputEnable == Value.FALSE;
  }

  private static boolean exposedPowerIsInvalid(InstanceState state) {
    if (!state.getAttributeValue(TtlLibrary.VCC_GND)) return false;
    return state.getPortValue(PORT_INDEX_GND) != Value.FALSE
        || state.getPortValue(PORT_INDEX_VCC) != Value.TRUE;
  }

  private static Value[] readBus(InstanceState state, int[] ports) {
    final var values = new Value[BITS];
    for (var bit = 0; bit < BITS; bit++) {
      values[bit] = state.getPortValue(ports[bit]);
    }
    return values;
  }

  /** Drives {@code ports} with the complement of {@code stored}. An invalid bit becomes error. */
  private static void drive(InstanceState state, int[] ports, Value[] stored) {
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(ports[bit], stored[bit].not(), DELAY);
    }
  }

  private static void releaseBothBuses(InstanceState state) {
    release(state, BUS_A);
    release(state, BUS_B);
  }

  private static void release(InstanceState state, int[] ports) {
    for (final var port : ports) {
      state.setPort(port, Value.UNKNOWN, DELAY);
    }
  }

  /**
   * One direction's stored word. While {@code nE} and {@code nLE} are low the word follows the
   * input bus. Leaving that state on a rising {@code nE} or {@code nLE} stores the bus present on
   * that edge. The word is left alone when this chip is driving the same bus.
   */
  private static final class DirectionLatch {
    private final Value[] stored = new Value[BITS];
    private Value prevEnable = Value.UNKNOWN;
    private Value prevLatch = Value.UNKNOWN;
    private boolean prevInputDriven = false;

    private DirectionLatch() {
      final var reset = AppPreferences.Memory_Startup_Unknown.get() ? Value.UNKNOWN : Value.FALSE;
      for (var bit = 0; bit < BITS; bit++) {
        stored[bit] = reset;
      }
    }

    private void update(Value[] inputs, Value enable, Value latchEnable, boolean inputDriven) {
      final var risingEnable = prevEnable == Value.FALSE && enable == Value.TRUE;
      final var risingLatch = prevLatch == Value.FALSE && latchEnable == Value.TRUE;
      final var open = enable == Value.FALSE && latchEnable == Value.FALSE && !inputDriven;
      final var wasOpen = prevEnable == Value.FALSE && prevLatch == Value.FALSE && !prevInputDriven;
      if (!inputDriven && (open || (wasOpen && (risingEnable || risingLatch)))) {
        System.arraycopy(inputs, 0, stored, 0, BITS);
      }
      prevEnable = enable;
      prevLatch = latchEnable;
      prevInputDriven = inputDriven;
    }

    private void copyFrom(DirectionLatch other) {
      System.arraycopy(other.stored, 0, stored, 0, BITS);
      prevEnable = other.prevEnable;
      prevLatch = other.prevLatch;
      prevInputDriven = other.prevInputDriven;
    }
  }

  /** Instance-owned latches for the two directions. */
  private static final class StateData implements InstanceData, Cloneable {
    private final DirectionLatch aToB = new DirectionLatch();
    private final DirectionLatch bToA = new DirectionLatch();

    @Override
    public StateData clone() {
      final var copy = new StateData();
      copy.aToB.copyFrom(aToB);
      copy.bToA.copyFrom(bToA);
      return copy;
    }
  }
}
