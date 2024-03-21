package opc.models.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConsumerLevelCheckDimensionModel {
    private String key;
    private String value;

    public static ConsumerLevelCheckDimensionModel geolocationCheckDimensionModel(final String consumerLevel){
      return ConsumerLevelCheckDimensionModel.builder()
          .key("CONSUMER_PERFORM_GEOLOCATION_CHECK")
          .value(consumerLevel)
          .build();
    }

  public static ConsumerLevelCheckDimensionModel nationalityCheckDimensionModel(final String consumerLevel){
    return ConsumerLevelCheckDimensionModel.builder()
        .key("CONSUMER_PERFORM_NATIONALITY_CHECK")
        .value(consumerLevel)
        .build();
  }
}
