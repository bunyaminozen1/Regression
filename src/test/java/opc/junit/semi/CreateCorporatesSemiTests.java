package opc.junit.semi;

import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.multi.CorporatesService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;

public class CreateCorporatesSemiTests extends BaseSemiSetup {

    /**
     * SEMI flow for CreateCorporate endpoint. This is another way how to link identities with corporate. If user pass KYC and if user provide
     * same name, surname and email, corporates are linked.
     */

    @Test
    void CreateCorporateRootUser_NewIdentityLinked_Success() {
        //Root user is created
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);

        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //New identity linked to root user with the same email, name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(corporateRootEmail, name, surname);

        CorporatesService.createCorporate(createCorporateUser, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    void CreateCorporateRootUser_NewIdentityDoNotLinked_Conflict() {
        //Root user is created
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();

        CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);

        //New identity linked to root user with the same email, and random name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(corporateRootEmail, RandomStringUtils.randomAlphabetic(10),
                        RandomStringUtils.randomAlphabetic(10));

        CorporatesService.createCorporate(createCorporateUser, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_EMAIL_NOT_UNIQUE"));
    }

    /**
     * Test case for the Innovator with disabled SEMI
     */
    @Test
    void CreateCorporateRootUser_SemiDisabledNewIdentityLinked_Conflict() {
        //Create Root User
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileIdAppOne).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKeyAppOne);

        //New identity linked to root user with the same email, name and surname
        final CreateCorporateModel createCorporateUser =
                createNonFpsCorporateUser(corporateRootEmail, name, surname);

        CorporatesService.createCorporate(createCorporateUser, secretKeyAppOne, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_EMAIL_NOT_UNIQUE"));

    }

    @Test
    void CreateCorporate_RootUserDidNotPassKYC_Conflict() {
        //crate 1st corporate without root user pass KYC
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        CorporatesHelper.createCorporate(createCorporateModel, secretKey);

        //create 2nd corporate and try to link them - expected 409
        final CreateCorporateModel createCorporateModelWithRootUser = createCorporateUser(corporateRootEmail, name, surname);
        CorporatesService.createCorporate(createCorporateModelWithRootUser, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROOT_EMAIL_NOT_VERIFIED"));

    }

    @Test
    void CreateCorporateRootUser_PasswordReused_Success() {
        //Create corporate - property passwordReused should be false because corporate is not linked
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //create 2nd corporate and linking corporate with first one - passwordReused has to be true
        final CorporateRootUserModel corporateRootUserModel = CorporateRootUserModel.DefaultRootUserModel()
                .setEmail(corporateRootEmail)
                .setName(name)
                .setSurname(surname)
                .build();

        final CreateCorporateModel createCorporateModelForLinking = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                .setRootUser(corporateRootUserModel)
                .build();

        CorporatesService.createCorporate(createCorporateModelForLinking, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.passwordAlreadySet", equalTo(true));
    }

    @Test
    @Disabled("Currently not possible since payment model accepts only one profile")
    void CreateCorporate_ProfileMismatchSameProgramme_Conflict() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);

        //different corporate profileId
        final String corporatesProfileIdTwo = "109896916125614088";

        //New user with the same name, surname and email created under corporatesProfileIdTwo
        final CreateCorporateModel createCorporateUserToLinked =
                CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileIdTwo)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(corporateRootEmail)
                                .setName(name)
                                .setSurname(surname).build()).build();

        //Link new user to corporate root user
        CorporatesService.createCorporate(createCorporateUserToLinked, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROFILE_NOT_FOUND"));
    }

    @Test
    void CreateCorporate_ProfileMismatchDifferentProgrammes_Forbidden() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, passcodeAppSecretKey);
        CorporatesHelper.verifyEmail(corporateRootEmail, passcodeAppSecretKey);

        //New user with the same name, surname and email created under application two
        final CreateCorporateModel createCorporateUserToLinked =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(corporateRootEmail)
                                .setName(name)
                                .setSurname(surname).build()).build();
        //Link new user to corporate root user
        CorporatesService.createCorporate(createCorporateUserToLinked, passcodeAppSecretKey, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }


    private static CreateCorporateModel createCorporateUser(String email, String name, String surname) {
        return
                CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(email)
                                .setName(name)
                                .setSurname(surname)
                                .build()).build();

    }

    private static CreateCorporateModel createNonFpsCorporateUser(String email, String name, String surname) {
        return
                CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileIdAppOne)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(email)
                                .setName(name)
                                .setSurname(surname)
                                .build()).build();

    }
}
