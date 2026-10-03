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
import com.cburch.logisim.fpga.hdlgenerator.WithSelectHdlGenerator;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/**
 * VHDL and Verilog for one 74HC4511. The BCD latch is level-sensitive: {@code LE} low is
 * transparent and {@code LE} high holds, so {@code LE} is not a clock port. {@code LT} and {@code
 * BI} change only the segment outputs.
 */
public class Ttl744511HdlGenerator extends AbstractHdlGeneratorFactory {
  /** Glyph bit 0 is segment a and bit 6 is segment g. */
  private static final String[] GLYPH = {
    "0111111",
    "0000110",
    "1011011",
    "1001111",
    "1100110",
    "1101101",
    "1111100",
    "0000111",
    "1111111",
    "1100111"
  };

  /** Creates a generator for the BCD to 7-segment latch/decoder/driver. */
  public Ttl744511HdlGenerator() {
    super();
    myWires
        .addRegister("stored", 4)
        .addWire("segments", 7)
        .addWire("shown", 7);
    myPorts
        .add(Port.INPUT, "B", 1, Ttl744511.pinNrToPortNr(Ttl744511.B))
        .add(Port.INPUT, "C", 1, Ttl744511.pinNrToPortNr(Ttl744511.C))
        .add(Port.INPUT, "LT", 1, Ttl744511.pinNrToPortNr(Ttl744511.LT), false)
        .add(Port.INPUT, "BI", 1, Ttl744511.pinNrToPortNr(Ttl744511.BI), false)
        .add(Port.INPUT, "LE", 1, Ttl744511.pinNrToPortNr(Ttl744511.LE), false)
        .add(Port.INPUT, "D", 1, Ttl744511.pinNrToPortNr(Ttl744511.D))
        .add(Port.INPUT, "A", 1, Ttl744511.pinNrToPortNr(Ttl744511.A))
        .add(Port.OUTPUT, "Sege", 1, Ttl744511.pinNrToPortNr(Ttl744511.SEGE))
        .add(Port.OUTPUT, "Segd", 1, Ttl744511.pinNrToPortNr(Ttl744511.SEGD))
        .add(Port.OUTPUT, "Segc", 1, Ttl744511.pinNrToPortNr(Ttl744511.SEGC))
        .add(Port.OUTPUT, "Segb", 1, Ttl744511.pinNrToPortNr(Ttl744511.SEGB))
        .add(Port.OUTPUT, "Sega", 1, Ttl744511.pinNrToPortNr(Ttl744511.SEGA))
        .add(Port.OUTPUT, "Segg", 1, Ttl744511.pinNrToPortNr(Ttl744511.SEGG))
        .add(Port.OUTPUT, "Segf", 1, Ttl744511.pinNrToPortNr(Ttl744511.SEGF));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var decoder = new WithSelectHdlGenerator("decoder1", "stored", 4, "segments", 7)
        .setDefault("0000000");
    for (var code = 0; code < GLYPH.length; code++) {
      decoder.add((long) code, GLYPH[code]);
    }
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          latches : {{process}}(LE, D, C, B, A) {{is}}
             {{begin}}
                {{if}} (LE = '0') {{then}}
                   stored <= D&C&B&A;
                {{end}} {{if}};
             {{end}} {{process}} latches;
          """);
    } else {
      contents.add("""
          always @(*)
          begin
             if (LE == 0) stored = {D, C, B, A};
          end
          """);
    }
    contents.empty().add(decoder.getHdlCode()).empty();
    if (Hdl.isVhdl()) {
      contents.add("""
          Sega <= shown(0);
          Segb <= shown(1);
          Segc <= shown(2);
          Segd <= shown(3);
          Sege <= shown(4);
          Segf <= shown(5);
          Segg <= shown(6);

          shown <= ({{others}} => '1') {{when}} LT = '0' {{else}}
                   ({{others}} => '0') {{when}} BI = '0' {{else}}
                   segments;
          """);
    } else {
      contents.add("""
          assign Sega = shown[0];
          assign Segb = shown[1];
          assign Segc = shown[2];
          assign Segd = shown[3];
          assign Sege = shown[4];
          assign Segf = shown[5];
          assign Segg = shown[6];

          assign shown = (LT == 0) ? 7'b1111111 : (BI == 0) ? 7'b0000000 : segments;
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
