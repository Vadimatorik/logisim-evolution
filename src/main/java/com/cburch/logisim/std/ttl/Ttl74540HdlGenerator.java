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

/** VHDL and Verilog for the eight inverting three-state buffers of a 74540. */
public class Ttl74540HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose unconnected output enables are pulled high. */
  public Ttl74540HdlGenerator() {
    super();
    // Both enables are active low, so an unconnected enable leaves the outputs released.
    myPorts
        .add(Port.INPUT, "nOE1", 1, Ttl74540.pinNrToPortNr(Ttl74540.OE1), false)
        .add(Port.INPUT, "nOE2", 1, Ttl74540.pinNrToPortNr(Ttl74540.OE2), false)
        .add(Port.INPUT, "A1", 1, Ttl74540.pinNrToPortNr(Ttl74540.A1))
        .add(Port.INPUT, "A2", 1, Ttl74540.pinNrToPortNr(Ttl74540.A2))
        .add(Port.INPUT, "A3", 1, Ttl74540.pinNrToPortNr(Ttl74540.A3))
        .add(Port.INPUT, "A4", 1, Ttl74540.pinNrToPortNr(Ttl74540.A4))
        .add(Port.INPUT, "A5", 1, Ttl74540.pinNrToPortNr(Ttl74540.A5))
        .add(Port.INPUT, "A6", 1, Ttl74540.pinNrToPortNr(Ttl74540.A6))
        .add(Port.INPUT, "A7", 1, Ttl74540.pinNrToPortNr(Ttl74540.A7))
        .add(Port.INPUT, "A8", 1, Ttl74540.pinNrToPortNr(Ttl74540.A8))
        .add(Port.OUTPUT, "Y1", 1, Ttl74540.pinNrToPortNr(Ttl74540.Y1))
        .add(Port.OUTPUT, "Y2", 1, Ttl74540.pinNrToPortNr(Ttl74540.Y2))
        .add(Port.OUTPUT, "Y3", 1, Ttl74540.pinNrToPortNr(Ttl74540.Y3))
        .add(Port.OUTPUT, "Y4", 1, Ttl74540.pinNrToPortNr(Ttl74540.Y4))
        .add(Port.OUTPUT, "Y5", 1, Ttl74540.pinNrToPortNr(Ttl74540.Y5))
        .add(Port.OUTPUT, "Y6", 1, Ttl74540.pinNrToPortNr(Ttl74540.Y6))
        .add(Port.OUTPUT, "Y7", 1, Ttl74540.pinNrToPortNr(Ttl74540.Y7))
        .add(Port.OUTPUT, "Y8", 1, Ttl74540.pinNrToPortNr(Ttl74540.Y8));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      for (var channel = 1; channel <= 8; channel++) {
        contents.add(
            "Y{{1}} <= {{not}} A{{1}} {{when}} nOE1 = '0' {{and}} nOE2 = '0' {{else}} 'Z';",
            channel);
      }
    } else {
      for (var channel = 1; channel <= 8; channel++) {
        contents.add(
            "assign Y{{1}} = (nOE1 == 0 && nOE2 == 0) ? ~A{{1}} : 1'bZ;",
            channel);
      }
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
