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
 * VHDL and Verilog for one 74HC279. Each output is a level-sensitive latch. Set is active low and
 * takes priority over reset, so there is no clock port.
 *
 * <p>The simulator reports unknown when both active-low inputs are released in the same step. This
 * HDL keeps the high level stored while both inputs were low, which is the level a latch description
 * holds across that transition.
 */
public class Ttl74279HdlGenerator extends AbstractHdlGeneratorFactory {
  private static final String[] VHDL_SET = {
    "S1A = '0' or S1B = '0'",
    "S2 = '0'",
    "S3A = '0' or S3B = '0'",
    "S4 = '0'"
  };
  private static final String[] VHDL_RESET = {"R1", "R2", "R3", "R4"};
  private static final String[] VERILOG_SET = {
    "S1A == 0 || S1B == 0",
    "S2 == 0",
    "S3A == 0 || S3B == 0",
    "S4 == 0"
  };
  private static final String[] VERILOG_RESET = {"R1", "R2", "R3", "R4"};
  private static final String[] OUTPUTS = {"Q1", "Q2", "Q3", "Q4"};

  /** Creates a generator for the quad S-R latch. */
  public Ttl74279HdlGenerator() {
    super();
    myWires.addRegister("state", OUTPUTS.length);
    myPorts
        .add(Port.INPUT, "R1", 1, Ttl74279.pinNrToPortNr(Ttl74279.R1), false)
        .add(Port.INPUT, "S1A", 1, Ttl74279.pinNrToPortNr(Ttl74279.S1A), false)
        .add(Port.INPUT, "S1B", 1, Ttl74279.pinNrToPortNr(Ttl74279.S1B), false)
        .add(Port.INPUT, "R2", 1, Ttl74279.pinNrToPortNr(Ttl74279.R2), false)
        .add(Port.INPUT, "S2", 1, Ttl74279.pinNrToPortNr(Ttl74279.S2), false)
        .add(Port.INPUT, "R3", 1, Ttl74279.pinNrToPortNr(Ttl74279.R3), false)
        .add(Port.INPUT, "S3A", 1, Ttl74279.pinNrToPortNr(Ttl74279.S3A), false)
        .add(Port.INPUT, "S3B", 1, Ttl74279.pinNrToPortNr(Ttl74279.S3B), false)
        .add(Port.INPUT, "R4", 1, Ttl74279.pinNrToPortNr(Ttl74279.R4), false)
        .add(Port.INPUT, "S4", 1, Ttl74279.pinNrToPortNr(Ttl74279.S4), false)
        .add(Port.OUTPUT, "Q1", 1, Ttl74279.pinNrToPortNr(Ttl74279.Q1))
        .add(Port.OUTPUT, "Q2", 1, Ttl74279.pinNrToPortNr(Ttl74279.Q2))
        .add(Port.OUTPUT, "Q3", 1, Ttl74279.pinNrToPortNr(Ttl74279.Q3))
        .add(Port.OUTPUT, "Q4", 1, Ttl74279.pinNrToPortNr(Ttl74279.Q4));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      for (var index = 0; index < OUTPUTS.length; index++) {
        contents.add("{{1}} <= state({{2}});", OUTPUTS[index], index);
      }
      contents
          .empty()
          .add("latches : {{process}}(R1, S1A, S1B, R2, S2, R3, S3A, S3B, R4, S4) {{is}}")
          .add("   {{begin}}");
      for (var index = 0; index < OUTPUTS.length; index++) {
        contents.add(
            "      {{if}} ({{1}}) {{then}} state({{2}}) <= '1';", VHDL_SET[index], index);
        contents.add(
            "      {{elsif}} ({{1}} = '0') {{then}} state({{2}}) <= '0';",
            VHDL_RESET[index],
            index);
        contents.add("      {{end}} {{if}};");
      }
      contents.add("   {{end}} {{process}} latches;");
    } else {
      for (var index = 0; index < OUTPUTS.length; index++) {
        contents.add("assign {{1}} = state[{{2}}];", OUTPUTS[index], index);
      }
      contents.empty().add("always @(*)").add("begin");
      for (var index = 0; index < OUTPUTS.length; index++) {
        contents.add("   if ({{1}}) state[{{2}}] = 1'b1;", VERILOG_SET[index], index);
        contents.add(
            "   else if ({{1}} == 0) state[{{2}}] = 1'b0;", VERILOG_RESET[index], index);
      }
      contents.add("end");
    }
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
