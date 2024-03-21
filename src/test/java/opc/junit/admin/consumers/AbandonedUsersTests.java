package opc.junit.admin.consumers;

import commons.models.GetUserFilterModel;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.innovator.ConsumersFilterModel;
import opc.models.multi.consumers.CreateConsumerModel;
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
public class AbandonedUsersTests extends BaseConsumersSetup{

    final private static int VERIFICATION_TIME_LIMIT = 90;

    /**
     * We introduced a new field for root users named abandoned that is a boolean. It checks if a root user has verified email
     * within 60 minutes, so it has ability to login and complete onboarding
     */

    @Test
    public void Consumer_RootUserNotVerifiedEmail_AbandonedTrue() throws InterruptedException, SQLException {

        final CreateConsumerModel consumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final String firstConsumerId = ConsumersHelper.createConsumer(consumerModel, secretKey);

        //Waiting 90 sec because of delay time in the config
        TimeUnit.SECONDS.sleep(VERIFICATION_TIME_LIMIT);

        AdminService.getConsumerUser(adminToken, firstConsumerId, firstConsumerId)
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(false))
                .body("abandoned", equalTo(true));

        AdminService.getConsumerAllUser(Optional.empty(), firstConsumerId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].active", equalTo(false))
                .body("user[0].abandoned", equalTo(true));

        //Canceled field in DB should be 0
        Assertions.assertEquals("0", ConsumersDatabaseHelper.getConsumerUser(firstConsumerId).get(0).get("canceled"));

        // Create a new consumer with the same credentials
        final String secondConsumerId = ConsumersHelper.createAuthenticatedConsumer(consumerModel, secretKey).getLeft();

        AdminService.getConsumerUser(adminToken, secondConsumerId, secondConsumerId)
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true))
                .body("abandoned", equalTo(false));

        AdminService.getConsumerAllUser(Optional.empty(), secondConsumerId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].active", equalTo(true))
                .body("user[0].abandoned", equalTo(false));

        //Canceled field in DB should be 1
        Assertions.assertEquals("1", ConsumersDatabaseHelper.getConsumerUser(firstConsumerId).get(0).get("canceled"));

        //User should not be retrieved because it is canceled
        AdminService.getConsumerAllUser(Optional.empty(), firstConsumerId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].active", nullValue())
                .body("user[0].abandoned", nullValue());

        //User should not be found because it is canceled
        AdminService.getConsumerUser(adminToken, firstConsumerId, firstConsumerId)
                .then()
                .statusCode(SC_NOT_FOUND);

        //Get all consumers to make sure this change doesn't affect the endpoint
        AdminService.getConsumers(ConsumersFilterModel.builder().build(), adminToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void Consumer_RootUserVerifiedEmailWithinTime_AbandonedFalse(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AdminService.getConsumerUser(adminToken, consumer.getLeft(), consumer.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true))
                .body("abandoned", equalTo(false));

        AdminService.getConsumerAllUser(Optional.empty(), consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].active", equalTo(true))
                .body("user[0].abandoned", equalTo(false));
    }

    @Test
    public void Consumer_GetUserWithFilterAbandoned_Success() throws SQLException {

        final CreateConsumerModel consumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerModel, secretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());

        //Deactivate root user update selected_login column in order to set user abandoned
        ConsumersDatabaseHelper.updateUser(consumer.getLeft(), "0");
        AdminHelper.deactivateConsumerUser(consumer.getLeft(), consumer.getLeft(), adminToken);

        //"active": "TRUE", "abandoned": "TRUE"
        //It returns just auth user because abandoned filter doesn't work if active field is true
        final GetUserFilterModel firstFilter = GetUserFilterModel.builder().active("TRUE").abandoned("TRUE").build();
        AdminService.getConsumerAllUser(Optional.of(firstFilter), consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(user.getLeft()))
                .body("user[0].active", equalTo(true))
                .body("user[0].abandoned", equalTo(false));

        //"active": "FALSE", "abandoned": "TRUE"
        //It returns just root user because abandoned filter works and only root user is abandoned
        final GetUserFilterModel secondFilter = GetUserFilterModel.builder().active("FALSE").abandoned("TRUE").build();
        AdminService.getConsumerAllUser(Optional.of(secondFilter), consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(consumer.getLeft()))
                .body("user[0].active", equalTo(false))
                .body("user[0].abandoned", equalTo(true));

        //"active": "FALSE", "abandoned": "FALSE"
        //It returns empty response because there is no user is inactive and is not abandoned at the same time
        final GetUserFilterModel thirdFilter = GetUserFilterModel.builder().active("FALSE").abandoned("FALSE").build();
        AdminService.getConsumerAllUser(Optional.of(thirdFilter), consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", nullValue())
                .body("user[0].active", nullValue())
                .body("user[0].abandoned", nullValue());

        //Deactivate auth user
        AdminHelper.deactivateConsumerUser(consumer.getLeft(), user.getLeft(), adminToken);

        //"active": "FALSE", "abandoned": "TRUE"
        //It returns just root user because only root user is abandoned
        final GetUserFilterModel fourthFilter = GetUserFilterModel.builder().active("FALSE").abandoned("TRUE").build();
        AdminService.getConsumerAllUser(Optional.of(fourthFilter), consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(consumer.getLeft()))
                .body("user[0].active", equalTo(false))
                .body("user[0].abandoned", equalTo(true));

        //"active": "FALSE", "abandoned": "FALSE"
        //It returns just auth user because only auth user is  NOT abandoned
        final GetUserFilterModel fifthFilter = GetUserFilterModel.builder().active("FALSE").abandoned("FALSE").build();
        AdminService.getConsumerAllUser(Optional.of(fifthFilter), consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(user.getLeft()))
                .body("user[0].active", equalTo(false))
                .body("user[0].abandoned", equalTo(false));
    }
}
