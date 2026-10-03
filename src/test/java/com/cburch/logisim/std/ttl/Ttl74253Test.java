/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.ttl.TtlTestInstanceState.createInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

class Ttl74253Test {
  /** Each row is I0, I1, I2, I3, the output, and the output enable of one multiplexer. */
  private static final byte[][] CHANNELS = {
    {Ttl74253.L1_I0, Ttl74253.L1_I1, Ttl74253.L1_I2, Ttl74253.L1_I3, Ttl74253.L1_Y, Ttl74253.OE1},
    {Ttl74253.L2_I0, Ttl74253.L2_I1, Ttl74253.L2_I2, Ttl74253.L2_I3, Ttl74253.L2_Y, Ttl74253.OE2}
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var mux = new Ttl74253();
    final var hiddenPower = createInstance(mux, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74253.OE1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.S1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.L1_I3, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.L1_I2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.L1_I1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.L1_I0, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.L1_Y, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.L2_Y, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.L2_I0, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.L2_I1, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.L2_I2, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.L2_I3, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.S0, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74253.OE2, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(mux, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachChannelPassesTheSelectedSourceAndIgnoresTheOthers() {
    final var mux = new Ttl74253();

    for (var select = 0; select < 4; select++) {
      for (var pattern = 0; pattern < 4; pattern++) {
        final var state = enabled(mux, select);
        for (var channel = 0; channel < CHANNELS.length; channel++) {
          final var selectedHigh = ((pattern >> channel) & 1) == 1;
          driveOppositeUnselected(state, channel, select, selectedHigh);
        }
        mux.propagate(state);

        for (var channel = 0; channel < CHANNELS.length; channel++) {
          final var selectedHigh = ((pattern >> channel) & 1) == 1;
          assertEquals(
              selectedHigh ? Value.TRUE : Value.FALSE,
              output(state, CHANNELS[channel][4]),
              "channel " + channel + " select " + select);
        }
      }
    }
  }

  @Test
  void eachOutputEnableReleasesOnlyItsOwnOutput() {
    final var mux = new Ttl74253();
    final var state = enabled(mux, 0);
    driveOppositeUnselected(state, 0, 0, true);
    driveOppositeUnselected(state, 1, 0, false);

    set(state, Ttl74253.OE1, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74253.L1_Y));
    assertEquals(Value.FALSE, output(state, Ttl74253.L2_Y));

    set(state, Ttl74253.OE1, Value.FALSE);
    set(state, Ttl74253.OE2, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74253.L1_Y));
    assertEquals(Value.UNKNOWN, output(state, Ttl74253.L2_Y));
  }

  @Test
  void highOutputEnableReleasesItsOutput() {
    final var mux = new Ttl74253();
    final var state = enabled(mux, 1);
    set(state, Ttl74253.OE1, Value.TRUE);
    set(state, Ttl74253.OE2, Value.TRUE);
    set(state, Ttl74253.S1, Value.UNKNOWN);
    set(state, Ttl74253.S0, Value.ERROR);
    for (final var channel : CHANNELS) {
      for (var source = 0; source < 4; source++) {
        set(state, channel[source], Value.ERROR);
      }
    }
    mux.propagate(state);

    assertEquals(Value.UNKNOWN, output(state, Ttl74253.L1_Y));
    assertEquals(Value.UNKNOWN, output(state, Ttl74253.L2_Y));
  }

  @Test
  void unknownSelectPassesTheValueTheCandidatesShare() {
    final var mux = new Ttl74253();
    final var state = enabled(mux, 0);
    set(state, Ttl74253.S0, Value.UNKNOWN);
    set(state, Ttl74253.L1_I0, Value.TRUE);
    set(state, Ttl74253.L1_I1, Value.TRUE);
    set(state, Ttl74253.L1_I2, Value.ERROR);
    set(state, Ttl74253.L2_I0, Value.FALSE);
    set(state, Ttl74253.L2_I1, Value.FALSE);
    set(state, Ttl74253.L2_I3, Value.ERROR);
    mux.propagate(state);

    assertEquals(Value.TRUE, output(state, Ttl74253.L1_Y));
    assertEquals(Value.FALSE, output(state, Ttl74253.L2_Y));
  }

  @Test
  void unknownSelectReleasesTheOutputWhenTheCandidatesDiffer() {
    final var mux = new Ttl74253();
    final var lowOrder = enabled(mux, 0);
    set(lowOrder, Ttl74253.S0, Value.UNKNOWN);
    set(lowOrder, Ttl74253.L1_I0, Value.FALSE);
    set(lowOrder, Ttl74253.L1_I1, Value.TRUE);
    mux.propagate(lowOrder);
    assertEquals(Value.UNKNOWN, output(lowOrder, Ttl74253.L1_Y));

    final var both = enabled(mux, 0);
    set(both, Ttl74253.S1, Value.UNKNOWN);
    set(both, Ttl74253.S0, Value.UNKNOWN);
    set(both, Ttl74253.L2_I0, Value.TRUE);
    set(both, Ttl74253.L2_I1, Value.TRUE);
    set(both, Ttl74253.L2_I2, Value.TRUE);
    set(both, Ttl74253.L2_I3, Value.FALSE);
    mux.propagate(both);
    assertEquals(Value.UNKNOWN, output(both, Ttl74253.L2_Y));
  }

  @Test
  void errorOnSelectMattersOnlyWhenTheCandidatesDiffer() {
    final var mux = new Ttl74253();
    final var agreed = enabled(mux, 2);
    set(agreed, Ttl74253.S1, Value.ERROR);
    set(agreed, Ttl74253.L1_I0, Value.TRUE);
    set(agreed, Ttl74253.L1_I2, Value.TRUE);
    set(agreed, Ttl74253.L1_I1, Value.ERROR);
    mux.propagate(agreed);
    assertEquals(Value.TRUE, output(agreed, Ttl74253.L1_Y));

    final var split = enabled(mux, 2);
    set(split, Ttl74253.S1, Value.ERROR);
    set(split, Ttl74253.L2_I0, Value.FALSE);
    set(split, Ttl74253.L2_I2, Value.TRUE);
    mux.propagate(split);
    assertEquals(Value.ERROR, output(split, Ttl74253.L2_Y));
  }

  @Test
  void selectedUnknownOrErrorPassesThroughAndTheOtherSourcesDoNot() {
    final var mux = new Ttl74253();
    final var fromSource2 = enabled(mux, 2);
    set(fromSource2, Ttl74253.L1_I0, Value.ERROR);
    set(fromSource2, Ttl74253.L1_I1, Value.ERROR);
    set(fromSource2, Ttl74253.L1_I2, Value.UNKNOWN);
    set(fromSource2, Ttl74253.L1_I3, Value.ERROR);
    mux.propagate(fromSource2);
    assertEquals(Value.UNKNOWN, output(fromSource2, Ttl74253.L1_Y));

    final var fromSource0 = enabled(mux, 0);
    set(fromSource0, Ttl74253.L2_I0, Value.FALSE);
    set(fromSource0, Ttl74253.L2_I1, Value.ERROR);
    set(fromSource0, Ttl74253.L2_I2, Value.ERROR);
    set(fromSource0, Ttl74253.L2_I3, Value.ERROR);
    mux.propagate(fromSource0);
    assertEquals(Value.FALSE, output(fromSource0, Ttl74253.L2_Y));

    final var errorSelected = enabled(mux, 3);
    set(errorSelected, Ttl74253.L1_I3, Value.ERROR);
    mux.propagate(errorSelected);
    assertEquals(Value.ERROR, output(errorSelected, Ttl74253.L1_Y));
  }

  @Test
  void uncertainOutputEnableFollowsTheDisagreementOfFloatAndData() {
    final var mux = new Ttl74253();
    final var unknownEnable = enabled(mux, 0);
    set(unknownEnable, Ttl74253.OE1, Value.UNKNOWN);
    set(unknownEnable, Ttl74253.L1_I0, Value.TRUE);
    mux.propagate(unknownEnable);
    assertEquals(Value.UNKNOWN, output(unknownEnable, Ttl74253.L1_Y));

    final var errorEnable = enabled(mux, 0);
    set(errorEnable, Ttl74253.OE2, Value.ERROR);
    set(errorEnable, Ttl74253.L2_I0, Value.TRUE);
    mux.propagate(errorEnable);
    assertEquals(Value.ERROR, output(errorEnable, Ttl74253.L2_Y));

    final var errorData = enabled(mux, 1);
    set(errorData, Ttl74253.OE1, Value.UNKNOWN);
    set(errorData, Ttl74253.L1_I1, Value.ERROR);
    mux.propagate(errorData);
    assertEquals(Value.ERROR, output(errorData, Ttl74253.L1_Y));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var mux = new Ttl74253();
    final var state = new TtlTestInstanceState(mux, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    set(state, Ttl74253.OE1, Value.FALSE);
    set(state, Ttl74253.OE2, Value.FALSE);
    set(state, Ttl74253.S1, Value.FALSE);
    set(state, Ttl74253.S0, Value.FALSE);
    set(state, Ttl74253.L1_I0, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74253.L1_Y));

    state.setPortValue(VCC_PORT, Value.FALSE);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74253.L1_Y));

