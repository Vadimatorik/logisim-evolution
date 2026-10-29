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
import java.awt.Font;
import java.awt.Graphics;
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

    public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var boxTop = y + 18;
    final var boxBottom = boxTop + 24;
    g.drawRect(x + 6, boxTop, 312, 24);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 9));
    GraphicsUtil.drawCenteredText(g, "REG", x + 162, y + 30);
    g.setFont(previousFont);
    for (var pin = 1; pin <= 16; pin++) {
      if (pin == 8 || pin == 16) {
        continue;
      }
      final var pinX = pin <= 8 ? x + (pin - 1) * 20 + 10 : x + (16 - pin) * 20 + 10;
      final var pinY =
          pin <= 8
              ? y + height - AbstractTtlGate.PIN_HEIGHT
              : y + AbstractTtlGate.PIN_HEIGHT;
      final var edge = pin <= 8 ? boxBottom : boxTop;
      if (pin == 10 || pin == 13) {
        final var bubbleY = pin <= 8 ? edge + 4 : edge - 4;
        g.drawLine(pinX, pinY, pinX, pin <= 8 ? bubbleY + 2 : bubbleY - 2);
        structureBubble(g, pinX, bubbleY);
      } else {
        g.drawLine(pinX, pinY, pinX, edge);
      }
    }
  }

  /** Upper row is the storage register. Lower row is the shift register, whose last bit is Q. */

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
