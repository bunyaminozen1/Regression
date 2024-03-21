package opc.junit.helpers.openbanking;

import opc.enums.opc.OwtType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.models.openbanking.CreateOutgoingWireTransferModel;
import opc.models.shared.CurrencyAmount;
import opc.services.openbanking.PaymentInitiationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

import static opc.enums.openbanking.SignatureHeader.DATE;
import static opc.enums.openbanking.SignatureHeader.DIGEST;
import static org.apache.http.HttpStatus.SC_OK;

public class OpenBankingPaymentInitiationHelper {

    public static Pair<String, CreateOutgoingWireTransferModel> createOutgoingWireTransfer(final String sharedKey,
                                                                                           final Map<String, String> headers,
                                                                                           final CreateOutgoingWireTransferModel createOutgoingWireTransferModel) {
        return Pair.of(TestHelper.ensureAsExpected(10,
                () -> PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel),
                SC_OK).jsonPath().getString("consent.id"), createOutgoingWireTransferModel);
    }

    public static Pair<String, CreateOutgoingWireTransferModel> createOutgoingWireTransfer(final String sharedKey,
                                                                                           final String outgoingWireTransfersProfileId,
                                                                                           final String clientKeyId,
                                                                                           final String managedAccountId,
                                                                                           final CurrencyAmount currencyAmount,
                                                                                           final OwtType type) throws Exception {

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        currencyAmount.getCurrency(), currencyAmount.getAmount(), type)
                        .description(RandomStringUtils.randomAlphabetic(type.equals(OwtType.SEPA) ? 35 : 18))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        return createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel);
    }

    public static Pair<String, CreateOutgoingWireTransferModel> createCompletedOutgoingWireTransfer(final String sharedKey,
                                                                                                    final String outgoingWireTransfersProfileId,
                                                                                                    final String clientKeyId,
                                                                                                    final String managedAccountId,
                                                                                                    final CurrencyAmount currencyAmount,
                                                                                                    final OwtType type,
                                                                                                    final String token,
                                                                                                    final String tppId) throws Exception {

        final Pair<String, CreateOutgoingWireTransferModel> paymentConsent =
                createOutgoingWireTransfer(sharedKey, outgoingWireTransfersProfileId, clientKeyId, managedAccountId, currencyAmount, type);
        OpenBankingSecureServiceHelper.authoriseAndVerifyPayment(sharedKey, token, tppId, paymentConsent.getLeft());

        OutgoingWireTransfersHelper
                .checkOwtStateByAccountId(paymentConsent.getRight().getSourceInstrument().getId(), "COMPLETED");

        return paymentConsent;
    }

    public static Pair<String, CreateOutgoingWireTransferModel> createAuthorisedOutgoingWireTransfer(final String sharedKey,
                                                                                                     final String outgoingWireTransfersProfileId,
                                                                                                     final String clientKeyId,
                                                                                                     final String managedAccountId,
                                                                                                     final CurrencyAmount currencyAmount,
                                                                                                     final OwtType type,
                                                                                                     final String token,
                                                                                                     final String tppId) throws Exception {

        final Pair<String, CreateOutgoingWireTransferModel> paymentConsent =
                createOutgoingWireTransfer(sharedKey, outgoingWireTransfersProfileId, clientKeyId, managedAccountId, currencyAmount, type);
        OpenBankingSecureServiceHelper.authorisePayment(sharedKey, token, tppId, paymentConsent.getLeft());

        return paymentConsent;
    }

    public static Pair<String, CreateOutgoingWireTransferModel> createAuthorisedOutgoingWireTransfer(final String sharedKey,
                                                                                                     final Map<String, String> headers,
                                                                                                     final CreateOutgoingWireTransferModel createOutgoingWireTransferModel,
                                                                                                     final String token,
                                                                                                     final String tppId) {

        final Pair<String, CreateOutgoingWireTransferModel> paymentConsent =
                createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel);
        OpenBankingSecureServiceHelper.authorisePayment(sharedKey, token, tppId, paymentConsent.getLeft());

        return paymentConsent;
    }

    public static Pair<String, CreateOutgoingWireTransferModel> createInitiatedChallengeOutgoingWireTransfer(final String sharedKey,
                                                                                                             final String outgoingWireTransfersProfileId,
                                                                                                             final String clientKeyId,
                                                                                                             final String managedAccountId,
                                                                                                             final CurrencyAmount currencyAmount,
                                                                                                             final OwtType type,
                                                                                                             final String token,
                                                                                                             final String tppId) throws Exception {

        final Pair<String, CreateOutgoingWireTransferModel> paymentConsent =
                createOutgoingWireTransfer(sharedKey, outgoingWireTransfersProfileId, clientKeyId, managedAccountId, currencyAmount, type);
        OpenBankingSecureServiceHelper.authorisePayment(sharedKey, token, tppId, paymentConsent.getLeft());
        OpenBankingSecureServiceHelper.initiatePaymentChallenge(sharedKey, token, tppId, paymentConsent.getLeft());

        return paymentConsent;
    }

    public static Pair<String, CreateOutgoingWireTransferModel> createInitiatedChallengeOutgoingWireTransfer(final String sharedKey,
                                                                                                             final Map<String, String> headers,
                                                                                                             final CreateOutgoingWireTransferModel createOutgoingWireTransferModel,
                                                                                                             final String token,
                                                                                                             final String tppId) {

        final Pair<String, CreateOutgoingWireTransferModel> paymentConsent =
                createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel);
        OpenBankingSecureServiceHelper.authorisePayment(sharedKey, token, tppId, paymentConsent.getLeft());
        OpenBankingSecureServiceHelper.initiatePaymentChallenge(sharedKey, token, tppId, paymentConsent.getLeft());

        return paymentConsent;
    }
}
