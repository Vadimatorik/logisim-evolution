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

/** VHDL and Verilog for the level-sensitive BCD latch and segment decoder of a 744543. */
public class Ttl744543HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose open inputs are pulled low, so the latch holds. */
  public Ttl744543HdlGenerator() {
    super();
    myWires
        .addWire("s_data", 4)
        .addWire("s_phase", 7)
        .addWire("s_segments", 7)
        .addRegister("s_bcd", 4)
        .addRegister("s_glyph", 7);
    myPorts
        .add(Port.INPUT, "LD", 1, Ttl744543.pinNrToPortNr(Ttl744543.LD), true)
        .add(Port.INPUT, "D2", 1, Ttl744543.pinNrToPortNr(Ttl744543.D2), true)
        .add(Port.INPUT, "D1", 1, Ttl744543.pinNrToPortNr(Ttl744543.D1), true)
        .add(Port.INPUT, "D3", 1, Ttl744543.pinNrToPortNr(Ttl744543.D3), true)
        .add(Port.INPUT, "D0", 1, Ttl744543.pinNrToPortNr(Ttl744543.D0), true)
        .add(Port.INPUT, "PH", 1, Ttl744543.pinNrToPortNr(Ttl744543.PH), true)
        .add(Port.INPUT, "BI", 1, Ttl744543.pinNrToPortNr(Ttl744543.BI), true)
        .add(Port.OUTPUT, "a", 1, Ttl744543.pinNrToPortNr(Ttl744543.QA))
        .add(Port.OUTPUT, "b", 1, Ttl744543.pinNrToPortNr(Ttl744543.QB))
        .add(Port.OUTPUT, "c", 1, Ttl744543.pinNrToPortNr(Ttl744543.QC))
        .add(Port.OUTPUT, "d", 1, Ttl744543.pinNrToPortNr(Ttl744543.QD))
        .add(Port.OUTPUT, "e", 1, Ttl744543.pinNrToPortNr(Ttl744543.QE))
        .add(Port.OUTPUT, "g", 1, Ttl744543.pinNrToPortNr(Ttl744543.QG))
        .add(Port.OUTPUT, "f", 1, Ttl744543.pinNrToPortNr(Ttl744543.QF));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      addVhdl(contents);
    } else {
      addVerilog(contents);
    }
    return contents.empty();
  }

  private static void addVhdl(LineBuffer contents) {
    contents.add(
        """
        s_data <= D3 & D2 & D1 & D0;

        store : {{process}}(LD, s_data) {{is}}
        {{begin}}
           {{if}} (LD = '1') {{then}}
              s_bcd <= s_data;
           {{end}} {{if}};
        {{end}} {{process}} store;

        {{with}} s_bcd {{select}} s_glyph <=
        """);
    for (var code = 0; code < 10; code++) {
      contents.add("   \"{{1}}\" {{when}} \"{{2}}\",", Ttl744543.glyphBits(code), nibble(code));
    }
    contents.add(
        """
           "0000000" {{when}} {{others}};

        s_segments <= ({{others}} => PH) {{when}} BI = '1' {{else}}
                      s_glyph {{xor}} ({{others}} => PH);
        a <= s_segments(6);
        b <= s_segments(5);
        c <= s_segments(4);
        d <= s_segments(3);
        e <= s_segments(2);
        f <= s_segments(1);
        g <= s_segments(0);
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add(
        """
        assign s_data = {D3, D2, D1, D0};

        always @(*)
        begin
           if (LD == 1)
              s_bcd = s_data;
        end

        always @(*)
        begin
           case (s_bcd)
        """);
    for (var code = 0; code < 10; code++) {
      contents.add(
          "      4'b{{1}} : s_glyph = 7'b{{2}};", nibble(code), Ttl744543.glyphBits(code));
    }
    contents.add(
        """
              default : s_glyph = 7'b0000000;
           endcase
        end

        assign s_phase = {PH, PH, PH, PH, PH, PH, PH};
        assign s_segments = BI ? s_phase : (s_glyph ^ s_phase);
        assign a = s_segments[6];
        assign b = s_segments[5];
        assign c = s_segments[4];
        assign d = s_segments[3];
        assign e = s_segments[2];
        assign f = s_segments[1];
        assign g = s_segments[0];
        """);
  }

  private static String nibble(int code) {
    return String.format("%4s", Integer.toBinaryString(code)).replace(' ', '0');
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
