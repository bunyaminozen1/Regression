package opc.junit.multiprivate;

import commons.enums.Currency;
import commons.models.CompanyModel;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ResourceType;
import opc.helpers.ChallengesModelHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.backoffice.IdentityModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multiprivate.RegisterLinkedAccountModel;
import opc.services.multiprivate.ChallengesService;
import opc.services.multiprivate.MultiPrivateService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public class VerifyOneTimePasswordVariableRecurringPaymentsTests extends BaseMultiPrivateSetup {
    final private String VERIFICATION_CODE = "123456";
    @Test
    void VerifyOneTimePassword_HappyPath_Success() {

        final String corporateCurrency = Currency.GBP.name();

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(pluginCorporateProfileId)
                        .setBaseCurrency(corporateCurrency)
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry("GB")
                                .build())
                        .build();

        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(corporateDetails, pluginSecretKey);
        final IdentityModel identityModel = new IdentityModel(corporate.getLeft(), IdentityType.CORPORATE);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(pluginZeroBalanceAccountProfileId, corporateCurrency, pluginSecretKey, corporate.getRight());

        final RegisterLinkedAccountModel registerLinkedAccountModel =
                RegisterLinkedAccountModel.DefaultRegisterLinkedAccountFasterModel(identityModel, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, corporateCurrency).build();

        final String linkedManagedAccountId = TestHelper.ensureAsExpected(60,
                        () -> MultiPrivateService.createLinkedAccount(registerLinkedAccountModel, fpiKey),
                        SC_OK)
                .jsonPath()
                .get("id");

        final String scaChallengeId = ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, List.of(linkedManagedAccountId+"-"+sourceManagedAccountId)), EnrolmentChannel.SMS.name(), pluginSecretKey, corporate.getRight())
                .jsonPath()
                .get("scaChallengeId");

        ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                .verifyChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, VERIFICATION_CODE), scaChallengeId,  EnrolmentChannel.SMS.name(), pluginSecretKey, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    void VerifyOneTimePassword_WrongVerificationCode_Conflict() {

        final String corporateCurrency = Currency.GBP.name();

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(pluginCorporateProfileId)
                        .setBaseCurrency(corporateCurrency)
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry("GB")
                                .build())
                        .build();

        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(corporateDetails, pluginSecretKey);
        final IdentityModel identityModel = new IdentityModel(corporate.getLeft(), IdentityType.CORPORATE);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(pluginZeroBalanceAccountProfileId, corporateCurrency, pluginSecretKey, corporate.getRight());

        final RegisterLinkedAccountModel registerLinkedAccountModel =
                RegisterLinkedAccountModel.DefaultRegisterLinkedAccountFasterModel(identityModel, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, corporateCurrency).build();

        final String linkedManagedAccountId = TestHelper.ensureAsExpected(60,
                        () -> MultiPrivateService.createLinkedAccount(registerLinkedAccountModel, fpiKey),
                        SC_OK)
                .jsonPath()
                .get("id");

        final String scaChallengeId = ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, List.of(linkedManagedAccountId+"-"+sourceManagedAccountId)), EnrolmentChannel.SMS.name(), pluginSecretKey, corporate.getRight())
                .jsonPath()
                .get("scaChallengeId");

        ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, "000000"), scaChallengeId,  EnrolmentChannel.SMS.name(), pluginSecretKey, corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Test
    void VerifyOneTimePassword_WrongChallengeId_NotFound() {

        final String corporateCurrency = Currency.GBP.name();

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(pluginCorporateProfileId)
                        .setBaseCurrency(corporateCurrency)
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry("GB")
                                .build())
                        .build();

        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(corporateDetails, pluginSecretKey);

        ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, VERIFICATION_CODE), "",  EnrolmentChannel.SMS.name(), pluginSecretKey, corporate.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    void VerifyOneTimePassword_UnknownChannel_BadRequest() {

        final String corporateCurrency = Currency.GBP.name();

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(pluginCorporateProfileId)
                        .setBaseCurrency(corporateCurrency)
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry("GB")
                                .build())
                        .build();

        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(corporateDetails, pluginSecretKey);
        final IdentityModel identityModel = new IdentityModel(corporate.getLeft(), IdentityType.CORPORATE);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(pluginZeroBalanceAccountProfileId, corporateCurrency, pluginSecretKey, corporate.getRight());

        final RegisterLinkedAccountModel registerLinkedAccountModel =
                RegisterLinkedAccountModel.DefaultRegisterLinkedAccountFasterModel(identityModel, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, corporateCurrency).build();

        final String linkedManagedAccountId = TestHelper.ensureAsExpected(60,
                        () -> MultiPrivateService.createLinkedAccount(registerLinkedAccountModel, fpiKey),
                        SC_OK)
                .jsonPath()
                .get("id");

        final String scaChallengeId = ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, List.of(linkedManagedAccountId+"-"+sourceManagedAccountId)), EnrolmentChannel.SMS.name(), pluginSecretKey, corporate.getRight())
                .jsonPath()
                .get("scaChallengeId");

        ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, VERIFICATION_CODE), scaChallengeId,  EnrolmentChannel.UNKNOWN.name(), pluginSecretKey, corporate.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    void VerifyOneTimePassword_WrongSecretKey_BadRequest() {

        final String corporateCurrency = Currency.GBP.name();

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(pluginCorporateProfileId)
                        .setBaseCurrency(corporateCurrency)
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry("GB")
                                .build())
                        .build();

        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(corporateDetails, pluginSecretKey);
        final IdentityModel identityModel = new IdentityModel(corporate.getLeft(), IdentityType.CORPORATE);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(pluginZeroBalanceAccountProfileId, corporateCurrency, pluginSecretKey, corporate.getRight());

        final RegisterLinkedAccountModel registerLinkedAccountModel =
                RegisterLinkedAccountModel.DefaultRegisterLinkedAccountFasterModel(identityModel, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, corporateCurrency).build();

        final String linkedManagedAccountId = TestHelper.ensureAsExpected(60,
                        () -> MultiPrivateService.createLinkedAccount(registerLinkedAccountModel, fpiKey),
                        SC_OK)
                .jsonPath()
                .get("id");

        final String scaChallengeId = ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, List.of(linkedManagedAccountId+"-"+sourceManagedAccountId)), EnrolmentChannel.SMS.name(), pluginSecretKey, corporate.getRight())
                .jsonPath()
                .get("scaChallengeId");

        ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, VERIFICATION_CODE), scaChallengeId,  EnrolmentChannel.SMS.name(), "", corporate.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    void VerifyOneTimePassword_WrongToken_Unauthorized() {

        final String corporateCurrency = Currency.GBP.name();

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(pluginCorporateProfileId)
                        .setBaseCurrency(corporateCurrency)
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry("GB")
                                .build())
                        .build();

        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(corporateDetails, pluginSecretKey);
        final IdentityModel identityModel = new IdentityModel(corporate.getLeft(), IdentityType.CORPORATE);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(pluginZeroBalanceAccountProfileId, corporateCurrency, pluginSecretKey, corporate.getRight());

        final RegisterLinkedAccountModel registerLinkedAccountModel =
                RegisterLinkedAccountModel.DefaultRegisterLinkedAccountFasterModel(identityModel, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, corporateCurrency).build();

        final String linkedManagedAccountId = TestHelper.ensureAsExpected(60,
                        () -> MultiPrivateService.createLinkedAccount(registerLinkedAccountModel, fpiKey),
                        SC_OK)
                .jsonPath()
                .get("id");

        final String scaChallengeId = ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, List.of(linkedManagedAccountId+"-"+sourceManagedAccountId)), EnrolmentChannel.SMS.name(), pluginSecretKey, corporate.getRight())
                .jsonPath()
                .get("scaChallengeId");

        ChallengesService.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, VERIFICATION_CODE), scaChallengeId,  EnrolmentChannel.SMS.name(), pluginSecretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
