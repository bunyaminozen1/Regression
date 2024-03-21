package opc.junit.innovatornew.consumers;

import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.ConsumersFilterModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.innovator.UpdateConsumerModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.AddressModel;
import opc.services.admin.AdminService;
import opc.services.innovatornew.InnovatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClosedIdentityFlagTests extends BaseConsumersSetup{

    private static CreateConsumerModel createConsumerModel;
    private static Pair<String, String> consumer;
    private static Pair<String, String> authorizedUser;


    @BeforeAll
    public static void setup() throws SQLException {

        createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        authorizedUser = UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());

        AdminHelper.deactivateConsumerUser(consumer.getLeft(), authorizedUser.getLeft(), impersonatedAdminToken);
        AdminHelper.deactivateConsumerUser(consumer.getLeft(), consumer.getLeft(), impersonatedAdminToken);

        AdminHelper.deactivateConsumer(
                new DeactivateIdentityModel(false, "ACCOUNT_REVIEW"), consumer.getLeft(), impersonatedAdminToken);

        AdminService.consumerIdentityClosure(consumer.getLeft(), impersonatedAdminToken)
                .then()
                .statusCode(SC_OK);

        assertEquals("1", ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0).get("permanently_closed"));
    }

    @Test
    public void IdentityClosureFlag_GetAllConsumers_Success(){
        final ConsumersFilterModel filterModel = ConsumersFilterModel.builder()
                .setEmail(createConsumerModel.getRootUser().getEmail())
                .build();
        InnovatorService.getConsumers(filterModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("consumerWithKyc[0].consumer.id.id", equalTo(consumer.getLeft()))
                .body("consumerWithKyc[0].consumer.permanentlyClosed", equalTo(true));
    }

    @Test
    public void IdentityClosureFlag_FilterAllPermanentlyClosedConsumers_Success(){
        final ConsumersFilterModel filterModel = ConsumersFilterModel.builder()
                .setPermanentlyClosed("TRUE")
                .build();

        InnovatorService.getConsumers(filterModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("consumerWithKyc[0].consumer.permanentlyClosed", equalTo(true));
    }

    @Test
    public void IdentityClosureFlag_GetConsumer_Success(){

        InnovatorService.getConsumer(consumer.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("id.id", equalTo(consumer.getLeft()))
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(createConsumerModel.getRootUser().getEmail()))
                .body("permanentlyClosed", equalTo(true));

    }

    @Test
    public void IdentityClosureFlag_UpdateConsumer_Success(){

        final UpdateConsumerModel updateConsumerModel = UpdateConsumerModel.builder()
                .address(AddressModel.RandomAddressModel())
                .build();

        InnovatorService.updateConsumer(updateConsumerModel, innovatorToken, consumer.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id.id", equalTo(consumer.getLeft()))
                .body("address.addressLine1", equalTo(updateConsumerModel.getAddress().getAddressLine1()))
                .body("address.addressLine2", equalTo(updateConsumerModel.getAddress().getAddressLine2()))
                .body("address.city", equalTo(updateConsumerModel.getAddress().getCity()))
                .body("address.country", equalTo(updateConsumerModel.getAddress().getCountry()))
                .body("address.postCode", equalTo(updateConsumerModel.getAddress().getPostCode()))
                .body("address.state", equalTo(updateConsumerModel.getAddress().getState()))
                .body("permanentlyClosed", equalTo(true));
    }

    @Test
    public void IdentityClosureFlag_ActivateConsumer_ClosedIdentity(){
        InnovatorService.activateConsumer(new ActivateIdentityModel(true), consumer.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CLOSED_IDENTITY"));
    }


    @Test
    public void IdentityClosureFlag_ActivateConsumerUser_ClosedIdentity(){

        //Try to activate root user
        InnovatorService.activateConsumerUser(consumer.getLeft(), consumer.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CLOSED_IDENTITY"));

        //Try to activate authorized user
        InnovatorService.activateConsumerUser(consumer.getLeft(), authorizedUser.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CLOSED_IDENTITY"));
    }
}
