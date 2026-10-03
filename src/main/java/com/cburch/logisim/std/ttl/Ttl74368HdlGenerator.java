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

/** VHDL and Verilog generator for the 74x368 hex inverting buffer/line driver. */
public class Ttl74368HdlGenerator extends AbstractHdlGeneratorFactory {

  /**
   * Creates an HDL generator. Unconnected active-low enables are pulled high, so an unwired device
   * stays in high-impedance. Unconnected data inputs are pulled low.
   */
  public Ttl74368HdlGenerator() {
    super();
    myWires
        .addWire("s_group1", 1)
        .addWire("s_group2", 1);
    myPorts
        .add(Port.INPUT, "n1OE", 1, Ttl74368.pinNrToPortNr(Ttl74368.OE1), false)
        .add(Port.INPUT, "A1", 1, Ttl74368.pinNrToPortNr(Ttl74368.A1), true)
        .add(Port.OUTPUT, "Y1", 1, Ttl74368.pinNrToPortNr(Ttl74368.Y1))
        .add(Port.INPUT, "A2", 1, Ttl74368.pinNrToPortNr(Ttl74368.A2), true)
        .add(Port.OUTPUT, "Y2", 1, Ttl74368.pinNrToPortNr(Ttl74368.Y2))
        .add(Port.INPUT, "A3", 1, Ttl74368.pinNrToPortNr(Ttl74368.A3), true)
        .add(Port.OUTPUT, "Y3", 1, Ttl74368.pinNrToPortNr(Ttl74368.Y3))
        .add(Port.OUTPUT, "Y4", 1, Ttl74368.pinNrToPortNr(Ttl74368.Y4))
        .add(Port.INPUT, "A4", 1, Ttl74368.pinNrToPortNr(Ttl74368.A4), true)
        .add(Port.OUTPUT, "Y5", 1, Ttl74368.pinNrToPortNr(Ttl74368.Y5))
        .add(Port.INPUT, "A5", 1, Ttl74368.pinNrToPortNr(Ttl74368.A5), true)
        .add(Port.OUTPUT, "Y6", 1, Ttl74368.pinNrToPortNr(Ttl74368.Y6))
        .add(Port.INPUT, "A6", 1, Ttl74368.pinNrToPortNr(Ttl74368.A6), true)
        .add(Port.INPUT, "n2OE", 1, Ttl74368.pinNrToPortNr(Ttl74368.OE2), false);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          s_group1 <= {{not}} n1OE;
          s_group2 <= {{not}} n2OE;
          Y1 <= ({{not}} A1) {{when}} s_group1 = '1' {{else}} 'Z';
          Y2 <= ({{not}} A2) {{when}} s_group1 = '1' {{else}} 'Z';
          Y3 <= ({{not}} A3) {{when}} s_group1 = '1' {{else}} 'Z';
          Y4 <= ({{not}} A4) {{when}} s_group1 = '1' {{else}} 'Z';
          Y5 <= ({{not}} A5) {{when}} s_group2 = '1' {{else}} 'Z';
          Y6 <= ({{not}} A6) {{when}} s_group2 = '1' {{else}} 'Z';
          """);
    } else {
      contents.add(
          """
          assign s_group1 = ~n1OE;
          assign s_group2 = ~n2OE;
          assign Y1 = s_group1 ? ~A1 : 1'bZ;
          assign Y2 = s_group1 ? ~A2 : 1'bZ;
          assign Y3 = s_group1 ? ~A3 : 1'bZ;
          assign Y4 = s_group1 ? ~A4 : 1'bZ;
          assign Y5 = s_group2 ? ~A5 : 1'bZ;
          assign Y6 = s_group2 ? ~A6 : 1'bZ;
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
