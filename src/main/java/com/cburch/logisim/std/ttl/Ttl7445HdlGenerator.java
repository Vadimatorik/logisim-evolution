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
 * VHDL and Verilog generator for the 74x45 BCD-to-decimal decoder/driver.
 *
 * <p>The equations are the BCD decoding of the 7442. A product term matches only one code from 0
 * to 9, so codes 10 to 15 drive every output high. HDL has no open-collector value, so the
 * released state is logic 1, as in the data-sheet function table.
 */
public class Ttl7445HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the active-low decimal outputs. */
  public Ttl7445HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "A", 1, 13)
        .add(Port.INPUT, "B", 1, 12)
        .add(Port.INPUT, "C", 1, 11)
        .add(Port.INPUT, "D", 1, 10)
        .add(Port.OUTPUT, "O0", 1, 0)
        .add(Port.OUTPUT, "O1", 1, 1)
        .add(Port.OUTPUT, "O2", 1, 2)
        .add(Port.OUTPUT, "O3", 1, 3)
        .add(Port.OUTPUT, "O4", 1, 4)
        .add(Port.OUTPUT, "O5", 1, 5)
        .add(Port.OUTPUT, "O6", 1, 6)
        .add(Port.OUTPUT, "O7", 1, 7)
        .add(Port.OUTPUT, "O8", 1, 8)
        .add(Port.OUTPUT, "O9", 1, 9);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.add(
          """
          O0 <= NOT (NOT D AND NOT C AND NOT B AND NOT A);
          O1 <= NOT (NOT D AND NOT C AND NOT B AND A);
          O2 <= NOT (NOT D AND NOT C AND B AND NOT A);
          O3 <= NOT (NOT D AND NOT C AND B AND A);
          O4 <= NOT (NOT D AND C AND NOT B AND NOT A);
          O5 <= NOT (NOT D AND C AND NOT B AND A);
          O6 <= NOT (NOT D AND C AND B AND NOT A);
          O7 <= NOT (NOT D AND C AND B AND A);
          O8 <= NOT (D AND NOT C AND NOT B AND NOT A);
          O9 <= NOT (D AND NOT C AND NOT B AND A);
          """);
    } else {
      contents.add(
          """
          assign O0 = ~( ~D & ~C & ~B & ~A );
          assign O1 = ~( ~D & ~C & ~B & A );
          assign O2 = ~( ~D & ~C & B & ~A );
          assign O3 = ~( ~D & ~C & B & A );
          assign O4 = ~( ~D & C & ~B & ~A );
          assign O5 = ~( ~D & C & ~B & A );
          assign O6 = ~( ~D & C & B & ~A );
          assign O7 = ~( ~D & C & B & A );
          assign O8 = ~( D & ~C & ~B & ~A );
          assign O9 = ~( D & ~C & ~B & A );
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
