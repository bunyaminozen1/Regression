package opc.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
@Builder
@Getter
@Setter

public class UpdateThirdPartyRegistryModel {

    private String providerName;
    public static UpdateThirdPartyRegistryModel.UpdateThirdPartyRegistryModelBuilder UpdateThirdPartyRegistryModel() {

        return UpdateThirdPartyRegistryModel
                .builder()
                .providerName(RandomStringUtils.randomAlphabetic(5));
    }
}