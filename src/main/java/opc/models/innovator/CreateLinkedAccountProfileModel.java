package opc.models.innovator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Builder
@Getter
@Setter
public class CreateLinkedAccountProfileModel {

    private String code;
    private String payletTypeCode;
    private List<String> tag;
    private List<String> currency;
    private List<String> country;
    private List<FeeDetailsModel> group;

    public static CreateLinkedAccountProfileModel DefaultCreateLinkedAccountProfileModel() {
        return CreateLinkedAccountProfileModel.builder()
                .code("default_linked_accounts")
                .payletTypeCode("default_linked_accounts")
                .currency(Collections.singletonList("GBP"))
                .country(Collections.singletonList("GB"))
                .build();
    }
}