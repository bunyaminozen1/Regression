package opc.helpers;

import java.util.List;
import opc.enums.opc.ResourceType;
import opc.models.multi.challenges.ChallengesModel;

public class ChallengesModelHelper {
  public static ChallengesModel issueChallengesModel(final ResourceType resourceType,
                                                     final List<String> resourceIds) {
    return ChallengesModel.builder().resourceType(resourceType.name().toLowerCase())
        .resourceIds(resourceIds).build();
  }

  public static ChallengesModel verifyChallengesModel(final ResourceType resourceType,
                                                      final String verificationCode) {
    return ChallengesModel.builder().resourceType(resourceType.name().toLowerCase())
        .verificationCode(verificationCode).build();
  }
}
