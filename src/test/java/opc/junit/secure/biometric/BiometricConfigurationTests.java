package opc.junit.secure.biometric;

import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.secure.DeviceIdModel;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class BiometricConfigurationTests extends BaseBiometricSetup{

    @Test
    public void BiometricConfiguration_CorporateUsersGetPasscodeLength_Success(){
        final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateModel, secretKey);
        final String corporateDeviceId = SecureHelper.enrolAndGetDeviceId(corporate.getRight(), corporate.getLeft(), passcodeApp);

        final UsersModel userModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledBiometricAuthenticatedUser(userModel, sharedKey, secretKey, corporate.getRight());
        final String userDeviceId = SecureHelper.enrolAndGetDeviceId(user.getRight(), user.getLeft(), passcodeApp);

        SecureService.getBiometricConfiguration(sharedKey, new DeviceIdModel(corporateDeviceId))
                .then()
                .statusCode(SC_OK)
                .body("passcodeLength", equalTo(4));

        SecureService.getBiometricConfiguration(sharedKey, new DeviceIdModel(userDeviceId))
                .then()
                .statusCode(SC_OK)
                .body("passcodeLength", equalTo(4));
    }

    @Test
    public void BiometricConfiguration_ConsumerUsersGetPasscodeLength_Success(){
        final CreateConsumerModel consumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerModel, secretKey);
        final String corporateDeviceId = SecureHelper.enrolAndGetDeviceId(consumer.getRight(), consumer.getLeft(), passcodeApp);

        final UsersModel userModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledBiometricAuthenticatedUser(userModel, sharedKey, secretKey, consumer.getRight());
        final String userDeviceId = SecureHelper.enrolAndGetDeviceId(user.getRight(), user.getLeft(), passcodeApp);

        SecureService.getBiometricConfiguration(sharedKey, new DeviceIdModel(corporateDeviceId))
                .then()
                .statusCode(SC_OK)
                .body("passcodeLength", equalTo(4));

        SecureService.getBiometricConfiguration(sharedKey, new DeviceIdModel(userDeviceId))
                .then()
                .statusCode(SC_OK)
                .body("passcodeLength", equalTo(4));
    }
}
