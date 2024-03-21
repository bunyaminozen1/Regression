package opc.junit.openbanking.outgoingwiretransfers;

import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.OwnerType;
import opc.enums.opc.OwtType;
import opc.enums.openbanking.PaymentState;
import opc.junit.database.OpenBankingDatabaseHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.helpers.openbanking.OpenBankingPaymentInitiationHelper;
import opc.junit.helpers.openbanking.OpenBankingSecureServiceHelper;
import opc.junit.openbanking.BaseSetup;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.openbanking.CreateOutgoingWireTransferModel;
import opc.models.shared.CurrencyAmount;
import opc.services.openbanking.PaymentInitiationService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static opc.enums.opc.OwnerType.CONSUMER;
import static opc.enums.opc.OwnerType.CORPORATE;
import static opc.enums.openbanking.PaymentState.AUTHORISED;
import static opc.enums.openbanking.PaymentState.PENDING_CHALLENGE;
import static opc.enums.openbanking.SignatureHeader.DATE;
import static opc.enums.openbanking.SignatureHeader.DIGEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetOutgoingWireTransferTests extends BaseSetup {

    private static Map<String, Pair<PaymentState, CreateOutgoingWireTransferModel>> corporatePayments = new HashMap<>();
    private static Map<String, Pair<PaymentState, CreateOutgoingWireTransferModel>> consumerPayments = new HashMap<>();

    private Map<String, String> corporateHeaders;
    private Map<String, String> consumerHeaders;

    @BeforeAll
    public static void OneTimeSetup() throws Exception {
        corporateSetup();
        consumerSetup();

        final String corporateConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));
        final String consumerConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));

        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, corporateAuthenticationToken, tppId, corporateConsent);
        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, consumerAuthenticationToken, tppId, consumerConsent);

        corporatePayments =
                createPayments(corporateManagedAccountProfileId, Currency.GBP, OwtType.FASTER_PAYMENTS, corporateAuthenticationToken);
        consumerPayments =
                createPayments(consumerManagedAccountProfileId, Currency.EUR, OwtType.SEPA, consumerAuthenticationToken);
    }

    @BeforeEach
    public void Setup() throws Exception {
        corporateHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                DIGEST, Optional.empty()));
        consumerHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                DIGEST, Optional.empty()));
    }

    @Test
    public void GetOwt_Corporate_Success() {

        corporatePayments.entrySet().forEach(payment -> {
            final ValidatableResponse response =
                    PaymentInitiationService.getOutgoingWireTransfer(sharedKey, corporateHeaders, payment.getKey())
                            .then()
                            .statusCode(SC_OK);

            assertSuccessfulResponse(response, payment, OwtType.FASTER_PAYMENTS, corporateId, CORPORATE);
        });
    }

    @Test
    public void GetOwt_Consumer_Success() {

        consumerPayments.entrySet().forEach(payment -> {
            final ValidatableResponse response =
                    PaymentInitiationService.getOutgoingWireTransfer(sharedKey, consumerHeaders, payment.getKey())
                            .then()
                            .statusCode(SC_OK);

            assertSuccessfulResponse(response, payment, OwtType.SEPA, consumerId, CONSUMER);
        });
    }

    private void  assertSuccessfulResponse(final ValidatableResponse response,
                                           final Map.Entry<String, Pair<PaymentState, CreateOutgoingWireTransferModel>> paymentConsent,
                                           final OwtType owtType,
                                           final String identityId,
                                           final OwnerType ownerType) {

        response.body("consent.id", equalTo(paymentConsent.getKey()))
                .body("consent.state", equalTo(paymentConsent.getValue().getLeft().name()))
                .body("consent.tppId",equalTo(tppId))
                .body("consent.tppName",equalTo("Test"))
                .body("consent.lastUpdated", notNullValue())
                .body("consent.expiry", notNullValue())
                .body("consent.identity.id", equalTo(identityId))
                .body("consent.identity.type", equalTo(ownerType.getValue()))
                .body("consent.createdTimestamp", notNullValue());

        response.body("payment.id", equalTo(Objects.requireNonNull(OpenBankingDatabaseHelper.getPaymentConsent(paymentConsent.getKey())).get(0).get("payment_id")))
                .body("payment.creationTimestamp", notNullValue())
                .body("payment.tag", equalTo(paymentConsent.getValue().getRight().getTag()))
                .body("payment.state", equalTo(paymentConsent.getValue().getLeft().equals(AUTHORISED) ? "COMPLETED" : paymentConsent.getValue().getLeft().name()))
                .body("payment.type", equalTo(owtType.name()))
                .body("payment.sourceInstrument.type", equalTo(paymentConsent.getValue().getRight().getSourceInstrument().getType()))
                .body("payment.sourceInstrument.id", equalTo(paymentConsent.getValue().getRight().getSourceInstrument().getId()))
                .body("payment.transferAmount.currency", equalTo(paymentConsent.getValue().getRight().getTransferAmount().getCurrency()))
                .body("payment.transferAmount.amount", equalTo(paymentConsent.getValue().getRight().getTransferAmount().getAmount().intValue()))
                .body("payment.description", equalTo(paymentConsent.getValue().getRight().getDescription()))
                .body("payment.destination.name", equalTo(paymentConsent.getValue().getRight().getDestinationBeneficiary().getName()))
                .body("payment.destination.address", equalTo(paymentConsent.getValue().getRight().getDestinationBeneficiary().getAddress()))
                .body("payment.destination.bankName", equalTo(paymentConsent.getValue().getRight().getDestinationBeneficiary().getBankName()))
                .body("payment.destination.bankAddress", equalTo(paymentConsent.getValue().getRight().getDestinationBeneficiary().getBankAddress()))
                .body("payment.destination.bankCountry", equalTo(paymentConsent.getValue().getRight().getDestinationBeneficiary().getBankCountry()));

        response.body("paymentRequest.profileId", equalTo(paymentConsent.getValue().getRight().getProfileId()))
                .body("paymentRequest.tag", equalTo(paymentConsent.getValue().getRight().getTag()))
                .body("paymentRequest.sourceInstrument.type", equalTo(paymentConsent.getValue().getRight().getSourceInstrument().getType()))
                .body("paymentRequest.sourceInstrument.id", equalTo(paymentConsent.getValue().getRight().getSourceInstrument().getId()))
                .body("paymentRequest.transferAmount.currency", equalTo(paymentConsent.getValue().getRight().getTransferAmount().getCurrency()))
                .body("paymentRequest.transferAmount.amount", equalTo(paymentConsent.getValue().getRight().getTransferAmount().getAmount().intValue()))
                .body("paymentRequest.description", equalTo(paymentConsent.getValue().getRight().getDescription()))
                .body("paymentRequest.destinationBeneficiary.name", equalTo(paymentConsent.getValue().getRight().getDestinationBeneficiary().getName()))
                .body("paymentRequest.destinationBeneficiary.address", equalTo(paymentConsent.getValue().getRight().getDestinationBeneficiary().getAddress()))
                .body("paymentRequest.destinationBeneficiary.bankName", equalTo(paymentConsent.getValue().getRight().getDestinationBeneficiary().getBankName()))
                .body("paymentRequest.destinationBeneficiary.bankAddress", equalTo(paymentConsent.getValue().getRight().getDestinationBeneficiary().getBankAddress()))
                .body("paymentRequest.destinationBeneficiary.bankCountry", equalTo(paymentConsent.getValue().getRight().getDestinationBeneficiary().getBankCountry()));

        assertBankAccountDetails(response, paymentConsent.getValue().getRight(), owtType);
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

    private static Map<String, Pair<PaymentState, CreateOutgoingWireTransferModel>> createPayments(final String managedAccountsProfileId,
                                                                                                   final Currency currency,
                                                                                                   final OwtType owtType,
                                                                                                   final String token) throws Exception {

        final Map<String, Pair<PaymentState, CreateOutgoingWireTransferModel>> payments = new HashMap<>();

        final Pair<String, Pair<Integer, Integer>> managedAccount =
                ManagedAccountsHelper.createFundedManagedAccount(managedAccountsProfileId, currency.name(), secretKey, token);

        final Pair<String, CreateOutgoingWireTransferModel> completedPayment =
                OpenBankingPaymentInitiationHelper.createCompletedOutgoingWireTransfer(sharedKey, outgoingWireTransfersProfileId, clientKeyId,
                        managedAccount.getLeft(), new CurrencyAmount(currency.name(), 100L), owtType, token, tppId);

        final Pair<String, CreateOutgoingWireTransferModel> authorisedPayment =
                OpenBankingPaymentInitiationHelper.createAuthorisedOutgoingWireTransfer(sharedKey, outgoingWireTransfersProfileId, clientKeyId,
                        managedAccount.getLeft(), new CurrencyAmount(currency.name(), 100L), owtType, token, tppId);

        final Pair<String, CreateOutgoingWireTransferModel> initiatedPayment =
                OpenBankingPaymentInitiationHelper.createInitiatedChallengeOutgoingWireTransfer(sharedKey, outgoingWireTransfersProfileId, clientKeyId,
                        managedAccount.getLeft(), new CurrencyAmount(currency.name(), 100L), owtType, token, tppId);

        payments.put(completedPayment.getKey(), Pair.of(AUTHORISED, completedPayment.getValue()));
        payments.put(authorisedPayment.getKey(), Pair.of(PENDING_CHALLENGE, authorisedPayment.getValue()));
        payments.put(initiatedPayment.getKey(), Pair.of(PENDING_CHALLENGE, initiatedPayment.getValue()));

        return payments;
    }
}
