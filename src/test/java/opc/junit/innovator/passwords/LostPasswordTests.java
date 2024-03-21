package opc.junit.innovator.passwords;

import opc.models.multi.passwords.LostPasswordStartModel;
import opc.services.innovator.InnovatorService;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;

public class LostPasswordTests extends BasePasswordsSetup{
    @Test
    public void StartLostPassword_UnknownUser_NoContent(){
        InnovatorService.startLostPassword(new LostPasswordStartModel("fakeemail@hello.com"))
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}
