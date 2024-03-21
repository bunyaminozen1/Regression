package opc.models.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MakerCheckerResponseModel {
    private String action;
    private String category;
    private String createdAt;
    private String makerId;
    private String resourceId;
    private String resourceType;
    private String reviewId;
    private String status;
}
