package opc.junit.multi.gpsthreedsecure;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerCreateManagedCardsThreeDSecureTests extends AbstractCreateManagedCardThreeDSecureTests {
    private static String threeDSIdentityToken;
    private static String threeDSIdentityCurrency;

    private static String identityToken;
    private static String identityCurrency;
    private static String identityPrepaidManagedCardProfileId;
    private static String identityDebitManagedCardProfileId;
    private static String identityManagedAccountProfileId;

    @BeforeAll
    public static void IdentitySetup() {
        //    setup 3ds identity
        final CreateConsumerModel threeDSConsumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(threeDSConsumerProfileId).build();

        final Pair<String, String> threeDSAuthenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
                threeDSConsumerDetails, secretKey);
        threeDSIdentityToken = threeDSAuthenticatedConsumer.getRight();
        threeDSIdentityCurrency = threeDSConsumerDetails.getBaseCurrency();

        //    setup identity not enabled for 3ds
        final CreateConsumerModel consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
                consumerDetails, secretKey);
        identityToken = authenticatedConsumer.getRight();
        identityCurrency = consumerDetails.getBaseCurrency();

        identityPrepaidManagedCardProfileId = consumerPrepaidManagedCardsProfileId;
        identityDebitManagedCardProfileId = consumerDebitManagedCardsProfileId;
        identityManagedAccountProfileId = consumerManagedAccountsProfileId;
    }

    @Override
    protected String getThreeDSIdentityToken() {
        return threeDSIdentityToken;
    }

    @Override
    public String getThreeDSIdentityCurrency() {
        return threeDSIdentityCurrency;
    }

    @Override
    protected String getIdentityToken() {
        return identityToken;
    }

    @Override
    public String getIdentityCurrency() {
        return identityCurrency;
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
