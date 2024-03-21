package opc.junit.innovator.registration;

import opc.models.innovator.*;
import opc.models.shared.PasswordModel;
import opc.services.innovator.InnovatorService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class InnovatorTests {

    @Test
    public void CreateInnovator_Success() {

        InnovatorRegistrationModel registrationModel =
                InnovatorRegistrationModel.RandomInnovatorRegistrationModel()
                        .build();

        InnovatorService.registerInnovator(registrationModel)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("innovatorId", notNullValue());
    }

    @Test
    public void CreateInnovator_ShortPassword_Conflict() {

        InnovatorRegistrationModel registrationModel =
                InnovatorRegistrationModel.RandomInnovatorRegistrationModel()
                .setPassword(new PasswordModel("12"))
                .build();

        InnovatorService.registerInnovator(registrationModel)
                .then()
                .statusCode(HttpStatus.SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));
    }
}
