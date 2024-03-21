package opc.models.sumsub;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;

@Data
@Builder
public class LegalEntityApplicantModel {
  private final String email;
  private final LegalEntityInfoModel info;
  private final String lang;

  public static LegalEntityApplicantModel defaultLegalEntityApplicantModel(){
    return LegalEntityApplicantModel.builder()
        .email(RandomStringUtils.randomAlphanumeric(10)+"@test.io")
        .info(LegalEntityInfoModel.defaultLegalEntityInfoModel())
        .lang("en")
        .build();
  }
}
