package opc.models.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RemoveDuplicateIdentityFlagModel {
    private String comment;

    public static RemoveDuplicateIdentityFlagModel removeComment (){
        return RemoveDuplicateIdentityFlagModel.builder()
                .comment("Not a duplicate")
                .build();
    }
}
