package opc.junit.multi.consumers;

import opc.enums.opc.ConsumerSourceOfFunds;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ConsumersService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetConsumersTests extends BaseConsumersSetup {
    private static String consumerId;
    private static String authenticationToken;
    private static CreateConsumerModel createConsumerModel;

    @BeforeAll
    public static void Setup(){
        createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        authenticationToken = authenticatedConsumer.getRight();
    }

    @Test
    public void GetConsumers_Success(){
        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo(IdentityType.CONSUMER.name()))
                .body("id.id", notNullValue())
                .body("profileId", equalTo(consumerProfileId))
                .body("tag", equalTo(createConsumerModel.getTag()))
                .body("rootUser.id.type", equalTo(IdentityType.CONSUMER.name()))
                .body("rootUser.id.id", notNullValue())
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(createConsumerModel.getRootUser().getEmail()))
                .body("rootUser.mobile.countryCode", equalTo(createConsumerModel.getRootUser().getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(createConsumerModel.getRootUser().getMobile().getNumber()))
                .body("rootUser.dateOfBirth.year", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getYear()))
                .body("rootUser.dateOfBirth.month", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.day", equalTo(createConsumerModel.getRootUser().getDateOfBirth().getDay()))
                .body("rootUser.occupation", equalTo(createConsumerModel.getRootUser().getOccupation().toString()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true))
                .body("rootUser.mobileNumberVerified", equalTo(false))
                .body("rootUser.address.addressLine1", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine1()))
                .body("rootUser.address.addressLine2", equalTo(createConsumerModel.getRootUser().getAddress().getAddressLine2()))
                .body("rootUser.address.city", equalTo(createConsumerModel.getRootUser().getAddress().getCity()))
                .body("rootUser.address.postCode", equalTo(createConsumerModel.getRootUser().getAddress().getPostCode()))
                .body("rootUser.address.state", equalTo(createConsumerModel.getRootUser().getAddress().getState()))
                .body("rootUser.address.country", equalTo(createConsumerModel.getRootUser().getAddress().getCountry()))
                .body("rootUser.placeOfBirth", equalTo(createConsumerModel.getRootUser().getPlaceOfBirth()))
                .body("rootUser.nationality", equalTo(createConsumerModel.getRootUser().getNationality()))
                .body("sourceOfFunds", equalTo(createConsumerModel.getSourceOfFunds() == null ? null : createConsumerModel.getSourceOfFunds().toString()))
                .body("sourceOfFundsOther",
                        equalTo(createConsumerModel.getSourceOfFunds().equals(ConsumerSourceOfFunds.OTHER) ?
                                createConsumerModel.getSourceOfFundsOther() : null))
                .body("acceptedTerms", equalTo(createConsumerModel.getAcceptedTerms()))
                .body("ipAddress", equalTo(createConsumerModel.getIpAddress()))
                .body("baseCurrency", equalTo(createConsumerModel.getBaseCurrency()))
                .body("feeGroup", equalTo("DEFAULT"))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetConsumers_InvalidApiKey_Unauthorised(){

        ConsumersService.getConsumers("abc", authenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetConsumers_NoApiKey_Unauthorised(){

        ConsumersService.getConsumers("", authenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetConsumers_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ConsumersService.getConsumers(secretKey, authenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetConsumers_RootUserLoggedOut_Unauthorised(){
        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        ConsumersService.getConsumers(secretKey, token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetConsumers_BackofficeImpersonator_Forbidden(){
        ConsumersService.getConsumers(secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }
}
