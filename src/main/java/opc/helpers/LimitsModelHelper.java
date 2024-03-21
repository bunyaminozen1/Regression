package opc.helpers;

import java.util.Collections;
import opc.enums.opc.IdentityType;
import opc.models.admin.LimitsApiContextModel;
import opc.models.admin.LimitsApiIntervalModel;
import opc.models.admin.LimitsApiModel;
import opc.models.admin.LimitsApiValueModel;

public class LimitsModelHelper {


  public static LimitsApiModel defaultSepaInstantLimitModel(final IdentityType identityType, final int maxSum) {
    return LimitsApiModel.builder()
        .setContext(LimitsApiContextModel.builder()
            .setSepaInstantDefaultLimitContext(identityType.getValue()).build())
        .setValue(Collections.singletonList(LimitsApiValueModel.builder()
            .setInterval(
                LimitsApiIntervalModel.builder()
                    .setTumbling("ALWAYS")
                    .build()
            )
            .setMaxCount(999999999)
            .setMaxSum(maxSum)
            .build()))
        .build();
  }


}
