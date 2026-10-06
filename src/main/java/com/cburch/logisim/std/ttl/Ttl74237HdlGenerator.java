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

/** VHDL and Verilog generator for the 74x237 decoder with address latches. */
public class Ttl74237HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose open controls leave the device latched and disabled. */
  public Ttl74237HdlGenerator() {
    super();
    myWires.addWire("s_enabled", 1).addRegister("s_addr", 3);
    // Open address pins read as zero. nLE and nE1 pull high, so an open device is latched and
    // not enabled. E2 pulls low, which also leaves the outputs disabled.
    myPorts
        .add(Port.INPUT, "A0", 1, Ttl74237.pinNrToPortNr(Ttl74237.A0), true)
        .add(Port.INPUT, "A1", 1, Ttl74237.pinNrToPortNr(Ttl74237.A1), true)
        .add(Port.INPUT, "A2", 1, Ttl74237.pinNrToPortNr(Ttl74237.A2), true)
        .add(Port.INPUT, "nLE", 1, Ttl74237.pinNrToPortNr(Ttl74237.LE), false)
        .add(Port.INPUT, "nE1", 1, Ttl74237.pinNrToPortNr(Ttl74237.E1), false)
        .add(Port.INPUT, "E2", 1, Ttl74237.pinNrToPortNr(Ttl74237.E2), true)
        .add(Port.OUTPUT, "Y0", 1, Ttl74237.pinNrToPortNr(Ttl74237.Y0))
        .add(Port.OUTPUT, "Y1", 1, Ttl74237.pinNrToPortNr(Ttl74237.Y1))
        .add(Port.OUTPUT, "Y2", 1, Ttl74237.pinNrToPortNr(Ttl74237.Y2))
        .add(Port.OUTPUT, "Y3", 1, Ttl74237.pinNrToPortNr(Ttl74237.Y3))
        .add(Port.OUTPUT, "Y4", 1, Ttl74237.pinNrToPortNr(Ttl74237.Y4))
        .add(Port.OUTPUT, "Y5", 1, Ttl74237.pinNrToPortNr(Ttl74237.Y5))
        .add(Port.OUTPUT, "Y6", 1, Ttl74237.pinNrToPortNr(Ttl74237.Y6))
        .add(Port.OUTPUT, "Y7", 1, Ttl74237.pinNrToPortNr(Ttl74237.Y7));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          s_enabled <= ({{not}} nE1) {{and}} E2;
          Y0 <= s_enabled {{when}} s_addr = "000" {{else}} '0';
          Y1 <= s_enabled {{when}} s_addr = "001" {{else}} '0';
          Y2 <= s_enabled {{when}} s_addr = "010" {{else}} '0';
          Y3 <= s_enabled {{when}} s_addr = "011" {{else}} '0';
          Y4 <= s_enabled {{when}} s_addr = "100" {{else}} '0';
          Y5 <= s_enabled {{when}} s_addr = "101" {{else}} '0';
          Y6 <= s_enabled {{when}} s_addr = "110" {{else}} '0';
          Y7 <= s_enabled {{when}} s_addr = "111" {{else}} '0';

          address : {{process}} (nLE, A0, A1, A2) {{is}}
             BEGIN
                {{if}} (nLE = '0') {{then}} s_addr <= A2 & A1 & A0;
                {{end}} {{if}};
             {{end}} {{process}} address;
          """);
    } else {
      contents.add(
          """
          assign s_enabled = ~nE1 & E2;
          assign Y0 = (s_addr == 3'b000) ? s_enabled : 1'b0;
          assign Y1 = (s_addr == 3'b001) ? s_enabled : 1'b0;
          assign Y2 = (s_addr == 3'b010) ? s_enabled : 1'b0;
          assign Y3 = (s_addr == 3'b011) ? s_enabled : 1'b0;
          assign Y4 = (s_addr == 3'b100) ? s_enabled : 1'b0;
          assign Y5 = (s_addr == 3'b101) ? s_enabled : 1'b0;
          assign Y6 = (s_addr == 3'b110) ? s_enabled : 1'b0;
          assign Y7 = (s_addr == 3'b111) ? s_enabled : 1'b0;

          always @(nLE or A0 or A1 or A2)
          begin
             if (nLE == 0) s_addr <= {A2, A1, A0};
          end
          """);
    }
    return contents;
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
