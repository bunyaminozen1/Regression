package opc.junit.secure.openbanking.paymentinitiation;

import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.OwnerType;
import opc.enums.opc.OwtType;
import opc.junit.database.OpenBankingDatabaseHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.helpers.openbanking.OpenBankingPaymentInitiationHelper;
import opc.junit.helpers.openbanking.OpenBankingSecureServiceHelper;
import opc.junit.secure.openbanking.BaseSetup;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.openbanking.CreateOutgoingWireTransferModel;
import opc.models.openbanking.VerificationModel;
import opc.services.openbanking.OpenBankingSecureService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static opc.enums.openbanking.SignatureHeader.DATE;
import static opc.enums.openbanking.SignatureHeader.DIGEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class VerifyOutgoingWireTransferChallengeTests extends BaseSetup {

    @BeforeAll
    public static void OneTimeSetup() throws Exception {
        corporateSetup();
        consumerSetup();

        final String corporateConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));
        final String consumerConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));

        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, corporateAuthenticationToken, tppId, corporateConsent);
        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, consumerAuthenticationToken, tppId, consumerConsent);
    }

    @ParameterizedTest
    @MethodSource("owtSuccessfulArgs")
    public void AuthoriseOwt_Corporate_Success(final Currency currency, final OwtType type) throws Exception {

        final Long paymentAmount = 200L;

        final Pair<String, Pair<Integer, Integer>> managedAccount =
                ManagedAccountsHelper.createFundedManagedAccount(corporateManagedAccountProfileId,
                        currency.name(), secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), paymentAmount, type)
                        .description(RandomStringUtils.randomAlphabetic(type.equals(OwtType.SEPA) ? 35 : 18))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        final Pair<String, CreateOutgoingWireTransferModel> payment =
                OpenBankingPaymentInitiationHelper
                        .createInitiatedChallengeOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel, corporateAuthenticationToken, tppId);

        final ValidatableResponse response =
                OpenBankingSecureService.verifyPaymentChallenge(sharedKey, corporateAuthenticationToken, tppId, payment.getLeft(), VerificationModel.defaultVerification())
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, payment.getLeft(), createOutgoingWireTransferModel, type, corporateId, OwnerType.CORPORATE);
    }

    @ParameterizedTest
    @MethodSource("owtSuccessfulArgs")
    public void AuthoriseOwt_Consumer_Success(final Currency currency, final OwtType type) throws Exception {

        final Long paymentAmount = 200L;

        final Pair<String, Pair<Integer, Integer>> managedAccount =
                ManagedAccountsHelper.createFundedManagedAccount(consumerManagedAccountProfileId,
                        currency.name(), secretKey, consumerAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), paymentAmount, type)
                        .description(RandomStringUtils.randomAlphabetic(type.equals(OwtType.SEPA) ? 35 : 18))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        final Pair<String, CreateOutgoingWireTransferModel> payment =
                OpenBankingPaymentInitiationHelper
                        .createInitiatedChallengeOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel, consumerAuthenticationToken, tppId);

        final ValidatableResponse response =
                OpenBankingSecureService.verifyPaymentChallenge(sharedKey, consumerAuthenticationToken, tppId, payment.getLeft(), VerificationModel.defaultVerification())
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, payment.getLeft(), createOutgoingWireTransferModel, type, consumerId, OwnerType.CONSUMER);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final String paymentConsentId,
                                          final CreateOutgoingWireTransferModel createOutgoingWireTransferModel,
                                          final OwtType owtType,
                                          final String identityId,
                                          final OwnerType ownerType) {

        response.body("consent.id", notNullValue())
                .body("consent.state", equalTo("AUTHORISED"))
                .body("consent.tppId",equalTo(tppId))
                .body("consent.tppName",equalTo("Test"))
                .body("consent.identity.id",equalTo(identityId))
                .body("consent.identity.type",equalTo(ownerType.getValue()))
                .body("consent.lastUpdated", notNullValue())
                .body("consent.expiry", notNullValue())
                .body("consent.createdTimestamp", notNullValue());

        response.body("payment.id", equalTo(Objects.requireNonNull(OpenBankingDatabaseHelper.getPaymentConsent(paymentConsentId)).get(0).get("payment_id")))
                .body("payment.creationTimestamp", notNullValue())
                .body("payment.tag", equalTo(createOutgoingWireTransferModel.getTag()))
                .body("payment.state", equalTo("PENDING_CHALLENGE"))
                .body("payment.type", equalTo(owtType.name()))
                .body("payment.sourceInstrument.type", equalTo(createOutgoingWireTransferModel.getSourceInstrument().getType()))
                .body("payment.sourceInstrument.id", equalTo(createOutgoingWireTransferModel.getSourceInstrument().getId()))
                .body("payment.transferAmount.currency", equalTo(createOutgoingWireTransferModel.getTransferAmount().getCurrency()))
                .body("payment.transferAmount.amount", equalTo(createOutgoingWireTransferModel.getTransferAmount().getAmount().intValue()))
                .body("payment.description", equalTo(createOutgoingWireTransferModel.getDescription()))
                .body("payment.destination.name", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getName()))
                .body("payment.destination.address", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getAddress()))
                .body("payment.destination.bankName", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getBankName()))
                .body("payment.destination.bankAddress", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getBankAddress()))
                .body("payment.destination.bankCountry", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getBankCountry()));

        response.body("paymentRequest.profileId", equalTo(createOutgoingWireTransferModel.getProfileId()))
                .body("paymentRequest.tag", equalTo(createOutgoingWireTransferModel.getTag()))
                .body("paymentRequest.sourceInstrument.type", equalTo(createOutgoingWireTransferModel.getSourceInstrument().getType()))
                .body("paymentRequest.sourceInstrument.id", equalTo(createOutgoingWireTransferModel.getSourceInstrument().getId()))
                .body("paymentRequest.transferAmount.currency", equalTo(createOutgoingWireTransferModel.getTransferAmount().getCurrency()))
                .body("paymentRequest.transferAmount.amount", equalTo(createOutgoingWireTransferModel.getTransferAmount().getAmount().intValue()))
                .body("paymentRequest.description", equalTo(createOutgoingWireTransferModel.getDescription()))
                .body("paymentRequest.destinationBeneficiary.name", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getName()))
                .body("paymentRequest.destinationBeneficiary.address", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getAddress()))
                .body("paymentRequest.destinationBeneficiary.bankName", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getBankName()))
                .body("paymentRequest.destinationBeneficiary.bankAddress", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getBankAddress()))
                .body("paymentRequest.destinationBeneficiary.bankCountry", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getBankCountry()));

        assertBankAccountDetails(response, createOutgoingWireTransferModel, owtType);
    }

    private void assertBankAccountDetails(final ValidatableResponse response,
                                          final CreateOutgoingWireTransferModel createOutgoingWireTransferModel,
                                          final OwtType owtType) {
        switch (owtType){
            case SEPA:
                final SepaBankDetailsModel sepaBankDetails =
                        (SepaBankDetailsModel) createOutgoingWireTransferModel.getDestinationBeneficiary().getBankAccountDetails();

                response
                        .body("paymentRequest.destinationBeneficiary.bankAccountDetails.iban", equalTo(sepaBankDetails.getIban()))
                        .body("paymentRequest.destinationBeneficiary.bankAccountDetails.bankIdentifierCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
                        .body("payment.destination.bankAccountDetails.iban", equalTo(sepaBankDetails.getIban()))
                        .body("payment.destination.bankAccountDetails.bankIdentifierCode", equalTo(sepaBankDetails.getBankIdentifierCode()));
                break;
            case FASTER_PAYMENTS:
                final FasterPaymentsBankDetailsModel fasterPaymentsBankDetails =
                        (FasterPaymentsBankDetailsModel) createOutgoingWireTransferModel.getDestinationBeneficiary().getBankAccountDetails();

                response
                        .body("paymentRequest.destinationBeneficiary.bankAccountDetails.accountNumber", equalTo(fasterPaymentsBankDetails.getAccountNumber()))
                        .body("paymentRequest.destinationBeneficiary.bankAccountDetails.sortCode", equalTo(fasterPaymentsBankDetails.getSortCode()))
                        .body("payment.destination.bankAccountDetails.accountNumber", equalTo(fasterPaymentsBankDetails.getAccountNumber()))
                        .body("payment.destination.bankAccountDetails.sortCode", equalTo(fasterPaymentsBankDetails.getSortCode()));
                break;
            default: throw new IllegalArgumentException("OWT type not supported.");
        }
    }

    private static Stream<Arguments> owtSuccessfulArgs() {
        return Stream.of(Arguments.of(Currency.EUR, OwtType.SEPA),
                Arguments.of(Currency.GBP, OwtType.FASTER_PAYMENTS));
    }
}