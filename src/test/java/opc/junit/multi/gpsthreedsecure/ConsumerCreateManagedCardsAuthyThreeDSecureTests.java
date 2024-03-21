package opc.junit.multi.gpsthreedsecure;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerCreateManagedCardsAuthyThreeDSecureTests extends AbstractCreateManagedCardAuthyThreeDSecureTests {
    private static String biometricIdentityToken;
    private static String authyIdentityToken;
    private static String authyIdentityCurrency;
    private static String identityPrepaidManagedCardProfileId;
    private static String identityDebitManagedCardProfileId;
    private static String identityManagedAccountProfileId;

    @BeforeAll
    public static void IdentitySetup() {
        //    setup identity not enabled for Authy
        final CreateConsumerModel threeDSConsumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(threeDSConsumerProfileId).build();

        final Pair<String, String> threeDSAuthenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
                threeDSConsumerDetails, secretKey);
        biometricIdentityToken = threeDSAuthenticatedConsumer.getRight();

        //    setup identity enabled for Authy
        final CreateConsumerModel consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
                consumerDetails, secretKey);
        authyIdentityToken = authenticatedConsumer.getRight();
        authyIdentityCurrency = consumerDetails.getBaseCurrency();

        identityPrepaidManagedCardProfileId = consumerPrepaidManagedCardsProfileId;
        identityDebitManagedCardProfileId = consumerDebitManagedCardsProfileId;
        identityManagedAccountProfileId = consumerManagedAccountsProfileId;
    }

    @Override
    protected String getBiometricIdentityToken() {
        return biometricIdentityToken;
    }

    @Override
    protected String getAuthyIdentityToken() {
        return authyIdentityToken;
    }

    @Override
    public String getAuthyIdentityCurrency() {
        return authyIdentityCurrency;
    }

    @Override
    public String getIdentityPrepaidManagedCardProfileId() {
        return identityPrepaidManagedCardProfileId;
    }

    @Override
    public String getIdentityDebitManagedCardProfileId() {
        return identityDebitManagedCardProfileId;
    }

    @Override
    public String getIdentityManagedAccountProfileId() {
        return identityManagedAccountProfileId;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CONSUMER;
    }
}
