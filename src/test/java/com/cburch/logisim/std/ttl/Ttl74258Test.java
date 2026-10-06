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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC258 quad inverting selector with three-state outputs. */
class Ttl74258Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74258();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74258.S, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.I0_1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.I1_1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.Y1, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.I0_2, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.I1_2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.Y2, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.Y3, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.I1_3, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.I0_3, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.Y4, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.I1_4, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.I0_4, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74258.OE, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void selectIsNotAnEdgeClock() {
    assertFalse(new Ttl74258().checkForGatedClocks(null));
  }

  @Test
  void outputsAreThreeStateDrivers() {
    final var gate = new Ttl74258();
    assertTrue(gate.hasThreeStateDrivers(null));
    assertTrue(gate.hasThreeStateDrivers(gate.createAttributeSet()));
  }

  @Test
  void enabledSelectorInvertsTheChosenSource() {
    final var gate = new Ttl74258();

    for (var channel = 0; channel < Ttl74258.Y.length; channel++) {
      for (var selectHigh : new boolean[] {false, true}) {
        for (var dataHigh : new boolean[] {false, true}) {
          final var state = enabled(gate, selectHigh);
          for (var other = 0; other < Ttl74258.Y.length; other++) {
            set(state, Ttl74258.I0[other], Value.FALSE);
            set(state, Ttl74258.I1[other], Value.FALSE);
          }
          final var selected = selectHigh ? Ttl74258.I1[channel] : Ttl74258.I0[channel];
          final var unselected = selectHigh ? Ttl74258.I0[channel] : Ttl74258.I1[channel];
          set(state, selected, dataHigh ? Value.TRUE : Value.FALSE);
          set(state, unselected, dataHigh ? Value.FALSE : Value.TRUE);
          gate.propagate(state);

          assertEquals(dataHigh ? Value.FALSE : Value.TRUE, output(state, channel));
          for (var other = 0; other < Ttl74258.Y.length; other++) {
            if (other != channel) {
              assertEquals(Value.TRUE, output(state, other));
            }
          }
        }
      }
    }
  }

  @Test
  void unselectedInputDoesNotAffectTheOutput() {
    final var gate = new Ttl74258();
    final var ignored = new Value[] {Value.FALSE, Value.TRUE, Value.UNKNOWN, Value.ERROR};

    for (final var value : ignored) {
      final var selectSource0 = enabled(gate, false);
      set(selectSource0, Ttl74258.I0_1, Value.TRUE);
      set(selectSource0, Ttl74258.I1_1, value);
      gate.propagate(selectSource0);
      assertEquals(Value.FALSE, output(selectSource0, 0));

      final var selectSource1 = enabled(gate, true);
      set(selectSource1, Ttl74258.I1_2, Value.FALSE);
      set(selectSource1, Ttl74258.I0_2, value);
      gate.propagate(selectSource1);
      assertEquals(Value.TRUE, output(selectSource1, 1));
    }
  }

  @Test
  void outputEnableHighReleasesEveryOutput() {
    final var gate = new Ttl74258();
    final var state = new TtlTestInstanceState(gate, false);
    set(state, Ttl74258.OE, Value.TRUE);
    set(state, Ttl74258.S, Value.ERROR);
    for (var channel = 0; channel < Ttl74258.Y.length; channel++) {
      set(state, Ttl74258.I0[channel], Value.ERROR);
      set(state, Ttl74258.I1[channel], Value.ERROR);
    }
    gate.propagate(state);

    assertOutputs(state, Value.UNKNOWN);
  }

  @Test
  void unknownSelectUsesTheValueBothSourcesShare() {
    final var gate = new Ttl74258();

    final var bothLow = enabled(gate, Value.UNKNOWN);
    driveAll(bothLow, Value.FALSE, Value.FALSE);
    gate.propagate(bothLow);
    assertOutputs(bothLow, Value.TRUE);

    final var bothHigh = enabled(gate, Value.UNKNOWN);
    driveAll(bothHigh, Value.TRUE, Value.TRUE);
    gate.propagate(bothHigh);
    assertOutputs(bothHigh, Value.FALSE);

    final var bothUnknown = enabled(gate, Value.UNKNOWN);
    driveAll(bothUnknown, Value.UNKNOWN, Value.UNKNOWN);
    gate.propagate(bothUnknown);
    assertOutputs(bothUnknown, Value.UNKNOWN);

    final var bothError = enabled(gate, Value.UNKNOWN);
    driveAll(bothError, Value.ERROR, Value.ERROR);
    gate.propagate(bothError);
    assertOutputs(bothError, Value.ERROR);
  }

  @Test
  void disagreeingSourcesMakeAnUnknownSelectUnknown() {
    final var gate = new Ttl74258();
    final var state = enabled(gate, Value.UNKNOWN);
    set(state, Ttl74258.I0_1, Value.FALSE);
    set(state, Ttl74258.I1_1, Value.TRUE);
    set(state, Ttl74258.I0_3, Value.UNKNOWN);
    set(state, Ttl74258.I1_3, Value.FALSE);
    gate.propagate(state);

    assertEquals(Value.UNKNOWN, output(state, 0));
    assertEquals(Value.UNKNOWN, output(state, 2));
  }

  @Test
  void errorOnSelectOrTheSelectedInputIsAnError() {
    final var gate = new Ttl74258();

    final var selectError = enabled(gate, Value.ERROR);
    driveAll(selectError, Value.FALSE, Value.FALSE);
    gate.propagate(selectError);
    assertOutputs(selectError, Value.ERROR);

    final var selectedError = enabled(gate, false);
    set(selectedError, Ttl74258.I0_4, Value.ERROR);
    set(selectedError, Ttl74258.I1_4, Value.FALSE);
    gate.propagate(selectedError);
    assertEquals(Value.ERROR, output(selectedError, 3));

    final var onePossibleError = enabled(gate, Value.UNKNOWN);
    set(onePossibleError, Ttl74258.I0_2, Value.ERROR);
    set(onePossibleError, Ttl74258.I1_2, Value.FALSE);
    gate.propagate(onePossibleError);
    assertEquals(Value.ERROR, output(onePossibleError, 1));
  }

  @Test
  void outputEnableThatIsNotHighOrLowDoesNotDrive() {
    final var gate = new Ttl74258();

    final var unknownEnable = new TtlTestInstanceState(gate, false);
    set(unknownEnable, Ttl74258.OE, Value.UNKNOWN);
    set(unknownEnable, Ttl74258.S, Value.FALSE);
    driveAll(unknownEnable, Value.TRUE, Value.FALSE);
    gate.propagate(unknownEnable);
    assertOutputs(unknownEnable, Value.UNKNOWN);

    final var errorEnable = new TtlTestInstanceState(gate, false);
    set(errorEnable, Ttl74258.OE, Value.ERROR);
    set(errorEnable, Ttl74258.S, Value.FALSE);
    driveAll(errorEnable, Value.FALSE, Value.FALSE);
    gate.propagate(errorEnable);
    assertOutputs(errorEnable, Value.ERROR);
  }

  @Test
  void invalidExposedPowerInputsReleaseTheOutputs() {
    final var gate = new Ttl74258();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    set(state, Ttl74258.OE, Value.FALSE);
    set(state, Ttl74258.S, Value.FALSE);
    driveAll(state, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.FALSE);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.FALSE);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN);
  }

  private static TtlTestInstanceState enabled(Ttl74258 gate, boolean selectHigh) {
    return enabled(gate, selectHigh ? Value.TRUE : Value.FALSE);
  }

  private static TtlTestInstanceState enabled(Ttl74258 gate, Value select) {
    final var state = new TtlTestInstanceState(gate, false);
    set(state, Ttl74258.OE, Value.FALSE);
    set(state, Ttl74258.S, select);
    return state;
  }

  private static void driveAll(TtlTestInstanceState state, Value source0, Value source1) {
    for (var channel = 0; channel < Ttl74258.Y.length; channel++) {
      set(state, Ttl74258.I0[channel], source0);
      set(state, Ttl74258.I1[channel], source1);
    }
  }

  private static void assertOutputs(TtlTestInstanceState state, Value expected) {
    for (var channel = 0; channel < Ttl74258.Y.length; channel++) {
      assertEquals(expected, output(state, channel));
    }
  }

  private static Value output(TtlTestInstanceState state, int channel) {
    return state.getPortValue(Ttl74258.pinNrToPortNr(Ttl74258.Y[channel]));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74258.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74258.pinNrToPortNr(dsPinNr), value);
  }
}
