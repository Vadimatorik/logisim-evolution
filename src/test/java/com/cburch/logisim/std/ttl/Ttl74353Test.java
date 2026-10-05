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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

class Ttl74353Test {
  /** Each row is C0, C1, C2, C3, the output, and the output strobe of one multiplexer. */
  private static final byte[][] CHANNELS = {
    {Ttl74353.C1_0, Ttl74353.C1_1, Ttl74353.C1_2, Ttl74353.C1_3, Ttl74353.Y1, Ttl74353.G1},
    {Ttl74353.C2_0, Ttl74353.C2_1, Ttl74353.C2_2, Ttl74353.C2_3, Ttl74353.Y2, Ttl74353.G2}
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var mux = new Ttl74353();
    assertTrue(mux.hasThreeStateDrivers(null));
    assertTrue(mux.hasThreeStateDrivers(mux.createAttributeSet()));

    final var hiddenPower = createInstance(mux, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74353.G1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.C1_3, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.C1_2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.C1_1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.C1_0, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.Y1, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.Y2, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.C2_0, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.C2_1, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.C2_2, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.C2_3, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.A, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74353.G2, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(mux, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachChannelInvertsTheSelectedSourceAndIgnoresTheOthers() {
    final var mux = new Ttl74353();

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
              selectedHigh ? Value.FALSE : Value.TRUE,
              output(state, CHANNELS[channel][4]),
              "channel " + channel + " select " + select);
        }
      }
    }
  }

  @Test
  void eachOutputStrobeReleasesOnlyItsOwnOutput() {
    final var mux = new Ttl74353();
    final var state = enabled(mux, 0);
    driveOppositeUnselected(state, 0, 0, true);
    driveOppositeUnselected(state, 1, 0, false);

    set(state, Ttl74353.G1, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74353.Y1));
    assertEquals(Value.TRUE, output(state, Ttl74353.Y2));

    set(state, Ttl74353.G1, Value.FALSE);
    set(state, Ttl74353.G2, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.FALSE, output(state, Ttl74353.Y1));
    assertEquals(Value.UNKNOWN, output(state, Ttl74353.Y2));
  }

  @Test
  void highOutputStrobeReleasesItsOutput() {
    final var mux = new Ttl74353();
    final var state = enabled(mux, 1);
    set(state, Ttl74353.G1, Value.TRUE);
    set(state, Ttl74353.G2, Value.TRUE);
    set(state, Ttl74353.B, Value.UNKNOWN);
    set(state, Ttl74353.A, Value.ERROR);
    for (final var channel : CHANNELS) {
      for (var source = 0; source < 4; source++) {
        set(state, channel[source], Value.ERROR);
      }
    }
    mux.propagate(state);

    assertEquals(Value.UNKNOWN, output(state, Ttl74353.Y1));
    assertEquals(Value.UNKNOWN, output(state, Ttl74353.Y2));
  }

  @Test
  void unknownSelectPassesTheInvertedValueTheCandidatesShare() {
    final var mux = new Ttl74353();
    final var state = enabled(mux, 0);
    set(state, Ttl74353.A, Value.UNKNOWN);
    set(state, Ttl74353.C1_0, Value.TRUE);
    set(state, Ttl74353.C1_1, Value.TRUE);
    set(state, Ttl74353.C1_2, Value.ERROR);
    set(state, Ttl74353.C2_0, Value.FALSE);
    set(state, Ttl74353.C2_1, Value.FALSE);
    set(state, Ttl74353.C2_3, Value.ERROR);
    mux.propagate(state);

    assertEquals(Value.FALSE, output(state, Ttl74353.Y1));
    assertEquals(Value.TRUE, output(state, Ttl74353.Y2));
  }

  @Test
  void unknownSelectReleasesTheOutputWhenTheCandidatesDiffer() {
    final var mux = new Ttl74353();
    final var lowOrder = enabled(mux, 0);
    set(lowOrder, Ttl74353.A, Value.UNKNOWN);
    set(lowOrder, Ttl74353.C1_0, Value.FALSE);
    set(lowOrder, Ttl74353.C1_1, Value.TRUE);
    mux.propagate(lowOrder);
    assertEquals(Value.UNKNOWN, output(lowOrder, Ttl74353.Y1));

    final var both = enabled(mux, 0);
    set(both, Ttl74353.B, Value.UNKNOWN);
    set(both, Ttl74353.A, Value.UNKNOWN);
    set(both, Ttl74353.C2_0, Value.TRUE);
    set(both, Ttl74353.C2_1, Value.TRUE);
    set(both, Ttl74353.C2_2, Value.TRUE);
    set(both, Ttl74353.C2_3, Value.FALSE);
    mux.propagate(both);
    assertEquals(Value.UNKNOWN, output(both, Ttl74353.Y2));
  }

  @Test
  void errorOnSelectMattersOnlyWhenTheCandidatesDiffer() {
    final var mux = new Ttl74353();
    final var agreed = enabled(mux, 2);
    set(agreed, Ttl74353.B, Value.ERROR);
    set(agreed, Ttl74353.C1_0, Value.TRUE);
    set(agreed, Ttl74353.C1_2, Value.TRUE);
    set(agreed, Ttl74353.C1_1, Value.ERROR);
    mux.propagate(agreed);
    assertEquals(Value.FALSE, output(agreed, Ttl74353.Y1));

    final var split = enabled(mux, 2);
    set(split, Ttl74353.B, Value.ERROR);
    set(split, Ttl74353.C2_0, Value.FALSE);
    set(split, Ttl74353.C2_2, Value.TRUE);
    mux.propagate(split);
    assertEquals(Value.ERROR, output(split, Ttl74353.Y2));
  }

  @Test
  void selectedUnknownOrErrorIsInvertedAndTheOtherSourcesDoNot() {
    final var mux = new Ttl74353();
    final var fromSource2 = enabled(mux, 2);
    set(fromSource2, Ttl74353.C1_0, Value.ERROR);
    set(fromSource2, Ttl74353.C1_1, Value.ERROR);
    set(fromSource2, Ttl74353.C1_2, Value.UNKNOWN);
    set(fromSource2, Ttl74353.C1_3, Value.ERROR);
    mux.propagate(fromSource2);
    assertEquals(Value.UNKNOWN, output(fromSource2, Ttl74353.Y1));

    final var fromSource0 = enabled(mux, 0);
    set(fromSource0, Ttl74353.C2_0, Value.FALSE);
    set(fromSource0, Ttl74353.C2_1, Value.ERROR);
    set(fromSource0, Ttl74353.C2_2, Value.ERROR);
    set(fromSource0, Ttl74353.C2_3, Value.ERROR);
    mux.propagate(fromSource0);
    assertEquals(Value.TRUE, output(fromSource0, Ttl74353.Y2));

    final var errorSelected = enabled(mux, 3);
    set(errorSelected, Ttl74353.C1_3, Value.ERROR);
    mux.propagate(errorSelected);
    assertEquals(Value.ERROR, output(errorSelected, Ttl74353.Y1));
  }

  @Test
  void uncertainOutputStrobeFollowsTheDisagreementOfFloatAndData() {
    final var mux = new Ttl74353();
    final var unknownEnable = enabled(mux, 0);
    set(unknownEnable, Ttl74353.G1, Value.UNKNOWN);
    set(unknownEnable, Ttl74353.C1_0, Value.TRUE);
    mux.propagate(unknownEnable);
    assertEquals(Value.UNKNOWN, output(unknownEnable, Ttl74353.Y1));

    final var errorEnable = enabled(mux, 0);
    set(errorEnable, Ttl74353.G2, Value.ERROR);
    set(errorEnable, Ttl74353.C2_0, Value.TRUE);
    mux.propagate(errorEnable);
    assertEquals(Value.ERROR, output(errorEnable, Ttl74353.Y2));

    final var errorData = enabled(mux, 1);
    set(errorData, Ttl74353.G1, Value.UNKNOWN);
    set(errorData, Ttl74353.C1_1, Value.ERROR);
    mux.propagate(errorData);
    assertEquals(Value.ERROR, output(errorData, Ttl74353.Y1));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var mux = new Ttl74353();
    final var state = new TtlTestInstanceState(mux, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    set(state, Ttl74353.G1, Value.FALSE);
    set(state, Ttl74353.G2, Value.FALSE);
    set(state, Ttl74353.B, Value.FALSE);
    set(state, Ttl74353.A, Value.FALSE);
    set(state, Ttl74353.C1_0, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.FALSE, output(state, Ttl74353.Y1));

    state.setPortValue(VCC_PORT, Value.FALSE);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74353.Y1));

    state.setPortValue(VCC_PORT, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.FALSE, output(state, Ttl74353.Y1));

    state.setPortValue(GND_PORT, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74353.Y1));
  }

  /** Enables both multiplexers and drives every data input low. */
  private static TtlTestInstanceState enabled(Ttl74353 mux, int select) {
    final var state = new TtlTestInstanceState(mux, false);
    set(state, Ttl74353.G1, Value.FALSE);
    set(state, Ttl74353.G2, Value.FALSE);
    set(state, Ttl74353.B, (select & 2) == 0 ? Value.FALSE : Value.TRUE);
    set(state, Ttl74353.A, (select & 1) == 0 ? Value.FALSE : Value.TRUE);
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
    final var port = Ttl74353.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74353.pinNrToPortNr(dsPinNr), value);
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74353.pinNrToPortNr(dsPinNr));
  }
}
