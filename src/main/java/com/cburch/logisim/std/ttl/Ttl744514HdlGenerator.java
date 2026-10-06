/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/**
 * VHDL and Verilog for the 74x4514. {@code LE} stays an ordinary input: the address latch is
 * transparent while it is high, and a level that is not high holds the stored address.
 */
public class Ttl744514HdlGenerator extends AbstractHdlGeneratorFactory {

  /**
   * Creates a generator for the latched decoder. An open {@code nE} is pulled high, so the device
   * stays disabled. Open address bits and {@code LE} are pulled low, so the latch is not
   * transparent.
   */
  public Ttl744514HdlGenerator() {
    super();
    myWires.addWire("s_enabled", 1).addRegister("s_addr", 4);
    myPorts
        .add(Port.INPUT, "LE", 1, Ttl744514.pinNrToPortNr(Ttl744514.LE), true)
        .add(Port.INPUT, "A0", 1, Ttl744514.pinNrToPortNr(Ttl744514.A0), true)
        .add(Port.INPUT, "A1", 1, Ttl744514.pinNrToPortNr(Ttl744514.A1), true)
        .add(Port.INPUT, "A2", 1, Ttl744514.pinNrToPortNr(Ttl744514.A2), true)
        .add(Port.INPUT, "A3", 1, Ttl744514.pinNrToPortNr(Ttl744514.A3), true)
        .add(Port.INPUT, "nE", 1, Ttl744514.pinNrToPortNr(Ttl744514.NE), false);
    for (var index = 0; index < 16; index++) {
      myPorts.add(Port.OUTPUT, "Q" + index, 1, Ttl744514.pinNrToPortNr(outputPin(index)));
    }
  }

  private static byte outputPin(int address) {
    return switch (address) {
      case 0 -> Ttl744514.Q0;
      case 1 -> Ttl744514.Q1;
      case 2 -> Ttl744514.Q2;
      case 3 -> Ttl744514.Q3;
      case 4 -> Ttl744514.Q4;
      case 5 -> Ttl744514.Q5;
      case 6 -> Ttl744514.Q6;
      case 7 -> Ttl744514.Q7;
      case 8 -> Ttl744514.Q8;
      case 9 -> Ttl744514.Q9;
      case 10 -> Ttl744514.Q10;
      case 11 -> Ttl744514.Q11;
      case 12 -> Ttl744514.Q12;
      case 13 -> Ttl744514.Q13;
      case 14 -> Ttl744514.Q14;
      case 15 -> Ttl744514.Q15;
      default -> throw new IllegalArgumentException("Address out of range");
    };
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      addVhdl(contents);
    } else {
      addVerilog(contents);
    }
    return contents.empty();
  }

  private static void addVhdl(LineBuffer contents) {
    contents.add(
        """
        s_enabled <= ({{not}} nE);

        latch : {{process}}(LE, A0, A1, A2, A3) {{is}}
           {{begin}}
              {{if}} (LE = '1') {{then}}
                 s_addr <= A3 & A2 & A1 & A0;
              {{end}} {{if}};
           {{end}} {{process}} latch;
        """);
    for (var index = 0; index < 16; index++) {
      contents.add(
          "Q"
              + index
              + " <= '1' {{when}} s_enabled = '1' {{and}} s_addr = \""
              + bits(index)
              + "\" {{else}} '0';");
    }
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add(
        """
        assign s_enabled = ~nE;

        always @(*)
        begin
           if (LE == 1) s_addr <= {A3, A2, A1, A0};
        end
        """);
    for (var index = 0; index < 16; index++) {
      contents.add(
          "assign Q"
              + index
              + " = (s_enabled == 1 && s_addr == 4'b"
              + bits(index)
              + ") ? 1'b1 : 1'b0;");
    }
  }

  private static String bits(int address) {
    return String.format("%4s", Integer.toBinaryString(address)).replace(' ', '0');
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) {
      return false;
    }
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
