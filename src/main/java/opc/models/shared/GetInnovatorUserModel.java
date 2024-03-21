package opc.models.shared;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetInnovatorUserModel {
    private String email;

    public GetInnovatorUserModel(final String email){
        this.email = email;
    }
}
