package opc.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class GetInnovatorsModel {
    private PagingLimitModel paging;
    private String modelId;

    public static GetInnovatorsModel defaultGetInnovatorsModel(String modelId){
        return GetInnovatorsModel
                .builder()
                .paging(new PagingLimitModel().setLimit(10))
                .modelId(modelId)
                .build();
    }
}
