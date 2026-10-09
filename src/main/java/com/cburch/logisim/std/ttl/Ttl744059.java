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
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * TTL 74x4059: programmable divide-by-n counter.
 *
 * <p>Simulation follows the
 * <a href="https://media.digikey.com/pdf/Data%20Sheets/NXP%20PDFs/74HC(T)4059.pdf">74HC/HCT4059</a>
 * data sheet, whose pinout matches the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc4059.pdf">CD74HC4059</a>. {@code CP} counts on
 * the low-to-high edge. While {@code Kb} and {@code Kc} are low the counter is in master preset and
 * each rising edge samples {@code J1} to {@code J16}. The first rising edge after preset only arms
 * the counter, so the first {@code Q} pulse is {@code N + 1} clocks after preset and later pulses
 * are {@code N} apart. {@code Q} is one clock wide unless {@code LE} is high in a counting mode, in
 * which case {@code Q} stays high until {@code LE} falls. There is no HDL model.
 */
public class Ttl744059 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744059";

  public static final int PORT_INDEX_CP = 0;
  public static final int PORT_INDEX_LE = 1;
  public static final int PORT_INDEX_J1 = 2;
  public static final int PORT_INDEX_J2 = 3;
  public static final int PORT_INDEX_J3 = 4;
  public static final int PORT_INDEX_J4 = 5;
  public static final int PORT_INDEX_J16 = 6;
  public static final int PORT_INDEX_J15 = 7;
  public static final int PORT_INDEX_J14 = 8;
  public static final int PORT_INDEX_J13 = 9;
  public static final int PORT_INDEX_KC = 10;
  public static final int PORT_INDEX_KB = 11;
  public static final int PORT_INDEX_KA = 12;
  public static final int PORT_INDEX_J12 = 13;
  public static final int PORT_INDEX_J11 = 14;
  public static final int PORT_INDEX_J10 = 15;
  public static final int PORT_INDEX_J9 = 16;
  public static final int PORT_INDEX_J8 = 17;
  public static final int PORT_INDEX_J7 = 18;
  public static final int PORT_INDEX_J6 = 19;
  public static final int PORT_INDEX_J5 = 20;
  public static final int PORT_INDEX_Q = 21;

  private static final int DELAY = 4;
  private static final int INHIBIT_DIVISOR = 10_000;
  /** {@code J1} is bit 0. */
  private static final int[] JAM_PORTS = {
    PORT_INDEX_J1,
    PORT_INDEX_J2,
    PORT_INDEX_J3,
    PORT_INDEX_J4,
    PORT_INDEX_J5,
    PORT_INDEX_J6,
    PORT_INDEX_J7,
    PORT_INDEX_J8,
    PORT_INDEX_J9,
    PORT_INDEX_J10,
    PORT_INDEX_J11,
    PORT_INDEX_J12,
    PORT_INDEX_J13,
    PORT_INDEX_J14,
    PORT_INDEX_J15,
    PORT_INDEX_J16
  };
  private static final byte[] OUTPUT_PORTS = {23};
  private static final String[] PORT_NAMES = {
    "CP (Clock, LOW-to-HIGH)",
    "LE (Latch enable, active HIGH)",
    "J1",
    "J2",
    "J3",
    "J4",
    "J16",
    "J15",
    "J14",
    "J13",
    "Kc (Mode select)",
    "Kb (Mode select)",
    "Ka (Mode select)",
    "J12",
    "J11",
    "J10",
    "J9",
    "J8",
    "J7",
    "J6",
    "J5",
    "Q (Divide-by-n output)"
  };

  /** Creates a 744059 programmable divide-by-n counter. */
  public Ttl744059() {
    super(_ID, (byte) 24, OUTPUT_PORTS, PORT_NAMES, null);
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
          "CP", "LE", "J1", "J2", "J3", "J4", "J16", "J15", "J14", "J13", "Kc", null,
          "Kb", "Ka", "J12", "J11", "J10", "J9", "J8", "J7", "J6", "J5", "Q", null
        });
    final var data = (CounterData) painter.getData();
    if (data == null) return;
    final var shown = data.lastOutput;
    gfx.setColor(shown.getColor());
    gfx.fillOval(x + 116, y + height / 2 - 6, 12, 12);
    gfx.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(gfx, shown.toDisplayString(), x + 122, y + height / 2);
    gfx.setColor(Color.BLACK);
  }

  private static CounterData getStateData(InstanceState state) {
    var data = (CounterData) state.getData();
    if (data == null) {
      data = new CounterData();
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var latchEnable = state.getPortValue(PORT_INDEX_LE);
    final var ka = state.getPortValue(PORT_INDEX_KA);
    final var kb = state.getPortValue(PORT_INDEX_KB);
    final var kc = state.getPortValue(PORT_INDEX_KC);
    final var clocked =
        data.updateClock(state.getPortValue(PORT_INDEX_CP), StdAttr.TRIG_RISING);
    final var controlFault = faultOf(ka, kb, kc, latchEnable);
    if (latchEnable == Value.FALSE) data.latched = false;
    if (controlFault != null) {
      if (clocked) data.invalidate(controlFault);
    } else if (clocked) {
      data.clock(
          decode(latchEnable == Value.TRUE, ka == Value.TRUE, kb == Value.TRUE, kc == Value.TRUE),
          readJam(state));
    }
    data.lastOutput = data.output(latchEnable);
    state.setPort(PORT_INDEX_Q, data.lastOutput, DELAY);
  }

  private static Value faultOf(Value... levels) {
    var unknown = false;
    for (final var level : levels) {
      if (level == Value.ERROR) return Value.ERROR;
      if (level != Value.TRUE && level != Value.FALSE) unknown = true;
    }
    return unknown ? Value.UNKNOWN : null;
  }

  /**
   * Master preset is {@code Kb = Kc = L}. {@code LE = L} with {@code Ka = L}, {@code Kb = H} and
   * {@code Kc = L} is the fixed divide-by-10000 mode. The same selects with {@code LE = H} count
   * by 10 and do not latch {@code Q}.
   */
  private static Mode decode(boolean latchHigh, boolean kaHigh, boolean kbHigh, boolean kcHigh) {
    if (!kbHigh && !kcHigh) return new Mode(true, false, false, 0);
    if (!kaHigh && kbHigh && !kcHigh) {
      return latchHigh ? new Mode(false, false, false, 10) : new Mode(false, true, false, 10);
    }
    final int modulus;
    if (kaHigh && kbHigh && kcHigh) modulus = 2;
    else if (!kaHigh && kbHigh && kcHigh) modulus = 4;
    else if (kaHigh && !kbHigh && kcHigh) modulus = 5;
    else if (!kaHigh && !kbHigh && kcHigh) modulus = 8;
    else modulus = 10;
    return new Mode(false, false, latchHigh, modulus);
  }

  private static Jam readJam(InstanceState state) {
    var bits = 0;
    Value fault = null;
    for (var index = 0; index < JAM_PORTS.length; index++) {
      final var level = state.getPortValue(JAM_PORTS[index]);
      if (level == Value.ERROR) fault = Value.ERROR;
      else if (level != Value.TRUE && level != Value.FALSE && fault != Value.ERROR) {
        fault = Value.UNKNOWN;
      } else if (level == Value.TRUE) bits |= 1 << index;
    }
    return new Jam(bits, fault);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CP};
  }

  private record Mode(
      boolean masterPreset, boolean presetInhibit, boolean latchEnabled, int modulus) {}

  private record Jam(int bits, Value fault) {}

  private static final class CounterData extends ClockState implements InstanceData {
    private boolean armed;
    private boolean defined;
    private boolean pulse;
    private boolean latched;
    private boolean jamValid;
    private int storedJam;
    private int phase;
    private int slow;
    private Value fault = Value.UNKNOWN;
    private Value lastOutput = Value.UNKNOWN;

    @Override
    public CounterData clone() {
      return (CounterData) super.clone();
    }

    private void invalidate(Value reason) {
      defined = false;
      armed = false;
      pulse = false;
      fault = reason;
    }

    private void clock(Mode mode, Jam jam) {
      if (mode.masterPreset) {
        preset(jam);
        return;
      }
      if (!armed) {
        arm(mode);
        return;
      }
      pulse = false;
      if (!defined) return;
      if (!reachedTerminal(mode.modulus)) return;
      pulse = true;
      if (mode.latchEnabled) latched = true;
      if (mode.presetInhibit) {
        phase = 0;
        slow = INHIBIT_DIVISOR / mode.modulus;
        return;
      }
      if (jam.fault != null) {
        invalidate(jam.fault);
        return;
      }
      load(jam.bits, mode.modulus);
    }

    private void preset(Jam jam) {
      pulse = false;
      armed = false;
      if (jam.fault != null) {
        invalidate(jam.fault);
        return;
      }
      storedJam = jam.bits;
      jamValid = true;
      defined = true;
      fault = null;
    }

    /** The arming edge loads the JAM word sampled by master preset and does not count. */
    private void arm(Mode mode) {
      pulse = false;
      if (!jamValid) {
        invalidate(Value.UNKNOWN);
        return;
      }
      load(storedJam, mode.modulus);
      armed = true;
      defined = true;
      fault = null;
    }

    /**
     * Counts down one clock. {@code phase} is the first-section remainder. Each time it expires,
     * {@code slow} loses one and the first section reloads to a full modulus cycle.
     */
    private boolean reachedTerminal(int modulus) {
      if (phase > 0) {
        phase--;
        return phase == 0 && slow == 0;
      }
      if (slow == 0) return true;
      slow--;
      phase = modulus - 1;
      return phase == 0 && slow == 0;
    }

    private void load(int jam, int modulus) {
      final int dec2 = (jam >> 4) & 0xF;
      final int dec3 = (jam >> 8) & 0xF;
      final int dec4 = (jam >> 12) & 0xF;
      final int dec1;
      final int dec5;
      if (modulus == 2) {
        dec1 = jam & 0x1;
        dec5 = (jam >> 1) & 0x7;
      } else if (modulus == 4) {
        dec1 = jam & 0x3;
        dec5 = (jam >> 2) & 0x3;
      } else if (modulus == 10) {
        dec1 = jam & 0xF;
        dec5 = 0;
      } else {
        dec1 = jam & 0x7;
        dec5 = (jam >> 3) & 0x1;
      }
      phase = dec1;
      slow = dec5 * 1000 + dec4 * 100 + dec3 * 10 + dec2;
    }

    private Value output(Value latchEnable) {
      if (!defined) return fault == null ? Value.UNKNOWN : fault;
      if (latched) {
        if (latchEnable == Value.TRUE) return Value.TRUE;
        if (latchEnable == Value.ERROR) return Value.ERROR;
        if (latchEnable != Value.FALSE) return Value.UNKNOWN;
      }
      return pulse ? Value.TRUE : Value.FALSE;
    }
  }
}
