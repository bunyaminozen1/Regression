
package opc.models.admin;

import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import opc.enums.opc.CompanyType;

@Data
@Builder
public class RegisteredCountriesDimension {


    private String key;

    private String value;

    public RegisteredCountriesDimension(final String key, final String value){
        this.key = key;
        this.value = value;
    }


    public static List<RegisteredCountriesDimension> defaultDimensionModel(String tenantId,String programmeId){
        return Arrays.asList(
            RegisteredCountriesDimension.builder().key("TenantIdDimension").value(tenantId).build(),
            RegisteredCountriesDimension.builder().key("ProgrammeId").value(programmeId).build());
    }
}
