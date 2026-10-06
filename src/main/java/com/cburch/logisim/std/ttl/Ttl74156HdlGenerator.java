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
 * VHDL and Verilog generator for the 74x156 decoder.
 *
 * <p>The generated outputs are strong 0 and 1, matching the open-collector function after a
 * pull-up. High-impedance is not emitted.
 */
public class Ttl74156HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose unconnected enables leave both sections off. */
  public Ttl74156HdlGenerator() {
    super();
    // Active-low enables pull high so an unconnected section stays off.
    // 1C is active high, so it pulls low and also leaves section 1 off.
    myPorts
        .add(Port.INPUT, "C1", 1, 0, true)
        .add(Port.INPUT, "n1G", 1, 1, false)
        .add(Port.INPUT, "B", 1, 2, true)
        .add(Port.OUTPUT, "n1Y3", 1, 3)
        .add(Port.OUTPUT, "n1Y2", 1, 4)
        .add(Port.OUTPUT, "n1Y1", 1, 5)
        .add(Port.OUTPUT, "n1Y0", 1, 6)
        .add(Port.OUTPUT, "n2Y0", 1, 7)
        .add(Port.OUTPUT, "n2Y1", 1, 8)
        .add(Port.OUTPUT, "n2Y2", 1, 9)
        .add(Port.OUTPUT, "n2Y3", 1, 10)
        .add(Port.INPUT, "A", 1, 11, true)
        .add(Port.INPUT, "n2G", 1, 12, false)
        .add(Port.INPUT, "n2C", 1, 13, false);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          n1Y0 <= '0' {{when}} n1G = '0' {{and}} C1 = '1' {{and}} B = '0' {{and}} A = '0' {{else}} '1';
          n1Y1 <= '0' {{when}} n1G = '0' {{and}} C1 = '1' {{and}} B = '0' {{and}} A = '1' {{else}} '1';
          n1Y2 <= '0' {{when}} n1G = '0' {{and}} C1 = '1' {{and}} B = '1' {{and}} A = '0' {{else}} '1';
          n1Y3 <= '0' {{when}} n1G = '0' {{and}} C1 = '1' {{and}} B = '1' {{and}} A = '1' {{else}} '1';
          n2Y0 <= '0' {{when}} n2G = '0' {{and}} n2C = '0' {{and}} B = '0' {{and}} A = '0' {{else}} '1';
          n2Y1 <= '0' {{when}} n2G = '0' {{and}} n2C = '0' {{and}} B = '0' {{and}} A = '1' {{else}} '1';
          n2Y2 <= '0' {{when}} n2G = '0' {{and}} n2C = '0' {{and}} B = '1' {{and}} A = '0' {{else}} '1';
          n2Y3 <= '0' {{when}} n2G = '0' {{and}} n2C = '0' {{and}} B = '1' {{and}} A = '1' {{else}} '1';
          """);
    } else {
      contents.add(
          """
          assign n1Y0 = (n1G == 0 && C1 == 1 && B == 0 && A == 0) ? 1'b0 : 1'b1;
          assign n1Y1 = (n1G == 0 && C1 == 1 && B == 0 && A == 1) ? 1'b0 : 1'b1;
          assign n1Y2 = (n1G == 0 && C1 == 1 && B == 1 && A == 0) ? 1'b0 : 1'b1;
          assign n1Y3 = (n1G == 0 && C1 == 1 && B == 1 && A == 1) ? 1'b0 : 1'b1;
          assign n2Y0 = (n2G == 0 && n2C == 0 && B == 0 && A == 0) ? 1'b0 : 1'b1;
          assign n2Y1 = (n2G == 0 && n2C == 0 && B == 0 && A == 1) ? 1'b0 : 1'b1;
          assign n2Y2 = (n2G == 0 && n2C == 0 && B == 1 && A == 0) ? 1'b0 : 1'b1;
          assign n2Y3 = (n2G == 0 && n2C == 0 && B == 1 && A == 1) ? 1'b0 : 1'b1;
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
