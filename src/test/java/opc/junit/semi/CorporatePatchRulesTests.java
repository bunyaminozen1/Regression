package opc.junit.semi;

import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.corporates.PatchCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.services.multi.CorporatesService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class CorporatePatchRulesTests extends BaseSemiSetup {

    /**
     * SEMI tests for CorporatePatch flow. There is the way to link user with corporate patch endpoint. If user provide same name, surname and email,
     * corporates has to be linked. Also, if user who wants to link corporate, using this endpoint, doesn't pass KYC, linking is not allowed
     */

    @Test
    public void CorporatePatch_CorporateNotKybPatchRootUserData_Success() {
        //Create corporate root user with success patch with root user which has verified KYC
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String identityId = CorporatesHelper.createCorporate(createCorporateModel, secretKey);
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String corporatePassword = CorporatesHelper.createCorporatePassword(identityId, secretKey);
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        final String corporateAuthenticationToken = AuthenticationHelper.login(corporateRootEmail, corporatePassword, secretKey);

        PatchCorporateModel patchCorporateModel = PatchCorporateModel.DefaultPatchCorporateModel().setEmail(String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                RandomStringUtils.randomAlphabetic(5))).build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CorporatePatch_CorporateNotKybUserRootUserWithoutKycAuthorisedUserWithKyc_Conflict() {
        //Create corporate root user with KYC
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String identityId = CorporatesHelper.createCorporate(createCorporateModel, secretKey);
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();

        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        CorporatesHelper.verifyKyb(secretKey, identityId);

        //Create corporate root user without KYC
        final CreateCorporateModel createCorporateModelSecondIdentity = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String identityIdNotLinked = CorporatesHelper.createCorporate(createCorporateModelSecondIdentity, secretKey);
        final String corporateRootEmailNotLinked = createCorporateModelSecondIdentity.getRootUser().getEmail();
        final String corporatePasswordNotLinked = CorporatesHelper.createCorporatePassword(identityIdNotLinked, secretKey);
        CorporatesHelper.verifyEmail(corporateRootEmailNotLinked, secretKey);
        final String corporateAuthenticationTokenNotLinked = AuthenticationHelper.login(corporateRootEmailNotLinked, corporatePasswordNotLinked, secretKey);

        PatchCorporateModel patchCorporateModel = PatchCorporateModel.DefaultPatchCorporateModel().setEmail(corporateRootEmail).build();
        CorporatesService.patchCorporate(patchCorporateModel, secretKey, corporateAuthenticationTokenNotLinked, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void CorporatePatch_AuthenticatedUserDoesNotPassKycLinkToUserWhichPassKyc_Conflict() {
        //create corporate with KYC
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String identityId = CorporatesHelper.createCorporate(createCorporateModel, secretKey);
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();

        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        CorporatesHelper.verifyKyb(secretKey, identityId);

        //Create corporate root user with authenticated user which does not pass KYC
        final CreateCorporateModel createCorporateModelSecondIdentity = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String identityIdNotLinked = CorporatesHelper.createCorporate(createCorporateModelSecondIdentity, secretKey);
        final String corporateRootEmailNotLinked = createCorporateModelSecondIdentity.getRootUser().getEmail();
        final String corporatePasswordNotLinked = CorporatesHelper.createCorporatePassword(identityIdNotLinked, secretKey);
        CorporatesHelper.verifyEmail(corporateRootEmailNotLinked, secretKey);
        final String corporateAuthenticationTokenNotLinked = AuthenticationHelper.login(corporateRootEmailNotLinked, corporatePasswordNotLinked, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final String userId = UsersHelper.createUser(usersModel, secretKey, corporateAuthenticationTokenNotLinked);
        final String userEmail = usersModel.getEmail();

        PatchCorporateModel patchCorporateModel = PatchCorporateModel.DefaultPatchCorporateModel().setEmail(userEmail).build();
        CorporatesService.patchCorporate(patchCorporateModel, secretKey, corporateAuthenticationTokenNotLinked, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));

        final UsersModel usersModelNotLinked = UsersModel.DefaultUsersModel().setEmail(corporateRootEmail).build();
        UsersService.patchUser(usersModelNotLinked, secretKey, userId, corporateAuthenticationTokenNotLinked, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));

    }

}
