package opc.junit.secure;

import com.google.common.collect.ImmutableMap;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;

public class ConsumerDeTokenizeTests extends AbstractDeTokenizeTests {

    @Override
    protected String getAuthToken() { return consumerAuthenticationToken; }

    @Override
    protected String getPrepaidManagedCardsProfileId() {
        return consumerPrepaidManagedCardsProfileId;
    }

    @Override
    protected String getCurrency() {
        return consumerCurrency;
    }

    @Override
    protected String getAssociateRandom() {
        return consumerAssociateRandom;
    }

    @BeforeAll
    public static void Setup() {
        consumerSetup();
    }

    @AfterAll
    public static void Reset(){

        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), true,
                        SecurityModelConfiguration.CARD_NUMBER.name(), true);

        updateSecurityModel(securityModelConfig);
    }

    private static void consumerSetup() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        consumerAssociateRandom = associate(consumerAuthenticationToken);
    }
}
