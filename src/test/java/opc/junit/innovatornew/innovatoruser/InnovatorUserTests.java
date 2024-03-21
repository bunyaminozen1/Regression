package opc.junit.innovatornew.innovatoruser;

import io.restassured.path.json.JsonPath;
import opc.services.innovatornew.InnovatorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InnovatorUserTests extends  BaseInnovatorSetup {

    @Test
    public void getCurrentUser() {

        JsonPath jsonPath = InnovatorService.getCurrentUser(innovatorToken).body().jsonPath();

        String id = jsonPath.getString("id");
        String email = jsonPath.getString("email");
        String name = jsonPath.getString("name");

        Assertions.assertEquals(id, applicationOne.getInnovatorId());
        Assertions.assertEquals(email, applicationOne.getInnovatorEmail());
        Assertions.assertEquals(name, applicationOne.getInnovatorName());

    }

}
