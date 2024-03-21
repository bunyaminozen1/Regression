package opc.junit.secure.openbanking;

import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.openbanking.OpenBankingSecureServiceHelper;
import opc.junit.openbanking.BaseSetupExtension;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BaseSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
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

    protected static String tppId;
    protected static String clientKeyId;

    protected static String corporateId;
    protected static String consumerId;
    protected static String corporateAuthenticationToken;
    protected static String consumerAuthenticationToken;
    protected static String corporateCurrency;
    protected static String consumerCurrency;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

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
}
