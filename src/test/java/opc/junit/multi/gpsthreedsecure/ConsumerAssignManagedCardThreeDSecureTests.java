package opc.junit.multi.gpsthreedsecure;

import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.testmodels.UnassignedManagedCardDetails;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public class ConsumerAssignManagedCardThreeDSecureTests extends AbstractAssignManagedCardThreeDSecureTests{
    private static String threeDSIdentityToken;
    private static List<UnassignedManagedCardDetails> threeDSecureIdentityUnassignedCards;
    private static String identityToken;
    private static List<UnassignedManagedCardDetails> identityUnassignedCards;

    @BeforeAll
    public static void IdentitySetup() {
        //    setup 3ds identity
        final CreateConsumerModel threeDSConsumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(threeDSConsumerProfileId).build();

        final Pair<String, String> threeDSAuthenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
                threeDSConsumerDetails, secretKey);
        threeDSIdentityToken = threeDSAuthenticatedConsumer.getRight();
        final String threeDSIdentityCurrency = threeDSConsumerDetails.getBaseCurrency();

        //    setup identity not enabled for 3ds
        final CreateConsumerModel consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
                consumerDetails, secretKey);
        identityToken = authenticatedConsumer.getRight();
        final String identityCurrency = consumerDetails.getBaseCurrency();

        final String identityManagedAccountId =
                createManagedAccount(consumerManagedAccountsProfileId, identityCurrency, identityToken);
        final String threeDSIdentityManagedAccountId =
                createManagedAccount(consumerManagedAccountsProfileId, threeDSIdentityCurrency, threeDSIdentityToken);

        identityUnassignedCards =
                ManagedCardsHelper.replenishCardPool(identityManagedAccountId, consumerPrepaidManagedCardsProfileId,
                        consumerDebitManagedCardsProfileId, identityCurrency, CardLevelClassification.CONSUMER, innovatorToken);
        threeDSecureIdentityUnassignedCards =
                ManagedCardsHelper.replenishCardPool(threeDSIdentityManagedAccountId, consumerPrepaidManagedCardsProfileId,
                        consumerDebitManagedCardsProfileId, threeDSIdentityCurrency, CardLevelClassification.CONSUMER, innovatorToken);
    }

    @Override
    protected String getThreeDSIdentityToken() {
        return threeDSIdentityToken;
    }

    @Override
    protected String getIdentityToken() {
        return identityToken;
    }

    @Override
    public List<UnassignedManagedCardDetails> getThreeDSIdentityUnassignedCards() {
        return threeDSecureIdentityUnassignedCards;
    }

    @Override
    public List<UnassignedManagedCardDetails> getIdentityUnassignedCards() {
        return identityUnassignedCards;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CONSUMER;
    }
}
