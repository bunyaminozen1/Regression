package opc.junit.admin.corporates;

import opc.enums.opc.CompanyType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.admin.BaseSetupExtension;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubAuthenticatedUserDataModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BaseCorporatesSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected final static String UBO_DIRECTOR_QUESTIONNAIRE_ID = "ubo_director_questionnaire";

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel applicationTwo;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String managedAccountProfileId;
    protected static String prepaidCardProfileId;
    protected static String debitCardProfileId;
    protected static String transfersProfileId;
    protected static String secretKey;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String tenantId;
    protected static String programmeId;
    protected static String sharedKey;
    protected static String adminToken;
    protected static String impersonatedAdminToken;
    protected static String innovatorToken;

    @BeforeAll
    public static void GlobalSetup(){
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);

        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();
        tenantId = applicationOne.getInnovatorId();

        programmeId = applicationOne.getProgrammeId();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();
        managedAccountProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        prepaidCardProfileId = applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        debitCardProfileId = applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        transfersProfileId = applicationOne.getTransfersProfileId();

        secretKey = applicationOne.getSecretKey();
        sharedKey = applicationOne.getSharedKey();

        adminToken = AdminService.loginAdmin();
        impersonatedAdminToken = AdminService.impersonateTenant(tenantId, adminToken);
        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    }

    protected static String getBackofficeImpersonateToken(final String email, final IdentityType identityType){
        return BackofficeHelper.impersonateIdentity(email, identityType, secretKey);
    }

    /**
     * This method creates a new authorized user and verify email, mobile and KYC is completed
     *
     * @return user id to call api and UsersModel to check if information matches
     */
    protected Pair<String, UsersModel> startAndApproveAuthenticatedUserKyc(
            final CreateCorporateModel createCorporateModel,
            final String corporateToken,
            final CompanyType companyType) {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createEnrolledUser(usersModel, secretKey, corporateToken);

        final String kycReferenceId = UsersHelper.startUserKyc(secretKey, user.getRight());

        final IdentityDetailsModel weavrUserIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, user.getRight(), kycReferenceId)
                        .getParams();

        final SumSubAuthenticatedUserDataModel applicantData =
                SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(),
                                weavrUserIdentity.getExternalUserId())
                        .as(SumSubAuthenticatedUserDataModel.class);

        SumSubHelper.approveRepresentative(companyType, applicantData.getId(), createCorporateModel,
                weavrUserIdentity.getExternalUserId(), UBO_DIRECTOR_QUESTIONNAIRE_ID);

        return Pair.of(user.getLeft(), usersModel);
    }
}
