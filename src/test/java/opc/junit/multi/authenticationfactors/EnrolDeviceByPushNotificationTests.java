package opc.junit.multi.authenticationfactors;

import opc.enums.authy.AuthyMessage;
import opc.junit.database.AuthySimulatorDatabaseHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.authfactors.AuthyEnrolmentNotificationModel;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
public class EnrolDeviceByPushNotificationTests extends BaseAuthenticationFactorsSetup {

    @Test
    public void EnrolUser_AuthyNotificationCheck_Success() throws SQLException, IOException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        AuthenticationFactorsHelper.enrolAuthyPushUser(secretKey, corporate.getRight());

        final Map<String, String> notification = AuthySimulatorDatabaseHelper.getNotification(applicationOne.getProgrammeId()).get(0);
        assertEquals(String.format(AuthyMessage.ENROL.getMessage(), applicationOne.getProgrammeName()), notification.get("message"));

        AuthyEnrolmentNotificationModel keyValuePair =
                new ObjectMapper().readValue(notification.get("details"), AuthyEnrolmentNotificationModel.class);
        assertEquals(String.format("%s %s", createCorporateModel.getRootUser().getName(), createCorporateModel.getRootUser().getSurname()),
                keyValuePair.getName());
        assertEquals(createCorporateModel.getRootUser().getEmail(), keyValuePair.getEmail());
    }
}
