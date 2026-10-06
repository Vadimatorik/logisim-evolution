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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/**
 * VHDL and Verilog generator for the 74x28 quad NOR buffer.
 *
 * <p>The port order is the 7402 package: each left-hand gate is output, A, B, and the right-hand
 * gates are A, B, output. {@link AbstractGateHdlGenerator} maps the 7400 package instead.
 */
public class Ttl7428HdlGenerator extends AbstractHdlGeneratorFactory {
  private static final int[] INPUT_A = {
    Ttl7428.PORT_1A, Ttl7428.PORT_2A, Ttl7428.PORT_3A, Ttl7428.PORT_4A
  };
  private static final int[] INPUT_B = {
    Ttl7428.PORT_1B, Ttl7428.PORT_2B, Ttl7428.PORT_3B, Ttl7428.PORT_4B
  };
  private static final int[] OUTPUTS = {
    Ttl7428.PORT_1Y, Ttl7428.PORT_2Y, Ttl7428.PORT_3Y, Ttl7428.PORT_4Y
  };

  /** Creates a generator for the four NOR buffers. */
  public Ttl7428HdlGenerator() {
    super();
    for (var gate = 0; gate < OUTPUTS.length; gate++) {
      myPorts
          .add(Port.INPUT, String.format("gateA%d", gate), 1, INPUT_A[gate])
          .add(Port.INPUT, String.format("gateB%d", gate), 1, INPUT_B[gate])
          .add(Port.OUTPUT, String.format("gateO%d", gate), 1, OUTPUTS[gate]);
    }
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    for (var gate = 0; gate < OUTPUTS.length; gate++) {
      contents
          .addRemarkBlock(String.format("Here gate %d is described", gate))
          .add("{{assign}}gateO{{1}}{{=}}{{not}}(gateA{{1}}{{or}}gateB{{1}});", gate);
    }
    return contents;
  }

  /** Returns the logical port index bound to one generated HDL port. */
  int logicalPort(String name) {
    return myPorts.getComponentPortId(name);
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
