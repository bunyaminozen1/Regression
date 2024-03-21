package opc.junit.multi.multipleapps;

import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.corporates.PatchCorporateModel;
import opc.services.multi.CorporatesService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class CorporatesTests extends BaseApplicationsSetup {

    private static String corporateProfileId;
    private static String secretKey;

    @BeforeAll
    public static void TestSetup(){
        corporateProfileId = applicationTwo.getCorporatesProfileId();
        secretKey = applicationTwo.getSecretKey();
    }

    @Test
    public void CreateCorporate_Success(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo("CORPORATE"))
                .body("profileId", equalTo(corporateProfileId));
    }

    @Test
    public void CreateCorporate_OtherApplicationKey_Forbidden(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        CorporatesService.createCorporate(createCorporateModel, applicationThree.getSecretKey(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateCorporate_OtherApplicationCorporateProfile_Forbidden(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationFour.getCorporatesProfileId()).build();

        CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateCorporate_SameEmailDifferentApplications_Success(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationFour.getCorporatesProfileId()).build();

        CorporatesService.createCorporate(createCorporateModel, applicationFour.getSecretKey(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        final CreateCorporateModel otherApplicationCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationTwo.getCorporatesProfileId())
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(createCorporateModel.getRootUser().getEmail())
                                .build())
                        .build();

        CorporatesService.createCorporate(otherApplicationCorporateModel, applicationTwo.getSecretKey(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PatchCorporate_CrossApplicationUpdate_Forbidden(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationFour.getCorporatesProfileId()).build();
        final Pair<String, String> businessPayoutsCorporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, applicationFour.getSecretKey());

        final PatchCorporateModel patchCorporateModel = PatchCorporateModel.DefaultPatchCorporateModel().build();
        CorporatesService.patchCorporate(patchCorporateModel, secretKey, businessPayoutsCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

}
