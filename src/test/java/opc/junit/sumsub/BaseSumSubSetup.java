package opc.junit.sumsub;

import commons.config.ConfigHelper;
import opc.enums.opc.InnovatorSetup;
import opc.junit.database.PayneticsDatabaseHelper;
import opc.junit.database.SubscriptionsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.sumsub.questionnaire.SumSubQuestionnaireModel;
import opc.services.admin.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;

@Execution(ExecutionMode.CONCURRENT)
public class BaseSumSubSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    private final static String CONSUMER_LEVEL2_QUESTIONNAIRE_ID = "consumer_level2_questionnaire";
    private final static String CONSUMER_LEVEL1_QUESTIONNAIRE_ID = "consumer_level1_questionnaire";
    protected final static String CORPORATE_QUESTIONNAIRE_ID = "corporate_questionnaire";
    protected final static String CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID = "corporate_sole_trader_questionnaire";
    protected final static String UBO_DIRECTOR_QUESTIONNAIRE_ID = "ubo_director_questionnaire";
    protected final static String CONSUMER_1_POA_RESIDENCY_LEVEL = String.format("%s-consumer-1POA-residency",
        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment());
    protected final static String CONSUMER_PUK = String.format("%s-consumer-puk",
            ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment());
    protected final static String CORPORATE_PUK = String.format("%s-corporate-puk",
            ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment());

    protected final static String PAYNETICS_EEA_TOKEN = "92cf8f21-aa6d-4e9d-a1c1-ba50046d328c";
    protected final static String PAYNETICS_UK_TOKEN = "40a00bd0-67f7-48c5-8a75-0a47bdc0b864";

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel applicationThree;
    protected static ProgrammeDetailsModel applicationTwo;
    protected static ProgrammeDetailsModel nonFpsEnabledTenant;
    protected static ProgrammeDetailsModel semiPasscodeApp;
    protected static ProgrammeDetailsModel semiScaSendsApp;

    protected static ProgrammeDetailsModel applicationOneUk;

    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String corporateManagedAccountProfileId;
    protected static String prepaidCardProfileId;
    protected static String debitCardProfileId;
    protected static String transfersProfileId;
    protected static String programmeId;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorId;
    protected static String adminToken;
    protected static String impersonatedAdminToken;
    protected static String innovatorToken;

    @BeforeAll
    public static void GlobalSetup(){

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationThree = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_THREE);
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
        nonFpsEnabledTenant = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);
        semiPasscodeApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SEMI_PASSCODE_APP);
        semiScaSendsApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SEMI_SCA_SENDS_APP);
        applicationOneUk = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE_UK);

        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();
        innovatorId=applicationOne.getInnovatorId();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();

        prepaidCardProfileId = applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        debitCardProfileId = applicationOne.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        consumerManagedAccountProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();
        corporateManagedAccountProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();

        programmeId = applicationOne.getProgrammeId();
        secretKey = applicationOne.getSecretKey();
        sharedKey = applicationOne.getSharedKey();
        transfersProfileId = applicationOne.getTransfersProfileId();
        adminToken = AdminService.loginAdmin();
        impersonatedAdminToken = AdminService.impersonateTenant(applicationOne.getInnovatorId(), adminToken);
        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);

        setSumSubConsumerQuestionnaire();
        setSumSubCorporateQuestionnaire();
        setSumSubUboDirectorQuestionnaire();
        setSumSubCorporateSoleTraderQuestionnaire();
    }

    public static void setSumSubConsumerQuestionnaire() {

        final List<SumSubQuestionnaireModel> sumSubQuestionnaireModel = Arrays.asList(
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL2_QUESTIONNAIRE_ID, "pepcategory", "pepCategory"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL2_QUESTIONNAIRE_ID, "rcacategory", "rcaCategory"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL2_QUESTIONNAIRE_ID, "nolongerpep", "noLongerAPep"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL2_QUESTIONNAIRE_ID, "nolongerrca", "noLongerAnRCA"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL2_QUESTIONNAIRE_ID, "industry", "industry"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL2_QUESTIONNAIRE_ID, "originoffunds", "originOfFunds"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL1_QUESTIONNAIRE_ID, "industry", "industry"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL2_QUESTIONNAIRE_ID, "empstatus", "employmentStatus"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL2_QUESTIONNAIRE_ID, "receivefundsfrom", "incomingFundsFrom"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL2_QUESTIONNAIRE_ID, "otheroriginoffunds", "originOfFundsOther"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL2_QUESTIONNAIRE_ID, "expectedvalueofdeposits", "expectedValueOfDeposits"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CONSUMER_LEVEL2_QUESTIONNAIRE_ID, "expectedfrequencyofincomingtxs", "expectedFrequencyOfIncomingTxs")
        );

        sumSubQuestionnaireModel.forEach(model -> AdminService.removeSumSubQuestionnaire(model, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT));
        sumSubQuestionnaireModel.forEach(model -> AdminService.setSumSubQuestionnaire(model, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT));
    }

    private static void setSumSubCorporateQuestionnaire() {

        final List<SumSubQuestionnaireModel> sumSubQuestionnaireModel = Arrays.asList(
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "declaration", "declaration"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "businessaddressissame", "businessAddressIsSame"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "correspondenceaddressissame", "correspondenceAddressIsSame"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "microenterprise", "microEnterprise"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "tin", "tin"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "website", "website"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "lengthofoperation", "lengthOfOperation"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "industrycategory", "industryCategory"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "category_information", "categoryInformation"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "licencerequired", "licenceRequired"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "expectedmonthlyturnover", "expectedMonthlyTurnover"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "expectedsourceoffundslocation", "expectedSourceOfFundsLocation"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "expectedoriginoffunds", "expectedOriginOfFunds"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "expectedoriginoffundsother", "expectedOriginOfFundsOther"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "expectedcardsmonthly", "expectedCardsMonthly"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "expectedincomingtransfervolumemonthly", "expectedIncomingTransferVolumeMonthly"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "expectedincomingtransfersmonthly", "expectedIncomingTransfersMonthly"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "expectedpaymentreceiptcountries", "expectedPaymentReceiptCountries"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "expectedoutgoingtransfervolumemonthly", "expectedOutgoingTransferVolumeMonthly"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "expectedoutgoingtransfersmonthly", "expectedOutgoingTransfersMonthly"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_QUESTIONNAIRE_ID, "expectedaverageoutgoingfundsamount", "expectedAverageOutgoingFundsAmount")
        );

        sumSubQuestionnaireModel.forEach(model -> AdminService.setSumSubQuestionnaire(model, AdminService.loginAdmin())
                .then()
                .statusCode(SC_NO_CONTENT));
    }

    private static void setSumSubCorporateSoleTraderQuestionnaire() {

        final List<SumSubQuestionnaireModel> sumSubQuestionnaireModel = Arrays.asList(
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "declaration", "declaration"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "isregistered", "isRegistered"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "registrationnumber", "registrationNumber"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "correspondenceaddressissame", "correspondenceAddressIsSame"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "website", "website"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "lengthofoperation", "lengthOfOperation"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "industrycategory", "industryCategory"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "category_information", "categoryInformation"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "licencerequired", "licenceRequired"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "expectedsourceoffundslocation", "expectedSourceOfFundsLocation"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "expectedoriginoffunds", "expectedOriginOfFunds"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "expectedoriginoffundsother", "expectedOriginOfFundsOther"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "expectedcardsmonthly", "expectedCardsMonthly"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "expectedincomingtransfervolumemonthly", "expectedIncomingTransferVolumeMonthly"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "expectedincomingtransfersmonthly", "expectedIncomingTransfersMonthly"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "expectedpaymentreceiptcountries", "expectedPaymentReceiptCountries"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "expectedoutgoingtransfervolumemonthly", "expectedOutgoingTransferVolumeMonthly"),
                SumSubQuestionnaireModel.createSumSubQuestionnaire(CORPORATE_SOLE_TRADER_QUESTIONNAIRE_ID, "expectedoutgoingtransfersmonthly", "expectedOutgoingTransfersMonthly")
        );

        sumSubQuestionnaireModel.forEach(model -> AdminService.setSumSubQuestionnaire(model, AdminService.loginAdmin())
                .then()
                .statusCode(SC_NO_CONTENT));
    }

    public static void setSumSubUboDirectorQuestionnaire() {

        final List<SumSubQuestionnaireModel> sumSubQuestionnaireModel = Arrays.asList(
            SumSubQuestionnaireModel.createSumSubQuestionnaire(UBO_DIRECTOR_QUESTIONNAIRE_ID, "declaration", "declaration"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(UBO_DIRECTOR_QUESTIONNAIRE_ID, "pepcategory", "pepcategory"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(UBO_DIRECTOR_QUESTIONNAIRE_ID, "rcacategory", "rcacategory"),
            SumSubQuestionnaireModel.createSumSubQuestionnaire(UBO_DIRECTOR_QUESTIONNAIRE_ID, "industry", "industry")

        );

        sumSubQuestionnaireModel.forEach(model -> AdminService.setSumSubQuestionnaire(model, AdminService.loginAdmin())
            .then()
            .statusCode(SC_NO_CONTENT));
    }

    public static boolean ensureIdentitySubscribed(final String identityId) {

        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> SubscriptionsDatabaseHelper.getSubscription(identityId),
                x -> x.size() > 0 && x.get(0).get("status").equals("ACTIVE"),
                Optional.of(String.format("Subscription for identity with id %s not ACTIVE", identityId)));

        return true;
    }

    public static String getPayneticsUserCreateLog(final String identityId) {

        return TestHelper.ensureDatabaseResultAsExpected(120,
                () -> PayneticsDatabaseHelper.getPayneticsUserCreateLog(identityId),
                x -> x.size() > 0 && x.get(0).get("request_payload").contains(identityId),
                Optional.of(String.format("Paynetics create user log does not contain identity with id %s", identityId)))
                .get(0).get("request_payload");
    }

    public static String getNaturalPersonsApiRequest(final String identityId) {

        return TestHelper.ensureDatabaseResultAsExpected(120,
                        () -> PayneticsDatabaseHelper.getNaturalPersonsApiLogs(identityId),
                        x -> !x.isEmpty() && x.get(0).get("request_payload").contains(identityId),
                        Optional.of(String.format("Paynetics create user log does not contain identity with id %s", identityId)))
                .get(0).get("request_payload");
    }

    public static String getUpdateNaturalPersonsApiRequest(final String identityId) {

        return TestHelper.ensureDatabaseResultAsExpected(120,
                        () -> PayneticsDatabaseHelper.getUpdateNaturalPersonsApiLogs(identityId),
                        x -> !x.isEmpty() && x.get(0).get("request_payload").contains(identityId),
                        Optional.of(String.format("Paynetics create user log does not contain identity with id %s", identityId)))
                .get(0).get("request_payload");
    }

    public static String getCorporatesV3ApiRequest(final String corporateName) {

        return TestHelper.ensureDatabaseResultAsExpected(120,
                        () -> PayneticsDatabaseHelper.getCorporatesV3ApiLogs(corporateName),
                        x -> x.size() > 0 && x.get(0).get("request_payload").contains(corporateName),
                        Optional.of(String.format("Paynetics application_create log does not contain company with name %s", corporateName)))
                .get(0).get("request_payload");
    }
}
