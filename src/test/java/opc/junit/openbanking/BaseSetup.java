package opc.junit.openbanking;

import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.helpers.openbanking.OpenBankingSecureServiceHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

import static opc.enums.openbanking.SignatureHeader.*;

public class BaseSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel applicationTwo;
    protected static ProgrammeDetailsModel nonFpsEnabledTenantDetails;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static CreateCorporateModel createCorporateModel;
    protected static CreateConsumerModel createConsumerModel;
    protected static String corporateManagedAccountProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String outgoingWireTransfersProfileId;
    protected static String transfersProfileId;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String sharedKeyAppTwo;

    protected static String tppId;
    protected static String clientKeyId;

    protected static String innovatorId;
    protected static String corporateId;
    protected static String consumerId;
    protected static String corporateAuthenticationToken;
    protected static String consumerAuthenticationToken;
    protected static String corporateCurrency;
    protected static String consumerCurrency;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
        nonFpsEnabledTenantDetails = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);

        innovatorId = applicationOne.getInnovatorId();
        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();
        corporateManagedAccountProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();
        corporatePrepaidManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        outgoingWireTransfersProfileId = applicationOne.getOwtProfileId();
        transfersProfileId = applicationOne.getTransfersProfileId();
        secretKey = applicationOne.getSecretKey();
        sharedKey = applicationOne.getSharedKey();
        sharedKeyAppTwo = applicationTwo.getSharedKey();

        final Pair<String, String> tppWithCertificate = AdminHelper.getTppWithCertificate();

        tppId = tppWithCertificate.getLeft();
        clientKeyId = tppWithCertificate.getRight();
    }

    protected static void corporateSetup() {
        createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken =
                OpenBankingSecureServiceHelper.login(createCorporateModel.getRootUser().getEmail(), sharedKey, tppId);
        corporateCurrency = createCorporateModel.getBaseCurrency();
    }

    protected static void consumerSetup() {
        createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken =
                OpenBankingSecureServiceHelper.login(createConsumerModel.getRootUser().getEmail(), sharedKey, tppId);
        consumerCurrency = createConsumerModel.getBaseCurrency();
    }

    protected static Pair<String, Map<String, String>> createCorporateWithConsentHeaders() throws Exception {

        final Pair<String, String> corporate =
                CorporatesHelper.createEnrolledVerifiedCorporate(corporateProfileId, secretKey);
        final String corporateConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));
        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, corporate.getRight(), tppId, corporateConsent);

        return Pair.of(corporate.getRight(), OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(corporateConsent))));
    }
}
