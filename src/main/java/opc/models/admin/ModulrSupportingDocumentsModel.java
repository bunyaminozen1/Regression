package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@Getter
public class ModulrSupportingDocumentsModel {


    private String reference;
    private String entityName;
    private String type;
    private String entityType;
    private String fileName;
}
