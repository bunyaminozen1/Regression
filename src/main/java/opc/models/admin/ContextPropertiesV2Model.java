package opc.models.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ContextPropertiesV2Model {
    private List<Map<String, String>> context;
    private Map<String, String> value;

    public static ContextPropertiesV2Model setAlertNewUserAddedProperty(final String programmeId, final boolean isEnabled){
        return ContextPropertiesV2Model.builder()
                .context(List.of(Map.of("PROGRAMME_ID", programmeId)))
                .value(Map.of("stringValue", String.valueOf(isEnabled)))
                .build();
    }

    public static ContextPropertiesV2Model setEddCountriesProperty(final String innovatorId, final String eddCountries){
        return ContextPropertiesV2Model.builder()
                .context(List.of(Map.of("TENANT_ID", innovatorId)))
                .value(Map.of("stringValue", String.valueOf(eddCountries)))
                .build();
    }


}
