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
 * VHDL and Verilog for one 74HC77. Each pair is a level-sensitive latch. {@code LE12} and {@code
 * LE34} are active-high enables, not clock edges, so they are not declared as clock ports.
 */
public class Ttl7477HdlGenerator extends AbstractHdlGeneratorFactory {
  private static final int BITS = 4;

  /** Creates a generator for the quad transparent latch. */
  public Ttl7477HdlGenerator() {
    super();
    myWires.addRegister("state", BITS);
    myPorts
        .add(Port.INPUT, "D1", 1, Ttl7477.pinNrToPortNr(Ttl7477.D1))
        .add(Port.INPUT, "D2", 1, Ttl7477.pinNrToPortNr(Ttl7477.D2))
        .add(Port.INPUT, "D3", 1, Ttl7477.pinNrToPortNr(Ttl7477.D3))
        .add(Port.INPUT, "D4", 1, Ttl7477.pinNrToPortNr(Ttl7477.D4))
        .add(Port.INPUT, "LE12", 1, Ttl7477.pinNrToPortNr(Ttl7477.LE12))
        .add(Port.INPUT, "LE34", 1, Ttl7477.pinNrToPortNr(Ttl7477.LE34))
        .add(Port.OUTPUT, "Q1", 1, Ttl7477.pinNrToPortNr(Ttl7477.Q1))
        .add(Port.OUTPUT, "Q2", 1, Ttl7477.pinNrToPortNr(Ttl7477.Q2))
        .add(Port.OUTPUT, "Q3", 1, Ttl7477.pinNrToPortNr(Ttl7477.Q3))
        .add(Port.OUTPUT, "Q4", 1, Ttl7477.pinNrToPortNr(Ttl7477.Q4));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      for (var bit = 0; bit < BITS; bit++) {
        contents.add("Q{{1}} <= state({{2}});", bit + 1, bit);
      }
      contents
          .empty()
          .add("latches12 : {{process}}(LE12, D1, D2) {{is}}")
          .add("   {{begin}}")
          .add("      {{if}} (LE12 = '1') {{then}}")
          .add("         state(0) <= D1;")
          .add("         state(1) <= D2;")
          .add("      {{end}} {{if}};")
          .add("   {{end}} {{process}} latches12;")
          .empty()
          .add("latches34 : {{process}}(LE34, D3, D4) {{is}}")
          .add("   {{begin}}")
          .add("      {{if}} (LE34 = '1') {{then}}")
          .add("         state(2) <= D3;")
          .add("         state(3) <= D4;")
          .add("      {{end}} {{if}};")
          .add("   {{end}} {{process}} latches34;");
    } else {
      for (var bit = 0; bit < BITS; bit++) {
        contents.add("assign Q{{1}} = state[{{2}}];", bit + 1, bit);
      }
      contents
          .empty()
          .add("always @(*)")
          .add("begin")
          .add("   if (LE12 == 1) begin")
          .add("      state[0] = D1;")
          .add("      state[1] = D2;")
          .add("   end")
          .add("   if (LE34 == 1) begin")
          .add("      state[2] = D3;")
          .add("      state[3] = D4;")
          .add("   end")
          .add("end");
    }
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
