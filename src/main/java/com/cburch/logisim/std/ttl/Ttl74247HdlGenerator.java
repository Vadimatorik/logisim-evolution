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
 * VHDL and Verilog for one 74x247. Segment bit 0 is {@code a} and a 1 means the segment is lit
 * before the active-low inversion. {@code BI/RBO} is a blanking input here: the ripple-blanking
 * output shares that pin, and the HDL generators have no bidirectional port.
 */
public class Ttl74247HdlGenerator extends AbstractHdlGeneratorFactory {
  /** Lit-segment patterns for codes 0 through 14. Code 15 is the blank default. */
  private static final String[] GLYPH = {
    "0111111",
    "0000110",
    "1011011",
    "1001111",
    "1100110",
    "1101101",
    "1111101",
    "0000111",
    "1111111",
    "1101111",
    "1011000",
    "1001100",
    "1100010",
    "1101001",
    "1111000"
  };

  /** Creates a generator for the BCD to 7-segment decoder/driver. */
  public Ttl74247HdlGenerator() {
    super();
    myWires
        .addWire("segments", 7)
        .addWire("realSegments", 7)
        .addWire("bcd", 4);
    myPorts
        .add(Port.INPUT, "B", 1, Ttl74247.pinNrToPortNr(Ttl74247.B))
        .add(Port.INPUT, "C", 1, Ttl74247.pinNrToPortNr(Ttl74247.C))
        .add(Port.INPUT, "LT", 1, Ttl74247.pinNrToPortNr(Ttl74247.LT), false)
        .add(Port.INPUT, "BI", 1, Ttl74247.pinNrToPortNr(Ttl74247.BI_RBO), false)
        .add(Port.INPUT, "RBI", 1, Ttl74247.pinNrToPortNr(Ttl74247.RBI), false)
        .add(Port.INPUT, "D", 1, Ttl74247.pinNrToPortNr(Ttl74247.D))
        .add(Port.INPUT, "A", 1, Ttl74247.pinNrToPortNr(Ttl74247.A))
        .add(Port.OUTPUT, "Sege", 1, Ttl74247.pinNrToPortNr(Ttl74247.SEGE))
        .add(Port.OUTPUT, "Segd", 1, Ttl74247.pinNrToPortNr(Ttl74247.SEGD))
        .add(Port.OUTPUT, "Segc", 1, Ttl74247.pinNrToPortNr(Ttl74247.SEGC))
        .add(Port.OUTPUT, "Segb", 1, Ttl74247.pinNrToPortNr(Ttl74247.SEGB))
        .add(Port.OUTPUT, "Sega", 1, Ttl74247.pinNrToPortNr(Ttl74247.SEGA))
        .add(Port.OUTPUT, "Segg", 1, Ttl74247.pinNrToPortNr(Ttl74247.SEGG))
        .add(Port.OUTPUT, "Segf", 1, Ttl74247.pinNrToPortNr(Ttl74247.SEGF));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var decoder = new WithSelectHdlGenerator("decoder1", "bcd", 4, "segments", 7)
        .setDefault("0000000");
    for (var code = 0; code < GLYPH.length; code++) {
      decoder.add((long) code, GLYPH[code]);
    }
    final var contents = LineBuffer.getHdlBuffer();
    contents.add(decoder.getHdlCode()).empty();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          Sega <= realSegments(0);
          Segb <= realSegments(1);
          Segc <= realSegments(2);
          Segd <= realSegments(3);
          Sege <= realSegments(4);
          Segf <= realSegments(5);
          Segg <= realSegments(6);

          bcd <= D&C&B&A;

          realSegments <= ({{others}} => '1') {{when}} BI = '0' {{else}}
                          ({{others}} => '0') {{when}} LT = '0' {{else}}
                          ({{others}} => '1') {{when}} (RBI='0') {{and}} (bcd=x"0") {{else}}
                          {{not}}(segments);
          """);
    } else {
      contents.add("""
          assign Sega = realSegments[0];
          assign Segb = realSegments[1];
          assign Segc = realSegments[2];
          assign Segd = realSegments[3];
          assign Sege = realSegments[4];
          assign Segf = realSegments[5];
          assign Segg = realSegments[6];
          assign bcd  = {D, C, B, A};

          assign realSegments = BI == 0 ? 7'h7F : LT == 0 ? 0 : RBI == 0 && bcd == 0 ? 7'h7F : !segments;
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
