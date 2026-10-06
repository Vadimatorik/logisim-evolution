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
 * HDL export for the 74249 segment decoder.
 *
 * <p>{@code BI/RBO} is a wire-AND pin. The generator treats it as the blanking input, matching the
 * 7447 exporter, and does not drive the ripple-blanking output.
 */
public class Ttl74249HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates the port and wire lists for a 74249. */
  public Ttl74249HdlGenerator() {
    super();
    myWires
        .addWire("segments", 7)
        .addWire("realSegments", 7)
        .addWire("bcd", 4);
    myPorts
        .add(Port.INPUT, "BCD0", 1, Ttl74249.pinNrToPortNr(Ttl74249.A))
        .add(Port.INPUT, "BCD1", 1, Ttl74249.pinNrToPortNr(Ttl74249.B))
        .add(Port.INPUT, "BCD2", 1, Ttl74249.pinNrToPortNr(Ttl74249.C))
        .add(Port.INPUT, "BCD3", 1, Ttl74249.pinNrToPortNr(Ttl74249.D))
        .add(Port.INPUT, "LT", 1, Ttl74249.pinNrToPortNr(Ttl74249.LT), false)
        .add(Port.INPUT, "BI", 1, Ttl74249.pinNrToPortNr(Ttl74249.BI_RBO), false)
        .add(Port.INPUT, "RBI", 1, Ttl74249.pinNrToPortNr(Ttl74249.RBI), false)
        .add(Port.OUTPUT, "Sega", 1, Ttl74249.pinNrToPortNr(Ttl74249.SEG_A))
        .add(Port.OUTPUT, "Segb", 1, Ttl74249.pinNrToPortNr(Ttl74249.SEG_B))
        .add(Port.OUTPUT, "Segc", 1, Ttl74249.pinNrToPortNr(Ttl74249.SEG_C))
        .add(Port.OUTPUT, "Segd", 1, Ttl74249.pinNrToPortNr(Ttl74249.SEG_D))
        .add(Port.OUTPUT, "Sege", 1, Ttl74249.pinNrToPortNr(Ttl74249.SEG_E))
        .add(Port.OUTPUT, "Segf", 1, Ttl74249.pinNrToPortNr(Ttl74249.SEG_F))
        .add(Port.OUTPUT, "Segg", 1, Ttl74249.pinNrToPortNr(Ttl74249.SEG_G));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    // Bit strings are gfedcba, so the rightmost bit is segment a. Six and nine include tails.
    final var decoder = new WithSelectHdlGenerator("decoder1", "bcd", 4, "segments", 7)
        .setDefault("0000000")
        .add(0L, "0111111")
        .add(1L, "0000110")
        .add(2L, "1011011")
        .add(3L, "1001111")
        .add(4L, "1100110")
        .add(5L, "1101101")
        .add(6L, "1111101")
        .add(7L, "0000111")
        .add(8L, "1111111")
        .add(9L, "1101111")
        .add(10L, "1011000")
        .add(11L, "1001100")
        .add(12L, "1100010")
        .add(13L, "1101001")
        .add(14L, "1111000");
    final var contents = LineBuffer.getHdlBuffer();
    contents.add(decoder.getHdlCode()).empty();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
            Sega  <= realSegments(0);
            Segb  <= realSegments(1);
            Segc  <= realSegments(2);
            Segd  <= realSegments(3);
            Sege  <= realSegments(4);
            Segf  <= realSegments(5);
            Segg  <= realSegments(6);

            bcd   <= BCD3&BCD2&BCD1&BCD0;

            realSegments <= ({{others}} => '0') {{when}} BI = '0' {{else}}
                            ({{others}} => '1') {{when}} LT = '0' {{else}}
                            ({{others}} => '0') {{when}} (RBI='0') {{and}} (bcd=x"0") {{else}}
                            segments;
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
          assign bcd  = {BCD3, BCD2, BCD1, BCD0};

          assign realSegments = BI == 0 ? 7'h00 : LT == 0 ? 7'h7F : RBI == 0 && bcd == 0 ? 7'h00 : segments;
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
