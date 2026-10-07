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
 * VHDL and Verilog for one 74HC544. {@code nLE} stays an ordinary input: the latch is transparent
 * for the whole time {@code nE} and {@code nLE} are low, so an edge and a clock tick would drop
 * updates that happen while the latch is already open.
 */
public class Ttl74544HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the inverting octal registered transceiver. */
  public Ttl74544HdlGenerator() {
    super();
    myWires
        .addRegister("stateAB", 8)
        .addRegister("stateBA", 8);
    myPorts
        .add(Port.INPUT, "nLEBA", 1, Ttl74544.PORT_INDEX_nLEBA)
        .add(Port.INPUT, "nOEBA", 1, Ttl74544.PORT_INDEX_nOEBA)
        .add(Port.INPUT, "nEAB", 1, Ttl74544.PORT_INDEX_nEAB)
        .add(Port.INPUT, "nOEAB", 1, Ttl74544.PORT_INDEX_nOEAB)
        .add(Port.INPUT, "nLEAB", 1, Ttl74544.PORT_INDEX_nLEAB)
        .add(Port.INPUT, "nEBA", 1, Ttl74544.PORT_INDEX_nEBA)
        .add(Port.INOUT, "A0", 1, Ttl74544.PORT_INDEX_A0)
        .add(Port.INOUT, "A1", 1, Ttl74544.PORT_INDEX_A1)
        .add(Port.INOUT, "A2", 1, Ttl74544.PORT_INDEX_A2)
        .add(Port.INOUT, "A3", 1, Ttl74544.PORT_INDEX_A3)
        .add(Port.INOUT, "A4", 1, Ttl74544.PORT_INDEX_A4)
        .add(Port.INOUT, "A5", 1, Ttl74544.PORT_INDEX_A5)
        .add(Port.INOUT, "A6", 1, Ttl74544.PORT_INDEX_A6)
        .add(Port.INOUT, "A7", 1, Ttl74544.PORT_INDEX_A7)
        .add(Port.INOUT, "B0", 1, Ttl74544.PORT_INDEX_B0)
        .add(Port.INOUT, "B1", 1, Ttl74544.PORT_INDEX_B1)
        .add(Port.INOUT, "B2", 1, Ttl74544.PORT_INDEX_B2)
        .add(Port.INOUT, "B3", 1, Ttl74544.PORT_INDEX_B3)
        .add(Port.INOUT, "B4", 1, Ttl74544.PORT_INDEX_B4)
        .add(Port.INOUT, "B5", 1, Ttl74544.PORT_INDEX_B5)
        .add(Port.INOUT, "B6", 1, Ttl74544.PORT_INDEX_B6)
        .add(Port.INOUT, "B7", 1, Ttl74544.PORT_INDEX_B7);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          B0 <= {{not}} stateAB(0) {{when}} nEAB = '0' {{and}} nOEAB = '0' {{else}} 'Z';
          B1 <= {{not}} stateAB(1) {{when}} nEAB = '0' {{and}} nOEAB = '0' {{else}} 'Z';
          B2 <= {{not}} stateAB(2) {{when}} nEAB = '0' {{and}} nOEAB = '0' {{else}} 'Z';
          B3 <= {{not}} stateAB(3) {{when}} nEAB = '0' {{and}} nOEAB = '0' {{else}} 'Z';
          B4 <= {{not}} stateAB(4) {{when}} nEAB = '0' {{and}} nOEAB = '0' {{else}} 'Z';
          B5 <= {{not}} stateAB(5) {{when}} nEAB = '0' {{and}} nOEAB = '0' {{else}} 'Z';
          B6 <= {{not}} stateAB(6) {{when}} nEAB = '0' {{and}} nOEAB = '0' {{else}} 'Z';
          B7 <= {{not}} stateAB(7) {{when}} nEAB = '0' {{and}} nOEAB = '0' {{else}} 'Z';
          A0 <= {{not}} stateBA(0) {{when}} nEBA = '0' {{and}} nOEBA = '0' {{else}} 'Z';
          A1 <= {{not}} stateBA(1) {{when}} nEBA = '0' {{and}} nOEBA = '0' {{else}} 'Z';
          A2 <= {{not}} stateBA(2) {{when}} nEBA = '0' {{and}} nOEBA = '0' {{else}} 'Z';
          A3 <= {{not}} stateBA(3) {{when}} nEBA = '0' {{and}} nOEBA = '0' {{else}} 'Z';
          A4 <= {{not}} stateBA(4) {{when}} nEBA = '0' {{and}} nOEBA = '0' {{else}} 'Z';
          A5 <= {{not}} stateBA(5) {{when}} nEBA = '0' {{and}} nOEBA = '0' {{else}} 'Z';
          A6 <= {{not}} stateBA(6) {{when}} nEBA = '0' {{and}} nOEBA = '0' {{else}} 'Z';
          A7 <= {{not}} stateBA(7) {{when}} nEBA = '0' {{and}} nOEBA = '0' {{else}} 'Z';

          latchAB : {{process}}(nEAB, nLEAB, nEBA, nOEBA, A0, A1, A2, A3, A4, A5, A6, A7) {{is}}
             {{begin}}
                {{if}} (nEAB = '0' {{and}} nLEAB = '0' {{and}} {{not}}(nEBA = '0' {{and}} nOEBA = '0')) {{then}}
                   stateAB <= {{not}}(A7&A6&A5&A4&A3&A2&A1&A0);
                {{end}} {{if}};
             {{end}} {{process}} latchAB;

          latchBA : {{process}}(nEBA, nLEBA, nEAB, nOEAB, B0, B1, B2, B3, B4, B5, B6, B7) {{is}}
             {{begin}}
                {{if}} (nEBA = '0' {{and}} nLEBA = '0' {{and}} {{not}}(nEAB = '0' {{and}} nOEAB = '0')) {{then}}
                   stateBA <= {{not}}(B7&B6&B5&B4&B3&B2&B1&B0);
                {{end}} {{if}};
             {{end}} {{process}} latchBA;
          """);
    } else {
      contents.add("""
          assign B0 = (nEAB == 0 && nOEAB == 0) ? ~stateAB[0] : 1'bz;
          assign B1 = (nEAB == 0 && nOEAB == 0) ? ~stateAB[1] : 1'bz;
          assign B2 = (nEAB == 0 && nOEAB == 0) ? ~stateAB[2] : 1'bz;
          assign B3 = (nEAB == 0 && nOEAB == 0) ? ~stateAB[3] : 1'bz;
          assign B4 = (nEAB == 0 && nOEAB == 0) ? ~stateAB[4] : 1'bz;
          assign B5 = (nEAB == 0 && nOEAB == 0) ? ~stateAB[5] : 1'bz;
          assign B6 = (nEAB == 0 && nOEAB == 0) ? ~stateAB[6] : 1'bz;
          assign B7 = (nEAB == 0 && nOEAB == 0) ? ~stateAB[7] : 1'bz;
          assign A0 = (nEBA == 0 && nOEBA == 0) ? ~stateBA[0] : 1'bz;
          assign A1 = (nEBA == 0 && nOEBA == 0) ? ~stateBA[1] : 1'bz;
          assign A2 = (nEBA == 0 && nOEBA == 0) ? ~stateBA[2] : 1'bz;
          assign A3 = (nEBA == 0 && nOEBA == 0) ? ~stateBA[3] : 1'bz;
          assign A4 = (nEBA == 0 && nOEBA == 0) ? ~stateBA[4] : 1'bz;
          assign A5 = (nEBA == 0 && nOEBA == 0) ? ~stateBA[5] : 1'bz;
          assign A6 = (nEBA == 0 && nOEBA == 0) ? ~stateBA[6] : 1'bz;
          assign A7 = (nEBA == 0 && nOEBA == 0) ? ~stateBA[7] : 1'bz;

          always @(*)
          begin
             if (nEAB == 0 && nLEAB == 0 && !(nEBA == 0 && nOEBA == 0))
                stateAB <= ~{A7, A6, A5, A4, A3, A2, A1, A0};
          end

          always @(*)
          begin
             if (nEBA == 0 && nLEBA == 0 && !(nEAB == 0 && nOEAB == 0))
                stateBA <= ~{B7, B6, B5, B4, B3, B2, B1, B0};
          end
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
