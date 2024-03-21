package opc.models.sumsub;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;

@Data
@Builder
public class LegalEntityDirectorInfoModel {

      final String companyName;
      final String registrationNumber;
      final String country;
      final String incorporatedOn;

  public static LegalEntityDirectorInfoModel defaultCompanyInfoModel(){
    return LegalEntityDirectorInfoModel.builder()
        .companyName(RandomStringUtils.randomAlphabetic(6))
        .registrationNumber(RandomStringUtils.randomNumeric(9))
        .country("MLT")
        .incorporatedOn("2020-01-01")
        .build();
  }
}
