package opc.junit.admin.corporates;

import commons.models.GetUserFilterModel;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.innovator.CorporatesFilterModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

@Execution(ExecutionMode.CONCURRENT)
public class AbandonedUserTests extends BaseCorporatesSetup{

    final private static int VERIFICATION_TIME_LIMIT = 90;

    /**
     * We introduced a new field for root users named abandoned that is a boolean. It checks if a root user has verified email
     * within 60 minutes, so it has ability to login and complete onboarding
     */

    @Test
    public void Corporate_RootUserNotVerifiedEmail_AbandonedTrue() throws SQLException, InterruptedException {

        final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final String firstCorporateId = CorporatesHelper.createCorporate(corporateModel, secretKey);

        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);
        CorporatesDatabaseHelper.updateUser(firstCorporateId, "0");

        AdminService.getCorporateUser(adminToken, firstCorporateId, firstCorporateId)
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(false))
                .body("abandoned", equalTo(true));

        AdminService.getCorporateAllUser(Optional.empty(), firstCorporateId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].active", equalTo(false))
                .body("user[0].abandoned", equalTo(true));

        Assertions.assertEquals("0", CorporatesDatabaseHelper.getCorporateUser(firstCorporateId).get(0).get("canceled"));

        // Create a new corporate with the same credentials
        final String secondCorporateId = CorporatesHelper.createAuthenticatedCorporate(corporateModel, secretKey).getLeft();

        AdminService.getCorporateUser(adminToken, secondCorporateId, secondCorporateId)
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true))
                .body("abandoned", equalTo(false));

        AdminService.getCorporateAllUser(Optional.empty(), secondCorporateId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].active", equalTo(true))
                .body("user[0].abandoned", equalTo(false));

        //Canceled field in DB should be 1
        Assertions.assertEquals("1", CorporatesDatabaseHelper.getCorporateUser(firstCorporateId).get(0).get("canceled"));

        //User should not be retrieved because it is canceled
        AdminService.getCorporateAllUser(Optional.empty(), firstCorporateId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].active", nullValue())
                .body("user[0].abandoned", nullValue());

        //User should not be found because it is canceled
        AdminService.getCorporateUser(adminToken, firstCorporateId, firstCorporateId)
                .then()
                .statusCode(SC_NOT_FOUND);

        //Get all corporates to make sure this change doesn't affect the endpoint
        AdminService.getCorporates(CorporatesFilterModel.builder().build(), adminToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void Corporate_RootUserVerifiedEmailWithinTime_AbandonedFalse(){

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AdminService.getCorporateUser(adminToken, corporate.getLeft(), corporate.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true))
                .body("abandoned", equalTo(false));

        AdminService.getCorporateAllUser(Optional.empty(), corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].active", equalTo(true))
                .body("user[0].abandoned", equalTo(false));
    }

    @Test
    public void Corporates_GetUserWithFilterAbandoned_Success() throws SQLException {

        final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateModel, secretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporate.getRight());

        //Deactivate root user update selected_login column in order to set user abandoned
        CorporatesDatabaseHelper.updateUser(corporate.getLeft(), "0");
        AdminHelper.deactivateCorporateUser(corporate.getLeft(), corporate.getLeft(), adminToken);

        //"active": "TRUE", "abandoned": "TRUE"
        //It returns just auth user because abandoned filter doesn't work if active field is true
        final GetUserFilterModel firstFilter = GetUserFilterModel.builder().active("TRUE").abandoned("TRUE").build();
        AdminService.getCorporateAllUser(Optional.of(firstFilter), corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(user.getLeft()))
                .body("user[0].active", equalTo(true))
                .body("user[0].abandoned", equalTo(false));

        //"active": "FALSE", "abandoned": "TRUE"
        //It returns just root user because abandoned filter works and only root user is abandoned
        final GetUserFilterModel secondFilter = GetUserFilterModel.builder().active("FALSE").abandoned("TRUE").build();
        AdminService.getCorporateAllUser(Optional.of(secondFilter), corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(corporate.getLeft()))
                .body("user[0].active", equalTo(false))
                .body("user[0].abandoned", equalTo(true));

        //"active": "FALSE", "abandoned": "FALSE"
        //It returns empty response because there is no user is inactive and is not abandoned at the same time
        final GetUserFilterModel thirdFilter = GetUserFilterModel.builder().active("FALSE").abandoned("FALSE").build();
        AdminService.getCorporateAllUser(Optional.of(thirdFilter), corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", nullValue())
                .body("user[0].active", nullValue())
                .body("user[0].abandoned", nullValue());

        //Deactivate auth user
        AdminHelper.deactivateCorporateUser(corporate.getLeft(), user.getLeft(), adminToken);

        //"active": "FALSE", "abandoned": "TRUE"
        //It returns just root user because only root user is abandoned
        final GetUserFilterModel fourthFilter = GetUserFilterModel.builder().active("FALSE").abandoned("TRUE").build();
        AdminService.getCorporateAllUser(Optional.of(fourthFilter), corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(corporate.getLeft()))
                .body("user[0].active", equalTo(false))
                .body("user[0].abandoned", equalTo(true));

        //"active": "FALSE", "abandoned": "FALSE"
        //It returns just auth user because only auth user is  NOT abandoned
        final GetUserFilterModel fifthFilter = GetUserFilterModel.builder().active("FALSE").abandoned("FALSE").build();
        AdminService.getCorporateAllUser(Optional.of(fifthFilter), corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(user.getLeft()))
                .body("user[0].active", equalTo(false))
                .body("user[0].abandoned", equalTo(false));
    }
}
