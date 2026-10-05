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
 * VHDL and Verilog for the 7440102 BCD down counter.
 *
 * <p>{@code MR} and {@code PL} are asynchronous. While {@code PL} stays low the jam inputs are in
 * the sensitivity list, so the code follows {@code P0} to {@code P7}. Synchronous preset and
 * counting run on the rising edge and only while the Logisim tick is high. Open active-low
 * controls pull high, so a floating counter neither resets, presets, nor counts.
 */
public class Ttl7440102HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 7440102. */
  public Ttl7440102HdlGenerator() {
    super();
    myWires
        .addWire("s_data", 8)
        .addWire("s_units", 4)
        .addWire("s_tens", 4)
        .addWire("s_units_next", 4)
        .addWire("s_tens_next", 4)
        .addWire("s_counted", 8)
        .addWire("s_sync", 8)
        .addRegister("s_count", 8);
    // Active-low controls pull high. Jam inputs and the clock pull low.
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl7440102.pinNrToPortNr(Ttl7440102.CP), true)
        .add(Port.INPUT, "MR", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.MR), false)
        .add(Port.INPUT, "TE", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.TE), false)
        .add(Port.INPUT, "P0", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.P0), true)
        .add(Port.INPUT, "P1", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.P1), true)
        .add(Port.INPUT, "P2", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.P2), true)
        .add(Port.INPUT, "P3", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.P3), true)
        .add(Port.INPUT, "PL", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.PL), false)
        .add(Port.INPUT, "P4", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.P4), true)
        .add(Port.INPUT, "P5", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.P5), true)
        .add(Port.INPUT, "P6", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.P6), true)
        .add(Port.INPUT, "P7", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.P7), true)
        .add(Port.OUTPUT, "TC", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.TC))
        .add(Port.INPUT, "PE", 1, Ttl7440102.pinNrToPortNr(Ttl7440102.PE), false);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer().pair("clock", HdlPorts.CLOCK).pair("tick", HdlPorts.TICK);
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
        s_data <= P7 & P6 & P5 & P4 & P3 & P2 & P1 & P0;
        s_units <= s_count(3 {{downto}} 0);
        s_tens <= s_count(7 {{downto}} 4);
        s_units_next <= "1001" {{when}} s_units = "0000" {{else}}
                       std_logic_vector(unsigned(s_units) - 1);
        s_tens_next <= "1001" {{when}} s_units = "0000" {{and}} s_tens = "0000" {{else}}
                       std_logic_vector(unsigned(s_tens) - 1) {{when}} s_units = "0000" {{else}}
                       s_tens;
        s_counted <= s_tens_next & s_units_next;
        s_sync <= s_data {{when}} PE = '0' {{else}}
                  s_counted {{when}} TE = '0' {{else}}
                  s_count;
        TC <= '0' {{when}} s_count = "00000000" {{and}} TE = '0' {{else}} '1';

        count : {{process}} ({{clock}}, MR, PL, P0, P1, P2, P3, P4, P5, P6, P7) {{is}}
        {{begin}}
           {{if}} (MR = '0') {{then}}
              s_count <= "10011001";
           {{elsif}} (PL = '0') {{then}}
              s_count <= P7 & P6 & P5 & P4 & P3 & P2 & P1 & P0;
           {{elsif}} (rising_edge({{clock}})) {{then}}
              {{if}} ({{tick}} = '1') {{then}}
                 s_count <= s_sync;
              {{end}} {{if}};
           {{end}} {{if}};
        {{end}} {{process}} count;
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add(
        """
        reg s_clock_was_low;

        assign s_data = {P7, P6, P5, P4, P3, P2, P1, P0};
        assign s_units = s_count[3:0];
        assign s_tens = s_count[7:4];
        assign s_units_next = (s_units == 4'b0000) ? 4'b1001 : (s_units - 4'b0001);
        assign s_tens_next = (s_units != 4'b0000) ? s_tens :
                             (s_tens == 4'b0000) ? 4'b1001 : (s_tens - 4'b0001);
        assign s_counted = {s_tens_next, s_units_next};
        assign s_sync = (PE == 0) ? s_data : (TE == 0) ? s_counted : s_count;
        assign TC = ((s_count == 8'b00000000) && (TE == 0)) ? 1'b0 : 1'b1;

        initial s_clock_was_low = 1'b1;

        always @(posedge {{clock}} or negedge {{clock}} or negedge MR or negedge PL
                 or P0 or P1 or P2 or P3 or P4 or P5 or P6 or P7)
        begin
           if (MR == 0) s_count <= 8'b10011001;
           else if (PL == 0) s_count <= {P7, P6, P5, P4, P3, P2, P1, P0};
           else if ({{clock}} == 1 && s_clock_was_low == 1 && {{tick}} == 1) s_count <= s_sync;
           s_clock_was_low <= ({{clock}} == 0);
        end
        """);
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
