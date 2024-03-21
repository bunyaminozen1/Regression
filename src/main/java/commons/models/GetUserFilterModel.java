package commons.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetUserFilterModel {
    private String active;
    private String abandoned;

    public GetUserFilterModel(String active, String abandoned) {
        this.active = active;
        this.abandoned = abandoned;
    }
}
