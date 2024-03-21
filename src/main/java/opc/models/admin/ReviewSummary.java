package opc.models.admin;

import lombok.Data;

@Data
public class ReviewSummary {
    private String category;
    private String resourceType;
    private int count;
}
