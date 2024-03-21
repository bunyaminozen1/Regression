package opc.junit.multi.multipleapps;

import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.VerificationModel;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.multi.AuthenticationService;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;

public class AuthenticationTests extends BaseApplicationsSetup {

    @Test
    public void Login_OtherApplicationKey_Forbidden() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationTwo.getConsumersProfileId()).build();
        ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, applicationTwo.getSecretKey());

        AuthenticationService.loginWithPassword(new LoginModel(createConsumerModel.getRootUser().getEmail(),
                new PasswordModel(TestHelper.getDefaultPassword(applicationTwo.getSecretKey()))), applicationFour.getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void Enrol_OtherApplicationKey_Forbidden() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationTwo.getConsumersProfileId()).build();
        final String consumerToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, applicationTwo.getSecretKey()).getRight();

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), applicationFour.getSecretKey(), consumerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyEnrolment_OtherApplicationKey_Forbidden() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationTwo.getConsumersProfileId()).build();
        final String consumerToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, applicationTwo.getSecretKey()).getRight();

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), applicationTwo.getSecretKey(), consumerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel("123456"), EnrolmentChannel.SMS.name(),
                applicationFour.getSecretKey(), consumerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetAuthenticationFactors_OtherApplicationKey_Forbidden() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationTwo.getConsumersProfileId()).build();
        final String consumerToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, applicationTwo.getSecretKey()).getRight();

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), applicationTwo.getSecretKey(), consumerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel("123456"), EnrolmentChannel.SMS.name(),
                applicationFour.getSecretKey(), consumerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }
}
