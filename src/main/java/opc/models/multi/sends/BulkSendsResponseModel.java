package opc.models.multi.sends;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkSendsResponseModel {

    private List<BulkSendResponseModel> response;
}
