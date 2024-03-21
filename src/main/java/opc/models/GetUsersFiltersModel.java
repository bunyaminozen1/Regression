package opc.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetUsersFiltersModel {

    private String active;
    private String email;
    private String type;
    private String tag;

}