    state.setPortValue(VCC_PORT, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74253.L1_Y));

    state.setPortValue(GND_PORT, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74253.L1_Y));
  }

  /** Enables both multiplexers and drives every data input low. */
  private static TtlTestInstanceState enabled(Ttl74253 mux, int select) {
    final var state = new TtlTestInstanceState(mux, false);
    set(state, Ttl74253.OE1, Value.FALSE);
    set(state, Ttl74253.OE2, Value.FALSE);
    set(state, Ttl74253.S1, (select & 2) == 0 ? Value.FALSE : Value.TRUE);
    set(state, Ttl74253.S0, (select & 1) == 0 ? Value.FALSE : Value.TRUE);
    for (final var channel : CHANNELS) {
      for (var source = 0; source < 4; source++) {
        set(state, channel[source], Value.FALSE);
      }
    }
    return state;
  }

  /** Drives the addressed input to {@code selectedHigh} and the other three to the opposite. */
  private static void driveOppositeUnselected(
      TtlTestInstanceState state, int channel, int select, boolean selectedHigh) {
    for (var source = 0; source < 4; source++) {
      final var high = source == select ? selectedHigh : !selectedHigh;
      set(state, CHANNELS[channel][source], high ? Value.TRUE : Value.FALSE);
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74253.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74253.pinNrToPortNr(dsPinNr), value);
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74253.pinNrToPortNr(dsPinNr));
  }
}
