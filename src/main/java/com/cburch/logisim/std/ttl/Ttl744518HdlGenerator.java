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
 * VHDL and Verilog for one 74HC4518. The clock pins stay ordinary inputs: each half has two edges,
 * and the generator's clock-port list cannot give four distinct one-bit clocks their own tick. The
 * next state is the same synchronous decade network as {@link Ttl744518}.
 */
public class Ttl744518HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose clocks are ordinary inputs. */
  public Ttl744518HdlGenerator() {
    super();
    myWires
        .addRegister("count1", 4)
        .addRegister("count2", 4);
    myPorts
        .add(Port.INPUT, "CP0_1", 1, Ttl744518.PORT_INDEX_1CP0)
        .add(Port.INPUT, "CP1_1", 1, Ttl744518.PORT_INDEX_1CP1)
        .add(Port.INPUT, "MR_1", 1, Ttl744518.PORT_INDEX_1MR)
        .add(Port.OUTPUT, "Q0_1", 1, Ttl744518.PORT_INDEX_1Q0)
        .add(Port.OUTPUT, "Q1_1", 1, Ttl744518.PORT_INDEX_1Q1)
        .add(Port.OUTPUT, "Q2_1", 1, Ttl744518.PORT_INDEX_1Q2)
        .add(Port.OUTPUT, "Q3_1", 1, Ttl744518.PORT_INDEX_1Q3)
        .add(Port.INPUT, "CP0_2", 1, Ttl744518.PORT_INDEX_2CP0)
        .add(Port.INPUT, "CP1_2", 1, Ttl744518.PORT_INDEX_2CP1)
        .add(Port.INPUT, "MR_2", 1, Ttl744518.PORT_INDEX_2MR)
        .add(Port.OUTPUT, "Q0_2", 1, Ttl744518.PORT_INDEX_2Q0)
        .add(Port.OUTPUT, "Q1_2", 1, Ttl744518.PORT_INDEX_2Q1)
        .add(Port.OUTPUT, "Q2_2", 1, Ttl744518.PORT_INDEX_2Q2)
        .add(Port.OUTPUT, "Q3_2", 1, Ttl744518.PORT_INDEX_2Q3);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          Q0_1 <= count1(0);
          Q1_1 <= count1(1);
          Q2_1 <= count1(2);
          Q3_1 <= count1(3);
          Q0_2 <= count2(0);
          Q1_2 <= count2(1);
          Q2_2 <= count2(2);
          Q3_2 <= count2(3);

          ctr1 : {{process}}(CP0_1, CP1_1, MR_1) {{is}}
             {{begin}}
                {{if}} (MR_1 = '1') {{then}} count1 <= "0000";
                {{elsif}} (rising_edge(CP0_1)) {{then}}
                   {{if}} (CP1_1 = '1') {{then}}
          """);
      contents.add(vhdlNext("count1"));
      contents.add("""
                   {{end}} {{if}};
                {{elsif}} (falling_edge(CP1_1)) {{then}}
                   {{if}} (CP0_1 = '0') {{then}}
          """);
      contents.add(vhdlNext("count1"));
      contents.add("""
                   {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} ctr1;

          ctr2 : {{process}}(CP0_2, CP1_2, MR_2) {{is}}
             {{begin}}
                {{if}} (MR_2 = '1') {{then}} count2 <= "0000";
                {{elsif}} (rising_edge(CP0_2)) {{then}}
                   {{if}} (CP1_2 = '1') {{then}}
          """);
      contents.add(vhdlNext("count2"));
      contents.add("""
                   {{end}} {{if}};
                {{elsif}} (falling_edge(CP1_2)) {{then}}
                   {{if}} (CP0_2 = '0') {{then}}
          """);
      contents.add(vhdlNext("count2"));
      contents.add("""
                   {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} ctr2;
          """);
    } else {
      contents.add("""
          assign Q0_1 = count1[0];
          assign Q1_1 = count1[1];
          assign Q2_1 = count1[2];
          assign Q3_1 = count1[3];
          assign Q0_2 = count2[0];
          assign Q1_2 = count2[1];
          assign Q2_2 = count2[2];
          assign Q3_2 = count2[3];

          always @(posedge CP0_1 or negedge CP1_1 or posedge MR_1)
          begin
             if (MR_1) count1 <= 0;
             else if (CP0_1 == 1 && CP1_1 == 1) begin
          """);
      contents.add(verilogNext("count1"));
      contents.add("""
             end else if (CP0_1 == 0 && CP1_1 == 0) begin
          """);
      contents.add(verilogNext("count1"));
      contents.add("""
             end
          end

          always @(posedge CP0_2 or negedge CP1_2 or posedge MR_2)
          begin
             if (MR_2) count2 <= 0;
             else if (CP0_2 == 1 && CP1_2 == 1) begin
          """);
      contents.add(verilogNext("count2"));
      contents.add("""
             end else if (CP0_2 == 0 && CP1_2 == 0) begin
          """);
      contents.add(verilogNext("count2"));
      contents.add("""
             end
          end
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }

  /** Bit updates for one decade stage. Every right-hand side reads the current code. */
  private static String vhdlNext(String count) {
    return """
        %1$s(0) <= {{not}} %1$s(0);
        %1$s(1) <= %1$s(1) {{xor}} (%1$s(0) {{and}} {{not}} %1$s(3));
        %1$s(2) <= %1$s(2) {{xor}} (%1$s(0) {{and}} %1$s(1));
        %1$s(3) <= %1$s(3) {{xor}} ((%1$s(0) {{and}} %1$s(1) {{and}} %1$s(2))
            {{or}} (%1$s(0) {{and}} %1$s(3)));
        """.formatted(count);
  }

  /** Bit updates for one decade stage. Non-blocking assignments read the current code. */
  private static String verilogNext(String count) {
    return """
        %1$s[0] <= ~%1$s[0];
        %1$s[1] <= %1$s[1] ^ (%1$s[0] & ~%1$s[3]);
        %1$s[2] <= %1$s[2] ^ (%1$s[0] & %1$s[1]);
        %1$s[3] <= %1$s[3] ^ ((%1$s[0] & %1$s[1] & %1$s[2]) | (%1$s[0] & %1$s[3]));
        """.formatted(count);
  }
}
