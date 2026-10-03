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

/** VHDL and Verilog for one 74HC83 4-bit full adder. */
public class Ttl7483HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the classic 7483 pinout. */
  public Ttl7483HdlGenerator() {
    super();
    myWires
        .addWire("oppA", 5)
        .addWire("oppB", 5)
        .addWire("oppC", 5)
        .addWire("result", 5);
    myPorts
        .add(Port.INPUT, "A1", 1, Ttl7483.pinToPort(Ttl7483.A1))
        .add(Port.INPUT, "A2", 1, Ttl7483.pinToPort(Ttl7483.A2))
        .add(Port.INPUT, "A3", 1, Ttl7483.pinToPort(Ttl7483.A3))
        .add(Port.INPUT, "A4", 1, Ttl7483.pinToPort(Ttl7483.A4))
        .add(Port.INPUT, "B1", 1, Ttl7483.pinToPort(Ttl7483.B1))
        .add(Port.INPUT, "B2", 1, Ttl7483.pinToPort(Ttl7483.B2))
        .add(Port.INPUT, "B3", 1, Ttl7483.pinToPort(Ttl7483.B3))
        .add(Port.INPUT, "B4", 1, Ttl7483.pinToPort(Ttl7483.B4))
        .add(Port.INPUT, "C0", 1, Ttl7483.pinToPort(Ttl7483.C0))
        .add(Port.OUTPUT, "S1", 1, Ttl7483.pinToPort(Ttl7483.S1))
        .add(Port.OUTPUT, "S2", 1, Ttl7483.pinToPort(Ttl7483.S2))
        .add(Port.OUTPUT, "S3", 1, Ttl7483.pinToPort(Ttl7483.S3))
        .add(Port.OUTPUT, "S4", 1, Ttl7483.pinToPort(Ttl7483.S4))
        .add(Port.OUTPUT, "C4", 1, Ttl7483.pinToPort(Ttl7483.C4));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.add(
          """
          oppA   <= "0"&A4&A3&A2&A1;
          oppB   <= "0"&B4&B3&B2&B1;
          oppC   <= "0000"&C0;
          result <= std_logic_vector(unsigned(oppA)+unsigned(oppB)+unsigned(oppC));
          S1     <= result(0);
          S2     <= result(1);
          S3     <= result(2);
          S4     <= result(3);
          C4     <= result(4);
          """);
    } else {
      contents.add(
          """
          assign oppA   = {1'b0, A4, A3, A2, A1};
          assign oppB   = {1'b0, B4, B3, B2, B1};
          assign oppC   = {4'b0, C0};
          assign result = oppA + oppB + oppC;
          assign S1     = result[0];
          assign S2     = result[1];
          assign S3     = result[2];
          assign S4     = result[3];
          assign C4     = result[4];
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
