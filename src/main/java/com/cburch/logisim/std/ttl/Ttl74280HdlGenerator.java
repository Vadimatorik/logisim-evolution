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
 * VHDL and Verilog generator for the 74x280 parity generator/checker. Unconnected inputs are
 * pulled low, so an open input contributes nothing to the parity.
 */
public class Ttl74280HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 9-bit parity tree. */
  public Ttl74280HdlGenerator() {
    super();
    myWires.addWire("s_odd", 1);
    myPorts
        .add(Port.INPUT, "I0", 1, Ttl74280.pinNrToPortNr(Ttl74280.I0))
        .add(Port.INPUT, "I1", 1, Ttl74280.pinNrToPortNr(Ttl74280.I1))
        .add(Port.INPUT, "I2", 1, Ttl74280.pinNrToPortNr(Ttl74280.I2))
        .add(Port.INPUT, "I3", 1, Ttl74280.pinNrToPortNr(Ttl74280.I3))
        .add(Port.INPUT, "I4", 1, Ttl74280.pinNrToPortNr(Ttl74280.I4))
        .add(Port.INPUT, "I5", 1, Ttl74280.pinNrToPortNr(Ttl74280.I5))
        .add(Port.INPUT, "I6", 1, Ttl74280.pinNrToPortNr(Ttl74280.I6))
        .add(Port.INPUT, "I7", 1, Ttl74280.pinNrToPortNr(Ttl74280.I7))
        .add(Port.INPUT, "I8", 1, Ttl74280.pinNrToPortNr(Ttl74280.I8))
        .add(Port.OUTPUT, "PE", 1, Ttl74280.pinNrToPortNr(Ttl74280.PE))
        .add(Port.OUTPUT, "PO", 1, Ttl74280.pinNrToPortNr(Ttl74280.PO));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          s_odd <= I0 {{xor}} I1 {{xor}} I2 {{xor}} I3 {{xor}} I4 {{xor}} I5 {{xor}} I6 {{xor}} I7 {{xor}} I8;
          PO    <= s_odd;
          PE    <= {{not}} s_odd;
          """);
    } else {
      contents.add(
          """
          assign s_odd = I0 ^ I1 ^ I2 ^ I3 ^ I4 ^ I5 ^ I6 ^ I7 ^ I8;
          assign PO    = s_odd;
          assign PE    = ~s_odd;
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
