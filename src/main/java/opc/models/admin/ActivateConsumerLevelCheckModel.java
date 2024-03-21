package opc.models.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivateConsumerLevelCheckModel {

  private String check;
  private String level;

  public static ActivateConsumerLevelCheckModel activateGeolocationCheckModel (final String consumerLevel){
    return ActivateConsumerLevelCheckModel.builder()
        .check("CONSUMER_PERFORM_GEOLOCATION_CHECK")
        .level(consumerLevel)
        .build();
  }

  public static ActivateConsumerLevelCheckModel activateNationalityCheckModel (final String consumerLevel){
    return ActivateConsumerLevelCheckModel.builder()
        .check("CONSUMER_PERFORM_NATIONALITY_CHECK")
        .level(consumerLevel)
        .build();
  }
}
