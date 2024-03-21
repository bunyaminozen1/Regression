package opc.models.sumsub;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddLegalEntityDirectorModel {

  private final LegalEntityApplicantModel applicant;
  private final String type;
  private final boolean inRegistry;

  public static AddLegalEntityDirectorModel defaultAddLegalEntityDirectorModel(){
    return AddLegalEntityDirectorModel.builder()
        .applicant(LegalEntityApplicantModel.defaultLegalEntityApplicantModel())
        .type("director")
        .inRegistry(false)
        .build();
  }

}
