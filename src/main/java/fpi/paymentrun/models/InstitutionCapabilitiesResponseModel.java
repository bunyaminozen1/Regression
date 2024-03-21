package fpi.paymentrun.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstitutionCapabilitiesResponseModel {
    private int maxTotal;
    private int minReferenceLength;
    private boolean requiresTrustedPayeesBulk;
    private boolean paymentSingleImmediate;
    private boolean statusSingleImmediate;
    private boolean paymentBulkImmediate;
    private boolean mobileBulk;
    private String dualAuth;

}
