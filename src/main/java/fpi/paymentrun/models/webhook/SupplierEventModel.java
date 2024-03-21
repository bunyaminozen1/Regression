package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Getter
@Setter
@Builder
public class SupplierEventModel {
    private String address;
    private LinkedHashMap<String, String> bankAccountDetails;
    private String bankAddress;
    private String bankCountry;
    private String bankName;
    private String name;
}
