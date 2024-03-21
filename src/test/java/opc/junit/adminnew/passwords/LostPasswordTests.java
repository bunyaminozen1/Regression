package opc.junit.adminnew.passwords;

import opc.models.multi.passwords.LostPasswordStartModel;
import opc.services.adminnew.AdminService;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;

public class LostPasswordTests extends BasePasswordsSetup {
    @Test
    public void StartLostPassword_UnknownUser_NoContent(){
        AdminService.startLostPassword(new LostPasswordStartModel("fakeemail@hello.com"))
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}
