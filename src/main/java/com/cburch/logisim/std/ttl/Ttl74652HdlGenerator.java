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
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/**
 * VHDL and Verilog for one 74HC652. Each bus has its own rising-edge register. OEAB enables the
 * A-to-B drivers and OEBA enables the B-to-A drivers, so both directions can be active together.
 */
public class Ttl74652HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the octal registered transceiver. */
  public Ttl74652HdlGenerator() {
    super();
    myWires
        .addWire("nextA", 8)
        .addWire("nextB", 8)
        .addRegister("regA", 8)
        .addRegister("regB", 8);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74652.PORT_INDEX_CLKAB)
        // The width selects the tick name: 1 is tick, 2 is tick2. See Ttl7474HdlGenerator.
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl74652.PORT_INDEX_CLKBA)
        .add(Port.INPUT, "SAB", 1, Ttl74652.PORT_INDEX_SAB)
        .add(Port.INPUT, "OEAB", 1, Ttl74652.PORT_INDEX_OEAB)
        .add(Port.INPUT, "OEBA", 1, Ttl74652.PORT_INDEX_OEBA)
        .add(Port.INPUT, "SBA", 1, Ttl74652.PORT_INDEX_SBA)
        .add(Port.INOUT, "A1", 1, Ttl74652.PORT_INDEX_A1)
        .add(Port.INOUT, "A2", 1, Ttl74652.PORT_INDEX_A2)
        .add(Port.INOUT, "A3", 1, Ttl74652.PORT_INDEX_A3)
        .add(Port.INOUT, "A4", 1, Ttl74652.PORT_INDEX_A4)
        .add(Port.INOUT, "A5", 1, Ttl74652.PORT_INDEX_A5)
        .add(Port.INOUT, "A6", 1, Ttl74652.PORT_INDEX_A6)
        .add(Port.INOUT, "A7", 1, Ttl74652.PORT_INDEX_A7)
        .add(Port.INOUT, "A8", 1, Ttl74652.PORT_INDEX_A8)
        .add(Port.INOUT, "B1", 1, Ttl74652.PORT_INDEX_B1)
        .add(Port.INOUT, "B2", 1, Ttl74652.PORT_INDEX_B2)
        .add(Port.INOUT, "B3", 1, Ttl74652.PORT_INDEX_B3)
        .add(Port.INOUT, "B4", 1, Ttl74652.PORT_INDEX_B4)
        .add(Port.INOUT, "B5", 1, Ttl74652.PORT_INDEX_B5)
        .add(Port.INOUT, "B6", 1, Ttl74652.PORT_INDEX_B6)
        .add(Port.INOUT, "B7", 1, Ttl74652.PORT_INDEX_B7)
        .add(Port.INOUT, "B8", 1, Ttl74652.PORT_INDEX_B8);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLKAB", HdlPorts.getClockName(1))
            .pair("CLKBA", HdlPorts.getClockName(2))
            .pair("tickAB", HdlPorts.getTickName(1))
            .pair("tickBA", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          nextA <= A8&A7&A6&A5&A4&A3&A2&A1 {{when}} {{tickAB}} = '1' {{else}} regA;
          nextB <= B8&B7&B6&B5&B4&B3&B2&B1 {{when}} {{tickBA}} = '1' {{else}} regB;

          rega : {{process}}({{CLKAB}}) {{is}}
             {{begin}}
                {{if}} (rising_edge({{CLKAB}})) {{then}} regA <= nextA;
                {{end}} {{if}};
             {{end}} {{process}} rega;

          regb : {{process}}({{CLKBA}}) {{is}}
             {{begin}}
                {{if}} (rising_edge({{CLKBA}})) {{then}} regB <= nextB;
                {{end}} {{if}};
             {{end}} {{process}} regb;
          """);
      addVhdlDrivers(contents);
    } else {
      contents.add("""
          assign nextA = ({{tickAB}} == 1) ? {A8, A7, A6, A5, A4, A3, A2, A1} : regA;
          assign nextB = ({{tickBA}} == 1) ? {B8, B7, B6, B5, B4, B3, B2, B1} : regB;

          always @(posedge {{CLKAB}})
          begin
             regA <= nextA;
          end

          always @(posedge {{CLKBA}})
          begin
             regB <= nextB;
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
    for (var bit = 1; bit <= 8; bit++) {
      final var index = bit - 1;
      contents.add(
          String.format(
              "B%d <= A%d {{when}} OEAB = '1' {{and}} SAB = '0' {{else}} regA(%d)"
                  + " {{when}} OEAB = '1' {{and}} SAB = '1' {{else}} 'Z';",
              bit, bit, index));
      contents.add(
          String.format(
              "A%d <= B%d {{when}} OEBA = '0' {{and}} SBA = '0' {{else}} regB(%d)"
                  + " {{when}} OEBA = '0' {{and}} SBA = '1' {{else}} 'Z';",
              bit, bit, index));
    }
  }

  private static void addVerilogDrivers(LineBuffer contents) {
    for (var bit = 1; bit <= 8; bit++) {
      final var index = bit - 1;
      contents.add(
          String.format(
              "assign B%d = (OEAB == 1 && SAB == 0) ? A%d : (OEAB == 1 && SAB == 1) ? regA[%d]"
                  + " : 1'bz;",
              bit, bit, index));
      contents.add(
          String.format(
              "assign A%d = (OEBA == 0 && SBA == 0) ? B%d : (OEBA == 0 && SBA == 1) ? regB[%d]"
                  + " : 1'bz;",
              bit, bit, index));
    }
  }
}
