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
 * VHDL and Verilog for the storage register and shift register of a 74597.
 *
 * <p>{@code MR} and {@code PL} pull high so an open pin neither clears nor loads the shift
 * register. Holding both low is invalid on the datasheet and is not given a defined HDL result;
 * the simulator model makes that shift register unknown instead. The bit width of a clock port is
 * its tick index, so {@code STCP} is declared two bits wide and enabled by {@code tick2}.
 */
public class Ttl74597HdlGenerator extends AbstractHdlGeneratorFactory {
  private static final String PARALLEL = "D7 & D6 & D5 & D4 & D3 & D2 & D1 & D0";
  private static final String PARALLEL_VERILOG = "{D7, D6, D5, D4, D3, D2, D1, D0}";

  /** Creates an HDL generator whose unconnected reset and load pins are pulled high. */
  public Ttl74597HdlGenerator() {
    super();
    myWires.addRegister("storeReg", 8).addRegister("shiftReg", 8);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74597.pinNrToPortNr(Ttl74597.SHCP))
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl74597.pinNrToPortNr(Ttl74597.STCP))
        .add(Port.INPUT, "MR", 1, Ttl74597.pinNrToPortNr(Ttl74597.MR), false)
        .add(Port.INPUT, "PL", 1, Ttl74597.pinNrToPortNr(Ttl74597.PL), false)
        .add(Port.INPUT, "DS", 1, Ttl74597.pinNrToPortNr(Ttl74597.DS))
        .add(Port.INPUT, "D0", 1, Ttl74597.pinNrToPortNr(Ttl74597.D0))
        .add(Port.INPUT, "D1", 1, Ttl74597.pinNrToPortNr(Ttl74597.D1))
        .add(Port.INPUT, "D2", 1, Ttl74597.pinNrToPortNr(Ttl74597.D2))
        .add(Port.INPUT, "D3", 1, Ttl74597.pinNrToPortNr(Ttl74597.D3))
        .add(Port.INPUT, "D4", 1, Ttl74597.pinNrToPortNr(Ttl74597.D4))
        .add(Port.INPUT, "D5", 1, Ttl74597.pinNrToPortNr(Ttl74597.D5))
        .add(Port.INPUT, "D6", 1, Ttl74597.pinNrToPortNr(Ttl74597.D6))
        .add(Port.INPUT, "D7", 1, Ttl74597.pinNrToPortNr(Ttl74597.D7))
        .add(Port.OUTPUT, "Q", 1, Ttl74597.pinNrToPortNr(Ttl74597.Q));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLK1", HdlPorts.getClockName(1))
            .pair("CLK2", HdlPorts.getClockName(2))
            .pair("tick1", HdlPorts.getTickName(1))
            .pair("tick2", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          Q <= shiftReg(7);

          storeProc : {{process}}({{CLK2}}) {{is}}
          {{begin}}
             {{if}} (rising_edge({{CLK2}})) {{then}}
                {{if}} ({{tick2}} = '1') {{then}}
                   storeReg <= {{parallel}};
                {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} storeProc;

          shiftProc : {{process}}({{CLK1}}, {{CLK2}}, PL, MR) {{is}}
          {{begin}}
             {{if}} (MR = '0' {{and}} PL = '1') {{then}}
                shiftReg <= ({{others}} => '0');
             {{elsif}} (PL = '0' {{and}} MR = '1') {{then}}
                {{if}} ({{tick2}} = '1') {{then}} shiftReg <= {{parallel}};
                {{else}} shiftReg <= storeReg; {{end}} {{if}};
             {{elsif}} (rising_edge({{CLK1}})) {{then}}
                {{if}} ({{tick1}} = '1') {{then}}
                   shiftReg <= shiftReg(6 {{downto}} 0) & DS;
                {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} shiftProc;
          """.replace("{{parallel}}", PARALLEL));
    } else {
      contents.add("""
          assign Q = shiftReg[7];

          always @(posedge {{CLK2}})
          begin
             if ({{tick2}} == 1) storeReg <= {{parallel}};
          end

          always @(posedge {{CLK1}} or posedge {{CLK2}} or negedge MR or negedge PL)
          begin
             if (MR == 0 && PL == 1) shiftReg <= 8'b0;
             else if (PL == 0 && MR == 1) shiftReg <= ({{tick2}} == 1) ? {{parallel}} : storeReg;
             else if ({{tick1}} == 1) shiftReg <= {shiftReg[6:0], DS};
          end
          """.replace("{{parallel}}", PARALLEL_VERILOG));
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
