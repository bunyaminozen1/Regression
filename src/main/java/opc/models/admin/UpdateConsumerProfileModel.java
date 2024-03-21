package opc.models.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UpdateConsumerProfileModel {

    private final List<String> allowedCountries;
    private final boolean hasAllowedCountries;
}
