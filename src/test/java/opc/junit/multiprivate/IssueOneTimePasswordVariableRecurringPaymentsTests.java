package opc.junit.multiprivate;

import commons.enums.Currency;
import commons.models.CompanyModel;
import opc.enums.mailhog.MailHogSms;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ResourceType;
import opc.helpers.ChallengesModelHelper;
import opc.junit.database.AuthFactorsSimulatorDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.backoffice.IdentityModel;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multiprivate.RegisterLinkedAccountModel;
import opc.services.multiprivate.ChallengesService;
import opc.services.multiprivate.MultiPrivateService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IssueOneTimePasswordVariableRecurringPaymentsTests extends BaseMultiPrivateSetup {
    @Test
    public void IssueOTP_HappyPath_Success() throws SQLException {

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

        ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, List.of(linkedManagedAccountId+"-"+sourceManagedAccountId)), EnrolmentChannel.SMS.name(), pluginSecretKey, corporate.getRight())
                .then()
                .statusCode(SC_OK);

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(corporateDetails.getRootUser().getMobile().getNumber());
        assertEquals(MailHogSms.SCA_VARIABLE_RECURRING_PAYMENTS.getFrom(), sms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", corporateDetails.getRootUser().getMobile().getCountryCode(), corporateDetails.getRootUser().getMobile().getNumber()), sms.getTo());
        assertEquals(String.format(MailHogSms.SCA_VARIABLE_RECURRING_PAYMENTS.getSmsText(), AuthFactorsSimulatorDatabaseHelper.getLatestFakeOtp(corporate.getLeft()).get(0).get("token")), sms.getBody());
    }

    @Test
    public void IssueOTP_NoSecretKey_BadRequest() {

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

        ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, List.of(linkedManagedAccountId+"-"+sourceManagedAccountId)), EnrolmentChannel.SMS.name(), "", corporate.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void IssueOTP_UnknownChannel_BadRequest() {

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

        ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, List.of(linkedManagedAccountId+"-"+sourceManagedAccountId)), EnrolmentChannel.UNKNOWN.name(), pluginSecretKey, corporate.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void IssueOTP_WrongToken_Unauthorized() {

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

        ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, List.of(linkedManagedAccountId+"-"+sourceManagedAccountId)), EnrolmentChannel.SMS.name(), pluginSecretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueOTP_WrongResourceId_BadRequest() {

        final String corporateCurrency = Currency.GBP.name();

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(pluginCorporateProfileId)
                        .setBaseCurrency(corporateCurrency)
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setRegistrationCountry("GB")
                                .build())
                        .build();

        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(corporateDetails, pluginSecretKey);

        ChallengesService.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.VARIABLE_RECURRING_PAYMENTS, List.of(" "+"-"+" ")), EnrolmentChannel.SMS.name(), pluginSecretKey, corporate.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }
}
