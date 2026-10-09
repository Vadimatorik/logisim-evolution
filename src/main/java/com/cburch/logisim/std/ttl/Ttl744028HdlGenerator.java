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

/** VHDL and Verilog generator for the 74x4028 BCD-to-decimal decoder. */
public class Ttl744028HdlGenerator extends AbstractHdlGeneratorFactory {

  /**
   * Creates an HDL generator. Unconnected address bits are pulled low, so an unwired device
   * selects {@code Y0}.
   */
  public Ttl744028HdlGenerator() {
    super();
    myWires.addWire("s_addr", 4);
    myPorts
        .add(Port.OUTPUT, "Y0", 1, Ttl744028.pinNrToPortNr(Ttl744028.Y0))
        .add(Port.OUTPUT, "Y1", 1, Ttl744028.pinNrToPortNr(Ttl744028.Y1))
        .add(Port.OUTPUT, "Y2", 1, Ttl744028.pinNrToPortNr(Ttl744028.Y2))
        .add(Port.OUTPUT, "Y3", 1, Ttl744028.pinNrToPortNr(Ttl744028.Y3))
        .add(Port.OUTPUT, "Y4", 1, Ttl744028.pinNrToPortNr(Ttl744028.Y4))
        .add(Port.OUTPUT, "Y5", 1, Ttl744028.pinNrToPortNr(Ttl744028.Y5))
        .add(Port.OUTPUT, "Y6", 1, Ttl744028.pinNrToPortNr(Ttl744028.Y6))
        .add(Port.OUTPUT, "Y7", 1, Ttl744028.pinNrToPortNr(Ttl744028.Y7))
        .add(Port.OUTPUT, "Y8", 1, Ttl744028.pinNrToPortNr(Ttl744028.Y8))
        .add(Port.OUTPUT, "Y9", 1, Ttl744028.pinNrToPortNr(Ttl744028.Y9))
        .add(Port.INPUT, "A0", 1, Ttl744028.pinNrToPortNr(Ttl744028.A0), true)
        .add(Port.INPUT, "A1", 1, Ttl744028.pinNrToPortNr(Ttl744028.A1), true)
        .add(Port.INPUT, "A2", 1, Ttl744028.pinNrToPortNr(Ttl744028.A2), true)
        .add(Port.INPUT, "A3", 1, Ttl744028.pinNrToPortNr(Ttl744028.A3), true);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          s_addr <= A3 & A2 & A1 & A0;
          Y0 <= '1' {{when}} s_addr = "0000" {{else}} '0';
          Y1 <= '1' {{when}} s_addr = "0001" {{else}} '0';
          Y2 <= '1' {{when}} s_addr = "0010" {{else}} '0';
          Y3 <= '1' {{when}} s_addr = "0011" {{else}} '0';
          Y4 <= '1' {{when}} s_addr = "0100" {{else}} '0';
          Y5 <= '1' {{when}} s_addr = "0101" {{else}} '0';
          Y6 <= '1' {{when}} s_addr = "0110" {{else}} '0';
          Y7 <= '1' {{when}} s_addr = "0111" {{else}} '0';
          Y8 <= '1' {{when}} s_addr = "1000" {{else}} '0';
          Y9 <= '1' {{when}} s_addr = "1001" {{else}} '0';
          """);
    } else {
      contents.add(
          """
          assign s_addr = {A3, A2, A1, A0};
          assign Y0 = (s_addr == 4'b0000);
          assign Y1 = (s_addr == 4'b0001);
          assign Y2 = (s_addr == 4'b0010);
          assign Y3 = (s_addr == 4'b0011);
          assign Y4 = (s_addr == 4'b0100);
          assign Y5 = (s_addr == 4'b0101);
          assign Y6 = (s_addr == 4'b0110);
          assign Y7 = (s_addr == 4'b0111);
          assign Y8 = (s_addr == 4'b1000);
          assign Y9 = (s_addr == 4'b1001);
          """);
    }
    return contents.empty();
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
