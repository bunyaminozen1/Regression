package opc.models.admin;

import commons.config.ConfigHelper;
import commons.enums.PaymentModel;
import lombok.Builder;
import lombok.Data;
import opc.enums.opc.PluginStatus;
import org.apache.commons.lang3.RandomStringUtils;

import static opc.enums.opc.PluginStatus.getRandomWithExcludedPluginStatus;

@Data
@Builder
public class CreatePluginModel {
    private String name;
    private String code;
    private String description;
    private PluginStatus status;
    private String icon;
    private String webhookUrl;
    private String modelId;
    private String adminUrl;

    public static CreatePluginModel defaultCreatePluginModel() {
        return CreatePluginModel.builder()
                .name(RandomStringUtils.randomAlphabetic(8))
                .code(RandomStringUtils.randomAlphabetic(8))
                .description(RandomStringUtils.randomAlphabetic(8))
                .status(getRandomWithExcludedPluginStatus(PluginStatus.UNKNOWN))
                .icon(RandomStringUtils.randomAlphabetic(8))
                .webhookUrl("http://testurl.com/123")
                .modelId(String.valueOf(ConfigHelper.getEnvironmentConfiguration().getPaymentModelId(PaymentModel.DEFAULT_QA)))
                .build();
    }
}
