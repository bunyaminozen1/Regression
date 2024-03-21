package commons.config;

import commons.enums.PaymentModel;
import opc.enums.opc.UrlType;

public class PaymentRunDevEnvironment extends BaseEnvironment implements Configuration {

    private final String baseUrl;

    public PaymentRunDevEnvironment(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String getBaseUrl() {
        return "https://qa.weavr.io";
    }

    @Override
    public String getSecureServiceOrigin() {
        return "https://qa.weavr.io";
    }

    @Override
    public String getSimulatorUrl() {
        return "https://qa.weavr.io";
    }

    @Override
    public String getOldSimulatorUrl() {
        return "https://qa.weavr.io";
    }

    @Override
    public String getMailhogUrl() {
        return "https://qa.weavr.io/mailhog";
    }

    @Override
    public String getSumsubUrl() {
        return SUMSUB_URL;
    }

    @Override
    public String getSumsubToken() {
        return "sbx:GFIMoa221OXpLEbzUS7LAaAu.Etq7nAbdX78ppWEuO0AxMbIct3R62fMV";
    }

    @Override
    public String getSumsubSecret() {
        return "cLvDEpHfZfxefDqzWSD2McuE8LPexJGj";
    }

    @Override
    public String getDatabaseUrl() {
        return "jdbc:mysql://development.cbyqez0tz540.eu-central-1.rds.amazonaws.com:3306";
    }

    @Override
    public String getDatabaseUsername() {
        return "qa-db-user";
    }

    @Override
    public String getDatabasePassword() {
        return "xnu6bVZg876jBYaP";
    }

    @Override
    public String getMainTestEnvironment() {
        return "qa";
    }

    @Override
    public String getWebhookUrl() {
        return WEBHOOOK_URL;
    }

    @Override
    public String getVerificationCode() {
        return DEFAULT_VERIFICATION_CODE;
    }

    @Override
    public String getOtpVerificationCode() {
        return DEFAULT_VERIFICATION_CODE;
    }

    @Override
    public String getBaseUrl(UrlType urlType) {
        switch (urlType) {
            case BASE:
                return getBaseUrl();
            case SECURE_SERVICE:
                return getSecureServiceOrigin();
            case SIMULATOR:
                return getSimulatorUrl();
            case OLD_SIMULATOR:
                return getOldSimulatorUrl();
            case MAILHOG:
                return getMailhogUrl();
            case WEBHOOK:
                return getWebhookUrl();
            case SUMSUB:
                return getSumsubUrl();
            case WEBHOOK_CALLBACK:
                return getWebhookCallbackUrl();
            case PAYMENT_RUN:
                return getPaymentRunBaseUrl();
            case OPEN_BANKING:
                return getOpenBankingBaseUrl();
            default:
                throw new IllegalArgumentException("unknown url type");
        }
    }

    @Override
    public int getPaymentModelId(final PaymentModel paymentModel) {
        switch (paymentModel) {
            case DEFAULT_QA:
                return 32;
            case DEFAULT_PAYMENT_RUN:
                return 35;
            default:
                throw new IllegalArgumentException("Unknown payment model");
        }
    }

    @Override
    public String getWebhookCallbackUrl() {
        return baseUrl;
    }

    @Override
    public String getPaymentRunBaseUrl() {
        return String.join("", baseUrl, ":5009");
    }

    @Override
    public String getPluginId() {
        return "111810662340952353";
    }

    @Override
    public String getPaymentRunWebhookUrl() {
        return String.join("", getPaymentRunBaseUrl(), "/webhook/v1");
    }

    @Override
    public String getOpenBankingBaseUrl() {
        return String.join("", baseUrl, ":5008");
    }

    @Override
    public String getTestRunEnvironment() {
        return "dev";
    }

    @Override
    public String getFpiKey() {
        return "4Ro+E+dQptoBjTs0eu8BIQ==";
    }

    @Override
    public String getSweepingJobApiKey() {
        return "4d68a70cdb834fff1";
    }

    @Override
    public String getOpenBankingAuthKey() {
        return "EauMJR9Z8Dl88Y2aLQaIpvqWCjqX0v6s";
        }
}