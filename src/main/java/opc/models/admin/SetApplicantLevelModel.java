
package opc.models.admin;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SetApplicantLevelModel {

  private String propertyKey;
  private String propertyValue;
  private Object json_value;
  private List<SetDimensionValueModel> dimensionValues;


  public static SetApplicantLevelModel setConsumerApplicantLevelProgramme(final String applicantLevel,
      final String programmeId, final String consumerType) {

    return SetApplicantLevelModel.builder()
        .propertyKey("CONSUMER_LEVEL")
        .propertyValue(applicantLevel)
        .dimensionValues(List.of(new SetDimensionValueModel("PROGRAMME_ID", programmeId),
            new SetDimensionValueModel("CONSUMER_TYPE", consumerType))
        ).build();
  }

  public static SetApplicantLevelModel setCorporateApplicantLevelProgramme(final String applicantLevel,
      final String programmeId, final String corporateType) {

    return SetApplicantLevelModel.builder()
        .propertyKey("CORPORATE_LEVEL")
        .propertyValue(applicantLevel)
        .dimensionValues(List.of(new SetDimensionValueModel("PROGRAMME_ID", programmeId),
            new SetDimensionValueModel("CORPORATE_TYPE", corporateType))
        ).build();
  }

  public static SetApplicantLevelModel setConsumerApplicantLevelForIdentity(final String applicantLevel,
                                                                 final String programmeId,
                                                                 final String consumerId, final String consumerType) {

    return SetApplicantLevelModel.builder()
            .propertyKey("CONSUMER_LEVEL")
            .propertyValue(applicantLevel)
            .dimensionValues(List.of(new SetDimensionValueModel("PROGRAMME_ID", programmeId),
                    new SetDimensionValueModel("CONSUMER_ID", consumerId),
                    new SetDimensionValueModel("CONSUMER_TYPE", consumerType))
            ).build();
  }

  public static SetApplicantLevelModel setCorporateApplicantLevelForIdentity(final String applicantLevel,
                                                                  final String programmeId,
                                                                  final String corporateId, final String corporateType) {

    return SetApplicantLevelModel.builder()
            .propertyKey("CORPORATE_LEVEL")
            .propertyValue(applicantLevel)
            .dimensionValues(List.of(new SetDimensionValueModel("PROGRAMME_ID", programmeId),
                    new SetDimensionValueModel("CORPORATE_ID", corporateId),
                    new SetDimensionValueModel("CORPORATE_TYPE", corporateType))
            ).build();
  }

    public static SetApplicantLevelModel setConsumerApplicantLevelForTenant(final String applicantLevel,
                                                                              final String consumerId, final String consumerType) {

        return SetApplicantLevelModel.builder()
                .propertyKey("CONSUMER_LEVEL")
                .propertyValue(applicantLevel)
                .dimensionValues(List.of(new SetDimensionValueModel("TENANT_ID", consumerId),
                        new SetDimensionValueModel("CONSUMER_TYPE", consumerType))
                ).build();
    }

    public static SetApplicantLevelModel setCorporateApplicantLevelForTenant(final String applicantLevel,
                                                                               final String corporateId, final String corporateType) {

        return SetApplicantLevelModel.builder()
                .propertyKey("CORPORATE_LEVEL")
                .propertyValue(applicantLevel)
                .dimensionValues(List.of(new SetDimensionValueModel("TENANT_ID", corporateId),
                        new SetDimensionValueModel("CORPORATE_TYPE", corporateType))
                ).build();
    }
}

