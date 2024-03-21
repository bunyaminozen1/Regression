package spi.openbanking.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstitutionCapabilitiesResponse {

    private int minAmount;
    private int maxAmount;
    private int minPayees;
    private int maxPayees;
    private int minReferenceLength;
    private boolean paymentSingleImmediate;
    private boolean statusSingleImmediate;
    private boolean paymentBulkImmediate;
    private boolean statusBulkImmediate;
    private String dualAuth;
    private String workingHoursOpening;
    private String workingHoursClosing;
    private boolean requiresTrustedPayeesBulk;
    private boolean closedOnWeekendsSingle;
    private boolean aggregatesBulkTransactions;
    private boolean mobileSingle;
    private boolean mobileBulk;
    private boolean closedOnWeekendsBulk;
    private boolean closedOnHolidaysSingle;
    private boolean closedOnHolidaysBulk;
}
