package fpi.paymentrun.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreatePaymentRunResponseModel {

    private String id;
    private String status;
    private List<PaymentsResponseModel> payments;
    private String createdBy;
    private String createdAt;
    private String paymentRunRef;
    private String tag;
    private String description;
    private String paymentDate;
}