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
 * VHDL and Verilog for one 74256. Each output is a level-sensitive latch. {@code nE} is an
 * active-low enable, not a clock edge, so it is not declared as a clock port. An open {@code nE}
 * or {@code nCL} is pulled high, so an unconnected device stays in memory mode.
 */
public class Ttl74256HdlGenerator extends AbstractHdlGeneratorFactory {
  private static final int SECTION_BITS = 4;
  private static final int BITS = 8;

  /** Creates a generator for the dual 4-bit addressable latch. */
  public Ttl74256HdlGenerator() {
    super();
    myWires.addRegister("state", BITS);
    // Address and data pull low. The active-low controls pull high.
    myPorts
        .add(Port.INPUT, "A0", 1, Ttl74256.pinNrToPortNr(Ttl74256.A0), true)
        .add(Port.INPUT, "A1", 1, Ttl74256.pinNrToPortNr(Ttl74256.A1), true)
        .add(Port.INPUT, "Da", 1, Ttl74256.pinNrToPortNr(Ttl74256.Da), true)
        .add(Port.INPUT, "Db", 1, Ttl74256.pinNrToPortNr(Ttl74256.Db), true)
        .add(Port.INPUT, "nE", 1, Ttl74256.pinNrToPortNr(Ttl74256.E), false)
        .add(Port.INPUT, "nCL", 1, Ttl74256.pinNrToPortNr(Ttl74256.CL), false)
        .add(Port.OUTPUT, "Q0a", 1, Ttl74256.pinNrToPortNr(Ttl74256.Q0a))
        .add(Port.OUTPUT, "Q1a", 1, Ttl74256.pinNrToPortNr(Ttl74256.Q1a))
        .add(Port.OUTPUT, "Q2a", 1, Ttl74256.pinNrToPortNr(Ttl74256.Q2a))
        .add(Port.OUTPUT, "Q3a", 1, Ttl74256.pinNrToPortNr(Ttl74256.Q3a))
        .add(Port.OUTPUT, "Q0b", 1, Ttl74256.pinNrToPortNr(Ttl74256.Q0b))
        .add(Port.OUTPUT, "Q1b", 1, Ttl74256.pinNrToPortNr(Ttl74256.Q1b))
        .add(Port.OUTPUT, "Q2b", 1, Ttl74256.pinNrToPortNr(Ttl74256.Q2b))
        .add(Port.OUTPUT, "Q3b", 1, Ttl74256.pinNrToPortNr(Ttl74256.Q3b));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      addVhdl(contents);
    } else {
      addVerilog(contents);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }

  private static void addVhdl(LineBuffer contents) {
    final var names = new String[] {"Q0a", "Q1a", "Q2a", "Q3a", "Q0b", "Q1b", "Q2b", "Q3b"};
    for (var bit = 0; bit < BITS; bit++) {
      contents.add("{{1}} <= state({{2}});", names[bit], bit);
    }
    contents
        .empty()
        .add("latches : {{process}}(nCL, nE, Da, Db, A0, A1) {{is}}")
        .add("   {{begin}}")
        .add("      {{if}} (nCL = '0' {{and}} nE = '1') {{then}}")
        .add("         state <= \"00000000\";")
        .add("      {{elsif}} (nCL = '0' {{and}} nE = '0') {{then}}");
    for (var bit = 0; bit < BITS; bit++) {
      contents.add(
          "         {{if}} ({{1}}) {{then}} state({{2}}) <= {{3}}; {{else}} state({{2}}) <= '0'; {{end}} {{if}};",
          vhdlAddress(bit % SECTION_BITS),
          bit,
          dataName(bit));
    }
    contents.add("      {{elsif}} (nCL = '1' {{and}} nE = '0') {{then}}");
    for (var bit = 0; bit < BITS; bit++) {
      contents.add(
          "         {{if}} ({{1}}) {{then}} state({{2}}) <= {{3}}; {{end}} {{if}};",
          vhdlAddress(bit % SECTION_BITS),
          bit,
          dataName(bit));
    }
    contents.add("      {{end}} {{if}};").add("   {{end}} {{process}} latches;");
  }

  private static void addVerilog(LineBuffer contents) {
    final var names = new String[] {"Q0a", "Q1a", "Q2a", "Q3a", "Q0b", "Q1b", "Q2b", "Q3b"};
    for (var bit = 0; bit < BITS; bit++) {
      contents.add("assign {{1}} = state[{{2}}];", names[bit], bit);
    }
    contents
        .empty()
        .add("always @(*)")
        .add("begin")
        .add("   if (nCL == 0 && nE == 1) state = 8'b00000000;")
        .add("   else if (nCL == 0 && nE == 0) begin");
    for (var bit = 0; bit < BITS; bit++) {
      contents.add(
          "      state[{{1}}] = ({{2}}) ? {{3}} : 1'b0;",
          bit,
          verilogAddress(bit % SECTION_BITS),
          dataName(bit));
    }
    contents.add("   end else if (nCL == 1 && nE == 0) begin");
    for (var bit = 0; bit < BITS; bit++) {
      contents.add(
          "      if ({{2}}) state[{{1}}] = {{3}};",
          bit,
          verilogAddress(bit % SECTION_BITS),
          dataName(bit));
    }
    contents.add("   end").add("end");
  }

  private static String dataName(int stateIndex) {
    return stateIndex < SECTION_BITS ? "Da" : "Db";
  }

  private static String vhdlAddress(int index) {
    return "A1 = '" + bitLevel(index, 1) + "' {{and}} A0 = '" + bitLevel(index, 0) + "'";
  }

  private static String verilogAddress(int index) {
    return "A1 == " + bitLevel(index, 1) + " && A0 == " + bitLevel(index, 0);
  }

  private static String bitLevel(int index, int place) {
    return ((index >> place) & 1) == 1 ? "1" : "0";
  }
}
