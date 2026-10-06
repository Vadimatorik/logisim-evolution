/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/**
 * VHDL and Verilog generator for the 74x7002 quad NOR gate.
 *
 * <p>The shared quad-gate generator maps ports like a 7400, with the output on the third pin of
 * each left-hand gate. 74x7002 follows the 74x02 pin order, so this generator replaces that map.
 * Logical ports, with power pins omitted, are {@code 1Y, 1A, 1B, 2Y, 2A, 2B, 3A, 3B, 3Y, 4A, 4B,
 * 4Y}.
 */
public class Ttl747002HdlGenerator extends AbstractGateHdlGenerator {
  private static final int[] INPUT_A = {1, 4, 6, 9};
  private static final int[] INPUT_B = {2, 5, 7, 10};
  private static final int[] OUTPUT = {0, 3, 8, 11};

  /** Creates a generator whose four NOR gates use the 74x02 pin order. */
  public Ttl747002HdlGenerator() {
    myPorts.removePorts();
    for (var gate = 0; gate < INPUT_A.length; gate++) {
      myPorts
          .add(Port.INPUT, String.format("gateA%d", gate), 1, INPUT_A[gate])
          .add(Port.INPUT, String.format("gateB%d", gate), 1, INPUT_B[gate])
          .add(Port.OUTPUT, String.format("gateO%d", gate), 1, OUTPUT[gate]);
    }
  }

  @Override
  public LineBuffer getLogicFunction(int index) {
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}gateO{{1}}{{=}}{{not}}(gateA{{1}}{{or}}gateB{{1}});", index);
  }

  int logicalPort(String name) {
    return myPorts.getComponentPortId(name);
  }
}
