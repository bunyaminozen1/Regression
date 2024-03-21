package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import opc.enums.opc.PermissionType;
import opc.models.shared.PagingModel;

import java.util.List;

@Data
@Builder

public class GetPermissionsListModel {
    private List<PermissionType> types;
    private PagingModel paging;
}