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
 * VHDL and Verilog for one 74HC543. Each latch is transparent while its chip enable and latch
 * enable are low, so those pins stay ordinary inputs. An edge and a clock tick would drop updates
 * that happen while the latch is already open.
 */
public class Ttl74543HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the octal registered transceiver. */
  public Ttl74543HdlGenerator() {
    super();
    myWires.addRegister("regAB", 8).addRegister("regBA", 8);
    myPorts
        .add(Port.INPUT, "nLEBA", 1, Ttl74543.PORT_INDEX_nLEBA)
        .add(Port.INPUT, "nOEBA", 1, Ttl74543.PORT_INDEX_nOEBA)
        .add(Port.INOUT, "A0", 1, Ttl74543.PORT_INDEX_A0)
        .add(Port.INOUT, "A1", 1, Ttl74543.PORT_INDEX_A1)
        .add(Port.INOUT, "A2", 1, Ttl74543.PORT_INDEX_A2)
        .add(Port.INOUT, "A3", 1, Ttl74543.PORT_INDEX_A3)
        .add(Port.INOUT, "A4", 1, Ttl74543.PORT_INDEX_A4)
        .add(Port.INOUT, "A5", 1, Ttl74543.PORT_INDEX_A5)
        .add(Port.INOUT, "A6", 1, Ttl74543.PORT_INDEX_A6)
        .add(Port.INOUT, "A7", 1, Ttl74543.PORT_INDEX_A7)
        .add(Port.INPUT, "nCEAB", 1, Ttl74543.PORT_INDEX_nCEAB)
        .add(Port.INPUT, "nOEAB", 1, Ttl74543.PORT_INDEX_nOEAB)
        .add(Port.INPUT, "nLEAB", 1, Ttl74543.PORT_INDEX_nLEAB)
        .add(Port.INOUT, "B7", 1, Ttl74543.PORT_INDEX_B7)
        .add(Port.INOUT, "B6", 1, Ttl74543.PORT_INDEX_B6)
        .add(Port.INOUT, "B5", 1, Ttl74543.PORT_INDEX_B5)
        .add(Port.INOUT, "B4", 1, Ttl74543.PORT_INDEX_B4)
        .add(Port.INOUT, "B3", 1, Ttl74543.PORT_INDEX_B3)
        .add(Port.INOUT, "B2", 1, Ttl74543.PORT_INDEX_B2)
        .add(Port.INOUT, "B1", 1, Ttl74543.PORT_INDEX_B1)
        .add(Port.INOUT, "B0", 1, Ttl74543.PORT_INDEX_B0)
        .add(Port.INPUT, "nCEBA", 1, Ttl74543.PORT_INDEX_nCEBA);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          latch_ab : {{process}}(nCEAB, nLEAB, A0, A1, A2, A3, A4, A5, A6, A7) {{is}}
             {{begin}}
                {{if}} (nCEAB = '0' {{and}} nLEAB = '0') {{then}}
                   regAB <= A7&A6&A5&A4&A3&A2&A1&A0;
                {{end}} {{if}};
             {{end}} {{process}} latch_ab;

          latch_ba : {{process}}(nCEBA, nLEBA, B0, B1, B2, B3, B4, B5, B6, B7) {{is}}
             {{begin}}
                {{if}} (nCEBA = '0' {{and}} nLEBA = '0') {{then}}
                   regBA <= B7&B6&B5&B4&B3&B2&B1&B0;
                {{end}} {{if}};
             {{end}} {{process}} latch_ba;
          """);
      addVhdlDrivers(contents);
    } else {
      contents.add("""
          always @(*)
          begin
             if (nCEAB == 0 && nLEAB == 0) regAB <= {A7, A6, A5, A4, A3, A2, A1, A0};
          end

          always @(*)
          begin
             if (nCEBA == 0 && nLEBA == 0) regBA <= {B7, B6, B5, B4, B3, B2, B1, B0};
          end
          """);
      addVerilogDrivers(contents);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }

  private static void addVhdlDrivers(LineBuffer contents) {
    for (var bit = 0; bit < 8; bit++) {
      contents.add(
          String.format(
              "B%d <= regAB(%d) {{when}} nCEAB = '0' {{and}} nOEAB = '0' {{else}} 'Z';",
              bit, bit));
      contents.add(
          String.format(
              "A%d <= regBA(%d) {{when}} nCEBA = '0' {{and}} nOEBA = '0' {{else}} 'Z';",
              bit, bit));
    }
  }

  private static void addVerilogDrivers(LineBuffer contents) {
    for (var bit = 0; bit < 8; bit++) {
      contents.add(
          String.format(
              "assign B%d = (nCEAB == 0 && nOEAB == 0) ? regAB[%d] : 1'bz;", bit, bit));
      contents.add(
          String.format(
              "assign A%d = (nCEBA == 0 && nOEBA == 0) ? regBA[%d] : 1'bz;", bit, bit));
    }
  }
}
