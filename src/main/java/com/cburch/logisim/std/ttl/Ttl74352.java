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
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x352: dual 4-line to 1-line data selector with inverted outputs.
 *
 * <p>Model based on the ST M54/M74HC352 function table (October 1993). The two sections share
 * select inputs. {@code S0} is the least significant bit. A high strobe forces that section's
 * output high. A low strobe makes the output the complement of the selected data input.
 *
 * <p>The same data sheet's prose says a high strobe holds the output low. The function table in
 * that document, and the AND-OR-INVERT equation of the pin-compatible 74LS352, both hold it high.
 * This model follows the table.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown. A data
 * input that the select code does not choose has no effect.
 */
public class Ttl74352 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74352";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  /** Section 1 strobe. High inhibits the section and forces {@link #L1_Y} high. */
  public static final byte L1_EN = 1;

  /** Shared select, most significant bit. Datasheet name {@code B}. */
  public static final byte S1 = 2;

  public static final byte L1_D3 = 3;
  public static final byte L1_D2 = 4;
  public static final byte L1_D1 = 5;
  public static final byte L1_D0 = 6;

  /** Section 1 inverted output. */
  public static final byte L1_Y = 7;

  public static final byte GND = 8;

  /** Section 2 inverted output. */
  public static final byte L2_Y = 9;

  public static final byte L2_D0 = 10;
  public static final byte L2_D1 = 11;
  public static final byte L2_D2 = 12;
  public static final byte L2_D3 = 13;

  /** Shared select, least significant bit. Datasheet name {@code A}. */
  public static final byte S0 = 14;

  /** Section 2 strobe. High inhibits the section and forces {@link #L2_Y} high. */
  public static final byte L2_EN = 15;

  public static final byte VCC = 16;

  private static final byte[] OUTPUTS = new byte[] {L1_Y, L2_Y};

  private static final byte[] SECTION1_DATA = new byte[] {L1_D0, L1_D1, L1_D2, L1_D3};
  private static final byte[] SECTION2_DATA = new byte[] {L2_D0, L2_D1, L2_D2, L2_D3};

  private static final String[] PORT_NAMES = {
    "n1E Enable (active low)",
    "S1 Select (MSB)",
    "1D3 Data input 3",
    "1D2 Data input 2",
    "1D1 Data input 1",
    "1D0 Data input 0",
    "n1Y Inverted output",
    "n2Y Inverted output",
    "2D0 Data input 0",
    "2D1 Data input 1",
    "2D2 Data input 2",
    "2D3 Data input 3",
    "S0 Select (LSB)",
    "n2E Enable (active low)"
  };

  /** Creates a 74352 dual 4-line to 1-line inverting data selector. */
  public Ttl74352() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74352HdlGenerator());
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

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "n1E", "S1", "1D3", "1D2", "1D1", "1D0", "n1Y", null,
          "n2Y", "2D0", "2D1", "2D2", "2D3", "S0", "n2E", null
        });
  }

  private static Value input(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr));
  }

  private static void setOutput(InstanceState state, byte dsPinNr, Value value) {
    state.setPort(pinNrToPortNr(dsPinNr), value, DELAY);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var s1 = input(state, S1);
    final var s0 = input(state, S0);
    setOutput(state, L1_Y, sectionOutput(input(state, L1_EN), s1, s0, data(state, SECTION1_DATA)));
    setOutput(state, L2_Y, sectionOutput(input(state, L2_EN), s1, s0, data(state, SECTION2_DATA)));
  }

  private static Value[] data(InstanceState state, byte[] pins) {
    final var values = new Value[pins.length];
    for (var index = 0; index < pins.length; index++) {
      values[index] = input(state, pins[index]);
    }
    return values;
  }

  /**
   * Output of one section. Index 0 of {@code data} is D0.
   *
   * <p>A high enable inhibits the section. Otherwise the output is the complement of {@code
   * data[S1:S0]}.
   */
  private static Value sectionOutput(Value enable, Value s1, Value s0, Value[] data) {
    final var inputs = new Value[] {enable, s1, s0, data[0], data[1], data[2], data[3]};
    final var choice = new Choice();
    expand(inputs, 0, false, choice);
    return choice.value();
  }

  /** Substitutes 0 and 1 for every unknown or error input, then merges the results. */
  private static void expand(Value[] inputs, int index, boolean sawError, Choice choice) {
    if (index == inputs.length) {
      choice.sawError |= sawError;
      choice.accept(binaryOutput(inputs));
      return;
    }
    final var value = inputs[index];
    if (value == Value.TRUE || value == Value.FALSE) {
      expand(inputs, index + 1, sawError, choice);
      return;
    }
    final var error = sawError || value == Value.ERROR;
    inputs[index] = Value.FALSE;
    expand(inputs, index + 1, error, choice);
    inputs[index] = Value.TRUE;
    expand(inputs, index + 1, error, choice);
    inputs[index] = value;
  }

  /** Binary function table. Every input is {@link Value#TRUE} or {@link Value#FALSE}. */
  private static boolean binaryOutput(Value[] inputs) {
    if (inputs[0] == Value.TRUE) {
      return true;
    }
    final var select = (inputs[1] == Value.TRUE ? 2 : 0) + (inputs[2] == Value.TRUE ? 1 : 0);
    return inputs[3 + select] != Value.TRUE;
  }

  /** Merges every accepted substitution. Disagreements become unknown, or error if one was seen. */
  private static final class Choice {
    private Boolean bit;
    private boolean conflict;
    private boolean sawError;

    private void accept(boolean high) {
      if (bit == null) {
        bit = high;
        return;
      }
      if (bit != high) {
        conflict = true;
      }
    }

    private Value value() {
      if (bit == null) {
        return Value.UNKNOWN;
      }
      if (!conflict) {
        return bit ? Value.TRUE : Value.FALSE;
      }
      return sawError ? Value.ERROR : Value.UNKNOWN;
    }
  }
}
