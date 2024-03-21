package opc.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.enums.opc.ServiceType;
import org.apache.commons.lang3.RandomStringUtils;
@Builder
@Getter
@Setter

public class CreateThirdPartyRegistryModel {

    private String providerKey;
    private String providerName;

    public static CreateThirdPartyRegistryModel DefaultCreateThirdPartyRegistryModel() {

        return CreateThirdPartyRegistryModel
                .builder()
                .providerKey(RandomStringUtils.randomAlphabetic(24))
                .providerName(RandomStringUtils.randomAlphabetic(24))
                .build();
    }

    public static CreateThirdPartyRegistryModel DefaultCreateThirdPartyRegistryModel(final String providerKey) {

        return CreateThirdPartyRegistryModel
                .builder()
                .providerKey(providerKey)
                .providerName(RandomStringUtils.randomAlphabetic(24))
                .build();
    }
}