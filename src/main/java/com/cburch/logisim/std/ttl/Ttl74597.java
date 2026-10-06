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
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;

/**
 * TTL 74x597: 8-bit shift register with an input storage register.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT597.pdf">Nexperia 74HC597</a>
 * and <a href="https://www.ti.com/lit/ds/symlink/cd74hc597.pdf">TI CD74HC597</a> function tables.
 * {@code STCP} loads {@code D0}..{@code D7} into the storage register. {@code PL} low copies that
 * register into the shift register immediately. {@code SHCP} shifts {@code DS} into the {@code D0}
 * stage and toward {@code Q} only while {@code PL} and {@code MR} are high. {@code MR} low with
 * {@code PL} high clears only the shift register. Both low is the datasheet's invalid combination:
 * the shift register becomes unknown and stays unknown until a later clear or load. Nexperia calls
 * the serial output {@code Q}; TI calls the same pin {@code Q7}. Nanosecond delays are not
 * modeled.
 */
public class Ttl74597 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74597";

  public static final int DELAY = 1;
  private static final int STAGES = 8;

  public static final byte D1 = 1;
  public static final byte D2 = 2;
  public static final byte D3 = 3;
  public static final byte D4 = 4;
  public static final byte D5 = 5;
  public static final byte D6 = 6;
  public static final byte D7 = 7;
  public static final byte GND = 8;
  /** Serial output of the shift register. TI calls this pin {@code Q7}. */
  public static final byte Q = 9;
  /** Asynchronous shift-register reset, active low. */
  public static final byte MR = 10;
  /** Shift register clock, rising edge. */
  public static final byte SHCP = 11;
  /** Storage register clock, rising edge. */
  public static final byte STCP = 12;
  /** Asynchronous copy from storage into the shift register, active low. */
  public static final byte PL = 13;
  public static final byte DS = 14;
  public static final byte D0 = 15;
  public static final byte VCC = 16;

  private static final byte[] DATA_PINS = {D0, D1, D2, D3, D4, D5, D6, D7};
  private static final byte[] OUTPUT_PINS = {Q};
  private static final String[] PORT_NAMES = {
    "D1",
    "D2",
    "D3",
    "D4",
    "D5",
    "D6",
    "D7",
    "Q / Q7",
    "MR (master reset, active low)",
    "SHCP",
    "STCP",
    "PL (parallel load, active low)",
    "DS",
    "D0"
  };
  private static final String[] PIN_NAMES = {
    "D1", "D2", "D3", "D4", "D5", "D6", "D7", null,
    "Q", "MR", "SHCP", "STCP", "PL", "DS", "D0", null
  };

  /** Creates a 74597 shift register with an input storage register. */
  public Ttl74597() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74597HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  private static State getData(InstanceState state) {
    var data = (State) state.getData();
    if (data == null) {
      data = new State();
      state.setData(data);
    }
    return data;
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_NAMES);
    drawState(g, x, y, getData(painter));
  }

  /** Upper row is the storage register. Lower row is the shift register, whose last bit is Q. */
  private void drawState(Graphics2D g, int x, int y, State state) {
    for (var stage = 0; stage < STAGES; stage++) {
      drawBit(g, x + 36 + stage * 12, y + 20, state.storage[stage]);
      drawBit(g, x + 36 + stage * 12, y + 34, state.shift[stage]);
    }
  }

  private void drawBit(Graphics2D g, int x, int y, Value bit) {
    g.setColor(bit.getColor());
    g.fillOval(x - 4, y - 4, 8, 8);
    g.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(g, bit.toDisplayString(), x, y);
    g.setColor(Color.BLACK);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getData(state);
    final var shiftEdge = data.updateClock(input(state, SHCP), 0);
    final var storeEdge = data.updateClock(input(state, STCP), 1);
    if (storeEdge) {
      loadStorage(data, state);
    }
    final var load = input(state, PL);
    final var reset = input(state, MR);
    if (reset == Value.FALSE && load == Value.FALSE) {
      invalidateShift(data);
    } else if (reset == Value.FALSE && load == Value.TRUE) {
      clearShift(data);
    } else if (reset == Value.TRUE && load == Value.FALSE) {
      copyStorage(data);
    } else if (reset == Value.TRUE && load == Value.TRUE && shiftEdge) {
      shiftIn(data, input(state, DS));
    }
    state.setPort(pinNrToPortNr(Q), data.shift[STAGES - 1], DELAY);
  }

  private static void loadStorage(State data, InstanceState state) {
    for (var stage = 0; stage < STAGES; stage++) {
      data.storage[stage] = input(state, DATA_PINS[stage]);
    }
  }

  private static void clearShift(State data) {
    Arrays.fill(data.shift, Value.FALSE);
  }

  private static void invalidateShift(State data) {
    Arrays.fill(data.shift, Value.UNKNOWN);
  }

  private static void copyStorage(State data) {
    data.shift = data.storage.clone();
  }

  private static void shiftIn(State data, Value serial) {
    for (var stage = STAGES - 1; stage > 0; stage--) {
      data.shift[stage] = data.shift[stage - 1];
    }
    data.shift[0] = serial;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(SHCP), pinNrToPortNr(STCP)};
  }

  /**
   * Storage register, shift register and the previous level of both clocks.
   *
   * <p>Index 0 is {@code D0}. Index 7 drives {@code Q}. Startup follows {@link
   * AppPreferences#Memory_Startup_Unknown}.
   */
  private static final class State extends ClockState implements InstanceData {
    private Value[] storage = new Value[STAGES];
    private Value[] shift = new Value[STAGES];

    private State() {
      final var initial =
          AppPreferences.Memory_Startup_Unknown.get() ? Value.UNKNOWN : Value.FALSE;
      Arrays.fill(storage, initial);
      Arrays.fill(shift, initial);
    }

    @Override
    public State clone() {
      final var copy = (State) super.clone();
      copy.storage = storage.clone();
      copy.shift = shift.clone();
      return copy;
    }
  }
}
