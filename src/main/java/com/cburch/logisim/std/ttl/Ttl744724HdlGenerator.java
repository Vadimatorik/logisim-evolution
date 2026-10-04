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
 * VHDL and Verilog for one 74HC4724. Each output is a level-sensitive latch. {@code E} is an
 * active-low enable, not a clock edge, so it is not declared as a clock port. {@code CL} is an
 * active-high clear.
 */
public class Ttl744724HdlGenerator extends AbstractHdlGeneratorFactory {
  private static final int BITS = 8;

  /** Creates a generator for the 8-bit addressable latch. */
  public Ttl744724HdlGenerator() {
    super();
    myWires.addRegister("state", BITS);
    myPorts
        .add(Port.INPUT, "A0", 1, Ttl744724.pinNrToPortNr(Ttl744724.A0))
        .add(Port.INPUT, "A1", 1, Ttl744724.pinNrToPortNr(Ttl744724.A1))
        .add(Port.INPUT, "A2", 1, Ttl744724.pinNrToPortNr(Ttl744724.A2))
        .add(Port.INPUT, "D", 1, Ttl744724.pinNrToPortNr(Ttl744724.D))
        .add(Port.INPUT, "E", 1, Ttl744724.pinNrToPortNr(Ttl744724.E))
        .add(Port.INPUT, "CL", 1, Ttl744724.pinNrToPortNr(Ttl744724.CL))
        .add(Port.OUTPUT, "Q0", 1, Ttl744724.pinNrToPortNr(Ttl744724.Q0))
        .add(Port.OUTPUT, "Q1", 1, Ttl744724.pinNrToPortNr(Ttl744724.Q1))
        .add(Port.OUTPUT, "Q2", 1, Ttl744724.pinNrToPortNr(Ttl744724.Q2))
        .add(Port.OUTPUT, "Q3", 1, Ttl744724.pinNrToPortNr(Ttl744724.Q3))
        .add(Port.OUTPUT, "Q4", 1, Ttl744724.pinNrToPortNr(Ttl744724.Q4))
        .add(Port.OUTPUT, "Q5", 1, Ttl744724.pinNrToPortNr(Ttl744724.Q5))
        .add(Port.OUTPUT, "Q6", 1, Ttl744724.pinNrToPortNr(Ttl744724.Q6))
        .add(Port.OUTPUT, "Q7", 1, Ttl744724.pinNrToPortNr(Ttl744724.Q7));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      for (var bit = 0; bit < BITS; bit++) {
        contents.add("Q{{1}} <= state({{1}});", bit);
      }
      contents
          .empty()
          .add("latches : {{process}}(CL, E, D, A0, A1, A2) {{is}}")
          .add("   {{begin}}")
          .add("      {{if}} (CL = '1' {{and}} E = '1') {{then}}")
          .add("         state <= \"00000000\";")
          .add("      {{elsif}} (CL = '1' {{and}} E = '0') {{then}}");
      for (var bit = 0; bit < BITS; bit++) {
        contents.add(
            "         {{if}} ({{1}}) {{then}} state({{2}}) <= D; {{else}} state({{2}}) <= '0'; {{end}} {{if}};",
            vhdlAddress(bit),
            bit);
      }
      contents.add("      {{elsif}} (CL = '0' {{and}} E = '0') {{then}}");
      for (var bit = 0; bit < BITS; bit++) {
        contents.add(
            "         {{if}} ({{1}}) {{then}} state({{2}}) <= D; {{end}} {{if}};",
            vhdlAddress(bit),
            bit);
      }
      contents.add("      {{end}} {{if}};").add("   {{end}} {{process}} latches;");
    } else {
      for (var bit = 0; bit < BITS; bit++) {
        contents.add("assign Q{{1}} = state[{{1}}];", bit);
      }
      contents
          .empty()
          .add("always @(*)")
          .add("begin")
          .add("   if (CL == 1 && E == 1) state = 8'b00000000;")
          .add("   else if (CL == 1 && E == 0) begin");
      for (var bit = 0; bit < BITS; bit++) {
        contents.add("      state[{{1}}] = ({{2}}) ? D : 1'b0;", bit, verilogAddress(bit));
      }
      contents.add("   end else if (CL == 0 && E == 0) begin");
      for (var bit = 0; bit < BITS; bit++) {
        contents.add("      if ({{2}}) state[{{1}}] = D;", bit, verilogAddress(bit));
      }
      contents.add("   end").add("end");
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }

  private static String vhdlAddress(int index) {
    return "A2 = '"
        + bitLevel(index, 2)
        + "' {{and}} A1 = '"
        + bitLevel(index, 1)
        + "' {{and}} A0 = '"
        + bitLevel(index, 0)
        + "'";
  }

  private static String verilogAddress(int index) {
    return "A2 == "
        + bitLevel(index, 2)
        + " && A1 == "
        + bitLevel(index, 1)
        + " && A0 == "
        + bitLevel(index, 0);
  }

  private static String bitLevel(int index, int place) {
    return ((index >> place) & 1) == 1 ? "1" : "0";
  }
}
