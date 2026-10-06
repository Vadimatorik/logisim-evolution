/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.List;

/** Built-in 74-series TTL devices. */
public class TtlLibrary extends Library {
  /**
   * Unique identifier of the library, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "TTL";

  private static final FactoryDescription[] DESCRIPTIONS = {
      new FactoryDescription(Ttl7400.class, S.getter("TTL7400"), "ttl.gif"),
      new FactoryDescription(Ttl7402.class, S.getter("TTL7402"), "ttl.gif"),
      new FactoryDescription(Ttl7403.class, S.getter("TTL7403"), "ttl.gif"),
      new FactoryDescription(Ttl7404.class, S.getter("TTL7404"), "ttl.gif"),
      new FactoryDescription(Ttl7408.class, S.getter("TTL7408"), "ttl.gif"),
      new FactoryDescription(Ttl7409.class, S.getter("TTL7409"), "ttl.gif"),
      new FactoryDescription(Ttl7410.class, S.getter("TTL7410"), "ttl.gif"),
      new FactoryDescription(Ttl7411.class, S.getter("TTL7411"), "ttl.gif"),
      new FactoryDescription(Ttl7412.class, S.getter("TTL7412"), "ttl.gif"),
      new FactoryDescription(Ttl7415.class, S.getter("TTL7415"), "ttl.gif"),
      new FactoryDescription(Ttl7413.class, S.getter("TTL7413"), "ttl.gif"),
      new FactoryDescription(Ttl7414.class, S.getter("TTL7414"), "ttl.gif"),
      new FactoryDescription(Ttl741G14.class, S.getter("TTL741G14"), "ttl.gif"),
      new FactoryDescription(Ttl7416.class, S.getter("TTL7416"), "ttl.gif"),
      new FactoryDescription(Ttl7417.class, S.getter("TTL7417"), "ttl.gif"),
      new FactoryDescription(Ttl7418.class, S.getter("TTL7418"), "ttl.gif"),
      new FactoryDescription(Ttl7419.class, S.getter("TTL7419"), "ttl.gif"),
      new FactoryDescription(Ttl7420.class, S.getter("TTL7420"), "ttl.gif"),
      new FactoryDescription(Ttl7421.class, S.getter("TTL7421"), "ttl.gif"),
      new FactoryDescription(Ttl7424.class, S.getter("TTL7424"), "ttl.gif"),
      new FactoryDescription(Ttl74249.class, S.getter("TTL74249"), "ttl.gif"),
      new FactoryDescription(Ttl7426.class, S.getter("TTL7426"), "ttl.gif"),
      new FactoryDescription(Ttl7427.class, S.getter("TTL7427"), "ttl.gif"),
      new FactoryDescription(Ttl7428.class, S.getter("TTL7428"), "ttl.gif"),
      new FactoryDescription(Ttl7430.class, S.getter("TTL7430"), "ttl.gif"),
      new FactoryDescription(Ttl7432.class, S.getter("TTL7432"), "ttl.gif"),
      new FactoryDescription(Ttl7433.class, S.getter("TTL7433"), "ttl.gif"),
      new FactoryDescription(Ttl7434.class, S.getter("TTL7434"), "ttl.gif"),
      new FactoryDescription(Ttl7436.class, S.getter("TTL7436"), "ttl.gif"),
      new FactoryDescription(Ttl7437.class, S.getter("TTL7437"), "ttl.gif"),
      new FactoryDescription(Ttl7438.class, S.getter("TTL7438"), "ttl.gif"),
      new FactoryDescription(Ttl7442.class, S.getter("TTL7442"), "ttl.gif"),
      new FactoryDescription(Ttl7443.class, S.getter("TTL7443"), "ttl.gif"),
      new FactoryDescription(Ttl7444.class, S.getter("TTL7444"), "ttl.gif"),
      new FactoryDescription(Ttl7447.class, S.getter("TTL7447"), "ttl.gif"),
      new FactoryDescription(Ttl7451.class, S.getter("TTL7451"), "ttl.gif"),
      new FactoryDescription(Ttl7454.class, S.getter("TTL7454"), "ttl.gif"),
      new FactoryDescription(Ttl7458.class, S.getter("TTL7458"), "ttl.gif"),
      new FactoryDescription(Ttl7464.class, S.getter("TTL7464"), "ttl.gif"),
      new FactoryDescription(Ttl7474.class, S.getter("TTL7474"), "ttl.gif"),
      new FactoryDescription(Ttl741G74.class, S.getter("TTL741G74"), "ttl.gif"),
      new FactoryDescription(Ttl7476.class, S.getter("TTL7476"), "ttl.gif"),
      new FactoryDescription(Ttl7485.class, S.getter("TTL7485"), "ttl.gif"),
      new FactoryDescription(Ttl7486.class, S.getter("TTL7486"), "ttl.gif"),
      new FactoryDescription(Ttl7487.class, S.getter("TTL7487"), "ttl.gif"),
      new FactoryDescription(Ttl7493.class, S.getter("TTL7493"), "ttl.gif"),
      new FactoryDescription(Ttl74109.class, S.getter("TTL74109"), "ttl.gif"),
      new FactoryDescription(Ttl74113.class, S.getter("TTL74113"), "ttl.gif"),
      new FactoryDescription(Ttl74122.class, S.getter("TTL74122"), "ttl.gif"),
      new FactoryDescription(Ttl74123.class, S.getter("TTL74123"), "ttl.gif"),
      new FactoryDescription(Ttl74125.class, S.getter("TTL74125"), "ttl.gif"),
      new FactoryDescription(Ttl74136.class, S.getter("TTL74136"), "ttl.gif"),
      new FactoryDescription(Ttl74138.class, S.getter("TTL74138"), "ttl.gif"),
      new FactoryDescription(Ttl74139.class, S.getter("TTL74139"), "ttl.gif"),
      new FactoryDescription(Ttl74145.class, S.getter("TTL74145"), "ttl.gif"),
      new FactoryDescription(Ttl74147.class, S.getter("TTL74147"), "ttl.gif"),
      new FactoryDescription(Ttl74148.class, S.getter("TTL74148"), "ttl.gif"),
      new FactoryDescription(Ttl74150.class, S.getter("TTL74150"), "ttl.gif"),
      new FactoryDescription(Ttl74151.class, S.getter("TTL74151"), "ttl.gif"),
      new FactoryDescription(Ttl74152.class, S.getter("TTL74152"), "ttl.gif"),
      new FactoryDescription(Ttl74153.class, S.getter("TTL74153"), "ttl.gif"),
      new FactoryDescription(Ttl74154.class, S.getter("TTL74154"), "ttl.gif"),
      new FactoryDescription(Ttl74155.class, S.getter("TTL74155"), "ttl.gif"),
      new FactoryDescription(Ttl74157.class, S.getter("TTL74157"), "ttl.gif"),
      new FactoryDescription(Ttl74158.class, S.getter("TTL74158"), "ttl.gif"),
      new FactoryDescription(Ttl74160.class, S.getter("TTL74160"), "ttl.gif"),
      new FactoryDescription(Ttl74161.class, S.getter("TTL74161"), "ttl.gif"),
      new FactoryDescription(Ttl74162.class, S.getter("TTL74162"), "ttl.gif"),
      new FactoryDescription(Ttl74163.class, S.getter("TTL74163"), "ttl.gif"),
      new FactoryDescription(Ttl74164.class, S.getter("TTL74164"), "ttl.gif"),
      new FactoryDescription(Ttl74165.class, S.getter("TTL74165"), "ttl.gif"),
      new FactoryDescription(Ttl74166.class, S.getter("TTL74166"), "ttl.gif"),
      new FactoryDescription(Ttl74168.class, S.getter("TTL74168"), "ttl.gif"),
      new FactoryDescription(Ttl74169.class, S.getter("TTL74169"), "ttl.gif"),
      new FactoryDescription(Ttl74173.class, S.getter("TTL74173"), "ttl.gif"),
      new FactoryDescription(Ttl74175.class, S.getter("TTL74175"), "ttl.gif"),
      new FactoryDescription(Ttl74176.class, S.getter("TTL74176"), "ttl.gif"),
      new FactoryDescription(Ttl74177.class, S.getter("TTL74177"), "ttl.gif"),
      new FactoryDescription(Ttl74178.class, S.getter("TTL74178"), "ttl.gif"),
      new FactoryDescription(Ttl74181.class, S.getter("TTL74181"), "ttl.gif"),
      new FactoryDescription(Ttl74182.class, S.getter("TTL74182"), "ttl.gif"),
      new FactoryDescription(Ttl74192.class, S.getter("TTL74192"), "ttl.gif"),
      new FactoryDescription(Ttl74193.class, S.getter("TTL74193"), "ttl.gif"),
      new FactoryDescription(Ttl74194.class, S.getter("TTL74194"), "ttl.gif"),
      new FactoryDescription(Ttl741G00.class, S.getter("TTL741G00"), "ttl.gif"),
      new FactoryDescription(Ttl74195.class, S.getter("TTL74195"), "ttl.gif"),
      new FactoryDescription(Ttl74196.class, S.getter("TTL74196"), "ttl.gif"),
      new FactoryDescription(Ttl74197.class, S.getter("TTL74197"), "ttl.gif"),
      new FactoryDescription(Ttl74198.class, S.getter("TTL74198"), "ttl.gif"),
      new FactoryDescription(Ttl74237.class, S.getter("TTL74237"), "ttl.gif"),
      new FactoryDescription(Ttl74293.class, S.getter("TTL74293"), "ttl.gif"),
      new FactoryDescription(Ttl74240.class, S.getter("TTL74240"), "ttl.gif"),
      new FactoryDescription(Ttl74241.class, S.getter("TTL74241"), "ttl.gif"),
      new FactoryDescription(Ttl74244.class, S.getter("TTL74244"), "ttl.gif"),
      new FactoryDescription(Ttl74245.class, S.getter("TTL74245"), "ttl.gif"),
      new FactoryDescription(Ttl74247.class, S.getter("TTL74247"), "ttl.gif"),
      new FactoryDescription(Ttl74251.class, S.getter("TTL74251"), "ttl.gif"),
      new FactoryDescription(Ttl74258.class, S.getter("TTL74258"), "ttl.gif"),
      new FactoryDescription(Ttl74259.class, S.getter("TTL74259"), "ttl.gif"),
      new FactoryDescription(Ttl74266.class, S.getter("TTL74266"), "ttl.gif"),
      new FactoryDescription(Ttl74273.class, S.getter("TTL74273"), "ttl.gif"),
      new FactoryDescription(Ttl74279.class, S.getter("TTL74279"), "ttl.gif"),
      new FactoryDescription(Ttl74280.class, S.getter("TTL74280"), "ttl.gif"),
      new FactoryDescription(Ttl74283.class, S.getter("TTL74283"), "ttl.gif"),
      new FactoryDescription(Ttl74290.class, S.getter("TTL74290"), "ttl.gif"),
      new FactoryDescription(Ttl74298.class, S.getter("TTL74298"), "ttl.gif"),
      new FactoryDescription(Ttl74299.class, S.getter("TTL74299"), "ttl.gif"),
      new FactoryDescription(Ttl74352.class, S.getter("TTL74352"), "ttl.gif"),
      new FactoryDescription(Ttl74367.class, S.getter("TTL74367"), "ttl.gif"),
      new FactoryDescription(Ttl74373.class, S.getter("TTL74373"), "ttl.gif"),
      new FactoryDescription(Ttl74374.class, S.getter("TTL74374"), "ttl.gif"),
      new FactoryDescription(Ttl74375.class, S.getter("TTL74375"), "ttl.gif"),
      new FactoryDescription(Ttl74377.class, S.getter("TTL74377"), "ttl.gif"),
      new FactoryDescription(Ttl74381.class, S.getter("TTL74381"), "ttl.gif"),
      new FactoryDescription(Ttl74386.class, S.getter("TTL74386"), "ttl.gif"),
      new FactoryDescription(Ttl74393.class, S.getter("TTL74393"), "ttl.gif"),
      new FactoryDescription(Ttl74399.class, S.getter("TTL74399"), "ttl.gif"),
      new FactoryDescription(Ttl74541.class, S.getter("TTL74541"), "ttl.gif"),
      new FactoryDescription(Ttl74670.class, S.getter("TTL74670"), "ttl.gif"),
      new FactoryDescription(Ttl7440102.class, S.getter("TTL7440102"), "ttl.gif"),
      new FactoryDescription(Ttl744015.class, S.getter("TTL744015"), "ttl.gif"),
      new FactoryDescription(Ttl744020.class, S.getter("TTL744020"), "ttl.gif"),
      new FactoryDescription(Ttl744024.class, S.getter("TTL744024"), "ttl.gif"),
      new FactoryDescription(Ttl744049.class, S.getter("TTL744049"), "ttl.gif"),
      new FactoryDescription(Ttl744050.class, S.getter("TTL744050"), "ttl.gif"),
      new FactoryDescription(Ttl744060.class, S.getter("TTL744060"), "ttl.gif"),
      new FactoryDescription(Ttl744072.class, S.getter("TTL744072"), "ttl.gif"),
      new FactoryDescription(Ttl744078.class, S.getter("TTL744078"), "ttl.gif"),
      new FactoryDescription(Ttl747266.class, S.getter("TTL747266"), "ttl.gif"),
      new FactoryDescription(Ttl7440103.class, S.getter("TTL7440103"), "ttl.gif"),
  };

  static final Attribute<Boolean> VCC_GND =
      Attributes.forBoolean("VccGndPorts", S.getter("VccGndPorts"));
  static final Attribute<Boolean> DRAW_INTERNAL_STRUCTURE =
      Attributes.forBoolean("ShowInternalStructure", S.getter("ShowInternalStructure"));

  private List<Tool> tools = null;

  @Override
  public List<? extends Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(TtlLibrary.class, DESCRIPTIONS);
    }
    return tools;
  }
}
