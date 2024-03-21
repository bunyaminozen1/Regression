package opc.models.admin;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GetReviewsResponseModel {
    private List<ReviewSummary> values;
}
