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

/** VHDL and Verilog for the transparent address latches and active-low decoder of a 74137. */
public class Ttl74137HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74137. Unconnected enables keep the outputs high. */
  public Ttl74137HdlGenerator() {
    super();
    myWires.addWire("s_enabled", 1).addRegister("s_address", 3);
    // Address pins pull low. nLE and nE1 pull high, so an open latch holds and an open nE1
    // disables the outputs. E2 pulls low, which also disables the outputs.
    myPorts
        .add(Port.INPUT, "A0", 1, Ttl74137.pinNrToPortNr(Ttl74137.A0), true)
        .add(Port.INPUT, "A1", 1, Ttl74137.pinNrToPortNr(Ttl74137.A1), true)
        .add(Port.INPUT, "A2", 1, Ttl74137.pinNrToPortNr(Ttl74137.A2), true)
        .add(Port.INPUT, "nLE", 1, Ttl74137.pinNrToPortNr(Ttl74137.LE), false)
        .add(Port.INPUT, "nE1", 1, Ttl74137.pinNrToPortNr(Ttl74137.E1), false)
        .add(Port.INPUT, "E2", 1, Ttl74137.pinNrToPortNr(Ttl74137.E2), true)
        .add(Port.OUTPUT, "nY7", 1, Ttl74137.pinNrToPortNr(Ttl74137.Y7))
        .add(Port.OUTPUT, "nY6", 1, Ttl74137.pinNrToPortNr(Ttl74137.Y6))
        .add(Port.OUTPUT, "nY5", 1, Ttl74137.pinNrToPortNr(Ttl74137.Y5))
        .add(Port.OUTPUT, "nY4", 1, Ttl74137.pinNrToPortNr(Ttl74137.Y4))
        .add(Port.OUTPUT, "nY3", 1, Ttl74137.pinNrToPortNr(Ttl74137.Y3))
        .add(Port.OUTPUT, "nY2", 1, Ttl74137.pinNrToPortNr(Ttl74137.Y2))
        .add(Port.OUTPUT, "nY1", 1, Ttl74137.pinNrToPortNr(Ttl74137.Y1))
        .add(Port.OUTPUT, "nY0", 1, Ttl74137.pinNrToPortNr(Ttl74137.Y0));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          s_enabled <= '1' {{when}} nE1 = '0' {{and}} E2 = '1' {{else}} '0';

          nY0 <= '0' {{when}} s_enabled = '1' {{and}} s_address = "000" {{else}} '1';
          nY1 <= '0' {{when}} s_enabled = '1' {{and}} s_address = "001" {{else}} '1';
          nY2 <= '0' {{when}} s_enabled = '1' {{and}} s_address = "010" {{else}} '1';
          nY3 <= '0' {{when}} s_enabled = '1' {{and}} s_address = "011" {{else}} '1';
          nY4 <= '0' {{when}} s_enabled = '1' {{and}} s_address = "100" {{else}} '1';
          nY5 <= '0' {{when}} s_enabled = '1' {{and}} s_address = "101" {{else}} '1';
          nY6 <= '0' {{when}} s_enabled = '1' {{and}} s_address = "110" {{else}} '1';
          nY7 <= '0' {{when}} s_enabled = '1' {{and}} s_address = "111" {{else}} '1';

          address : {{process}}(nLE, A0, A1, A2) {{is}}
          {{begin}}
             {{if}} (nLE = '0') {{then}}
                s_address <= A2 & A1 & A0;
             {{end}} {{if}};
          {{end}} {{process}} address;
          """);
    } else {
      contents.add("""
          assign s_enabled = (nE1 == 0) && (E2 == 1);

          assign nY0 = (s_enabled == 1 && s_address == 3'b000) ? 1'b0 : 1'b1;
          assign nY1 = (s_enabled == 1 && s_address == 3'b001) ? 1'b0 : 1'b1;
          assign nY2 = (s_enabled == 1 && s_address == 3'b010) ? 1'b0 : 1'b1;
          assign nY3 = (s_enabled == 1 && s_address == 3'b011) ? 1'b0 : 1'b1;
          assign nY4 = (s_enabled == 1 && s_address == 3'b100) ? 1'b0 : 1'b1;
          assign nY5 = (s_enabled == 1 && s_address == 3'b101) ? 1'b0 : 1'b1;
          assign nY6 = (s_enabled == 1 && s_address == 3'b110) ? 1'b0 : 1'b1;
          assign nY7 = (s_enabled == 1 && s_address == 3'b111) ? 1'b0 : 1'b1;

          always @(*)
          begin
             if (nLE == 0) s_address = {A2, A1, A0};
          end
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
