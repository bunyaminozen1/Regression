package opc.models.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UpdateAllowedCountriesModel {

    private final List<String> country;
    private final boolean hasCountry;
}
