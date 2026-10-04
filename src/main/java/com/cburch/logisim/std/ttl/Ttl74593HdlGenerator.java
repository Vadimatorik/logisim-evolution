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

/** VHDL and Verilog for the input register, counter and 3-state bus of a 74593. */
public class Ttl74593HdlGenerator extends AbstractHdlGeneratorFactory {
  private static final String[] BUS = {"QA", "QB", "QC", "QD", "QE", "QF", "QG", "QH"};

  /**
   * Creates a generator for the 74593. Open active-low controls stay high, so an open chip neither
   * loads, clears, nor enables its bus. The active-high count enable stays low.
   */
  public Ttl74593HdlGenerator() {
    super();
    myWires.addRegister("inputReg", 8).addRegister("counter", 8);
    myPorts
        .add(Port.INOUT, "QA", 1, port(Ttl74593.QA), true)
        .add(Port.INOUT, "QB", 1, port(Ttl74593.QB), true)
        .add(Port.INOUT, "QC", 1, port(Ttl74593.QC), true)
        .add(Port.INOUT, "QD", 1, port(Ttl74593.QD), true)
        .add(Port.INOUT, "QE", 1, port(Ttl74593.QE), true)
        .add(Port.INOUT, "QF", 1, port(Ttl74593.QF), true)
        .add(Port.INOUT, "QG", 1, port(Ttl74593.QG), true)
        .add(Port.INOUT, "QH", 1, port(Ttl74593.QH), true)
        .add(Port.INPUT, "nCLOAD", 1, port(Ttl74593.NCLOAD), false)
        .add(Port.OUTPUT, "nRCO", 1, port(Ttl74593.NRCO))
        .add(Port.INPUT, "nCCLR", 1, port(Ttl74593.NCCLR), false)
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, port(Ttl74593.CCK))
        .add(Port.INPUT, "nCCKEN", 1, port(Ttl74593.NCCKEN), false)
        .add(Port.INPUT, "CCKEN", 1, port(Ttl74593.CCKEN), true)
        .add(Port.CLOCK, HdlPorts.getClockName(2), 1, port(Ttl74593.RCK))
        .add(Port.INPUT, "nRCKEN", 1, port(Ttl74593.NRCKEN), false)
        .add(Port.INPUT, "nG", 1, port(Ttl74593.NG), false)
        .add(Port.INPUT, "G", 1, port(Ttl74593.G), true);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLK", HdlPorts.getClockName(1))
            .pair("CLK2", HdlPorts.getClockName(2))
            .pair("tick", HdlPorts.getTickName(1))
            .pair("tick2", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      addVhdl(contents);
    } else {
      addVerilog(contents);
    }
    return contents.empty();
  }

  private static void addVhdl(LineBuffer contents) {
    for (var bit = 0; bit < BUS.length; bit++) {
      contents.add(
          "{{1}} <= counter({{2}}) {{when}} G = '1' {{and}} nG = '0' {{else}} 'Z';",
          BUS[bit],
          bit);
    }
    contents.add(
        """
        nRCO <= '0' {{when}} counter = "11111111" {{else}} '1';

        regProc : {{process}}({{CLK2}}) {{is}}
        {{begin}}
           {{if}} (rising_edge({{CLK2}})) {{then}}
              {{if}} ({{tick2}} = '1' {{and}} nRCKEN = '0') {{then}}
                 inputReg <= QH & QG & QF & QE & QD & QC & QB & QA;
              {{end}} {{if}};
           {{end}} {{if}};
        {{end}} {{process}} regProc;

        cntProc : {{process}}({{CLK}}, nCCLR, nCLOAD, inputReg) {{is}}
        {{begin}}
           {{if}} (nCCLR = '0') {{then}}
              counter <= ({{others}} => '0');
           {{elsif}} (nCLOAD = '0') {{then}}
              counter <= inputReg;
           {{elsif}} (rising_edge({{CLK}})) {{then}}
              {{if}} ({{tick}} = '1'
                  {{and}} (CCKEN = '1' {{or}} nCCKEN = '0')) {{then}}
                 counter <= std_logic_vector(unsigned(counter) + 1);
              {{end}} {{if}};
           {{end}} {{if}};
        {{end}} {{process}} cntProc;
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    for (var bit = 0; bit < BUS.length; bit++) {
      contents.add(
          "assign {{1}} = (G == 1 && nG == 0) ? counter[{{2}}] : 1'bz;", BUS[bit], bit);
    }
    contents.add(
        """
        assign nRCO = (counter == 8'b11111111) ? 1'b0 : 1'b1;

        always @(posedge {{CLK2}})
        begin
           if ({{tick2}} == 1 && nRCKEN == 0)
              inputReg <= {QH, QG, QF, QE, QD, QC, QB, QA};
        end

        always @(posedge {{CLK}} or negedge nCCLR or negedge nCLOAD or inputReg)
        begin
           if (nCCLR == 0) counter <= 8'b0;
           else if (nCLOAD == 0) counter <= inputReg;
           else if ({{tick}} == 1 && (CCKEN == 1 || nCCKEN == 0))
              counter <= counter + 1;
        end
        """);
  }

  private static int port(byte pin) {
    return Ttl74593.pinNrToPortNr(pin);
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
