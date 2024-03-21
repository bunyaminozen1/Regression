package opc.models.admin;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeactivateConsumerLevelCheckModel {
    private String propertyKey;
    private List<ConsumerLevelCheckDimensionModel> dimensionValues;

    public static DeactivateConsumerLevelCheckModel deactivateNationalityCheck (final String consumerLevel){
        return DeactivateConsumerLevelCheckModel.builder()
            .propertyKey("CONSUMER_LEVEL_CHECKS")
            .dimensionValues(List.of(ConsumerLevelCheckDimensionModel.nationalityCheckDimensionModel(consumerLevel)))
            .build();
    }

    public static DeactivateConsumerLevelCheckModel deactivateGeolocationCheck (final String consumerLevel){
        return DeactivateConsumerLevelCheckModel.builder()
            .propertyKey("CONSUMER_LEVEL_CHECKS")
            .dimensionValues(List.of(ConsumerLevelCheckDimensionModel.geolocationCheckDimensionModel(consumerLevel)))
            .build();
    }
}
