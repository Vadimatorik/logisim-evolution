/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.awt.Graphics;

/**
 * TTL 74x174: hex positive-edge-triggered D-type flip-flop with a shared asynchronous clear.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT174.pdf">Nexperia
 * 74HC174/74HCT174 datasheet</a>. A low on {@code nCLR} (MR) resets every Q output, independent of
 * the clock and the data inputs. While {@code nCLR} is high, each Dn is stored on the low-to-high
 * transition of {@code CLK} and held otherwise. The Nexperia pin names Q0..Q5 and D0..D5 are the
 * same pins as Q1..Q6 and D1..D6 here. Setup and hold times are not modeled.
 */
public class Ttl74174 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74174";

  public static final int DELAY = 8;

  public static final int PORT_INDEX_nCLR = 0;
  public static final int PORT_INDEX_Q1 = 1;
  public static final int PORT_INDEX_D1 = 2;
  public static final int PORT_INDEX_D2 = 3;
  public static final int PORT_INDEX_Q2 = 4;
  public static final int PORT_INDEX_D3 = 5;
  public static final int PORT_INDEX_Q3 = 6;
  public static final int PORT_INDEX_CLK = 7;
  public static final int PORT_INDEX_Q4 = 8;
  public static final int PORT_INDEX_D4 = 9;
  public static final int PORT_INDEX_Q5 = 10;
  public static final int PORT_INDEX_D5 = 11;
  public static final int PORT_INDEX_D6 = 12;
  public static final int PORT_INDEX_Q6 = 13;

  private static final int[] DATA_PORTS = {
    PORT_INDEX_D1, PORT_INDEX_D2, PORT_INDEX_D3, PORT_INDEX_D4, PORT_INDEX_D5, PORT_INDEX_D6
  };

  private static final int[] OUTPUT_PORTS = {
    PORT_INDEX_Q1, PORT_INDEX_Q2, PORT_INDEX_Q3, PORT_INDEX_Q4, PORT_INDEX_Q5, PORT_INDEX_Q6
  };

  public Ttl74174() {
    super(
        _ID,
        (byte) 16,
        new byte[] {2, 5, 7, 10, 12, 15},
        new String[] {
          "nCLR", "Q1", "D1", "D2", "Q2", "D3", "Q3", "CLK", "Q4", "D4", "Q5", "D5", "D6", "Q6"
        },
        new Ttl74174HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    drawFlops(g, x, y, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(OUTPUT_PORTS.length));
      state.setData(data);
    }
    final var triggered = data.updateClock(state.getPortValue(PORT_INDEX_CLK));
    if (state.getPortValue(PORT_INDEX_nCLR) == Value.FALSE) {
      data.setValue(Value.createKnown(data.getWidth(), 0));
    } else if (triggered) {
      final var vals = data.getValue().getAll();
      for (var bit = 0; bit < DATA_PORTS.length; bit++) {
        vals[bit] = state.getPortValue(DATA_PORTS[bit]);
      }
      data.setValue(Value.create(vals));
    }
    for (var bit = 0; bit < OUTPUT_PORTS.length; bit++) {
      state.setPort(OUTPUT_PORTS[bit], data.getValue().get(bit), DELAY);
    }
  }

  private void drawFlops(Graphics g, int x, int y, int height) {
    final var top = y + AbstractTtlGate.PIN_HEIGHT;
    final var clearBottom = y + height - 8;
    final var clearTop = y + 8;

    g.drawLine(x + 10, y + height - AbstractTtlGate.PIN_HEIGHT, x + 10, clearBottom);
    g.drawLine(x + 10, clearBottom, x + 146, clearBottom);
    g.drawLine(x + 146, clearBottom, x + 146, clearTop);
    g.drawLine(x + 36, clearTop, x + 146, clearTop);

    g.drawLine(x + 150, top, x + 150, y + 30);
    g.drawLine(x + 36, y + 30, x + 150, y + 30);
    g.fillOval(x + 148, y + 28, 4, 4);

    drawFlop(g, x, y, height, false, 30, 50, true);
    drawFlop(g, x, y, height, false, 90, 70, false);
    drawFlop(g, x, y, height, false, 130, 110, false);
    drawFlop(g, x, y, height, true, 30, 50, true);
    drawFlop(g, x, y, height, true, 90, 70, false);
    drawFlop(g, x, y, height, true, 130, 110, false);
  }

  /** Draws one D flip-flop between its Q and D package pins. {@code qOnLeft} selects the side. */
  private void drawFlop(
      Graphics g, int x, int y, int height, boolean onTop, int qPin, int dPin, boolean qOnLeft) {
    final var leftPin = Math.min(qPin, dPin);
    final var bodyX = x + leftPin + 7;
    final var bodyY = onTop ? y + 12 : y + 33;
    final var pinY =
        onTop ? y + AbstractTtlGate.PIN_HEIGHT : y + height - AbstractTtlGate.PIN_HEIGHT;
    final var qEdge = qOnLeft ? bodyX : bodyX + 6;
    final var dEdge = qOnLeft ? bodyX + 6 : bodyX;
    final var qY = bodyY + 4;
    final var dY = bodyY + 10;

    g.drawRect(bodyX, bodyY, 6, 14);
    g.drawLine(x + qPin, pinY, x + qPin, qY);
    g.drawLine(x + qPin, qY, qEdge, qY);
    g.drawLine(x + dPin, pinY, x + dPin, dY);
    g.drawLine(x + dPin, dY, dEdge, dY);

    final var clockX = bodyX + 3;
    if (onTop) {
      g.drawLine(clockX, y + 30, clockX, bodyY + 14);
      g.drawLine(bodyX, bodyY + 14, clockX, bodyY + 11);
      g.drawLine(bodyX + 6, bodyY + 14, clockX, bodyY + 11);
      g.drawOval(bodyX + 2, bodyY - 3, 2, 2);
      g.drawLine(clockX, bodyY - 1, clockX, y + 8);
    } else {
      g.drawLine(clockX, bodyY, clockX, y + 30);
      g.drawLine(bodyX, bodyY, clockX, bodyY + 3);
      g.drawLine(bodyX + 6, bodyY, clockX, bodyY + 3);
      g.drawOval(bodyX + 2, bodyY + 14, 2, 2);
      g.drawLine(clockX, bodyY + 16, clockX, y + height - 8);
    }
    g.drawString("Q", qOnLeft ? bodyX - 8 : bodyX + 8, bodyY + 8);
    g.drawString("D", qOnLeft ? bodyX + 8 : bodyX - 8, bodyY + 12);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CLK};
  }
}
