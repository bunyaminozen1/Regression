package opc.enums.opc;

public enum WebhookType {
    MANAGED_ACCOUNT("/managed_accounts/watch"),
    MANAGED_ACCOUNT_DEPOSIT("/managed_accounts/deposits/watch"),
    CORPORATE_KYB("/corporates/kyb/watch"),
    CONSUMER_KYC("/consumers/kyc/watch"),
    AUTHORISATION("/managed_cards/authorisations/watch"),
    SETTLEMENT("/managed_cards/settlements/watch"),
    TRANSFERS("/transfers/watch"),
    SENDS("/send/watch"),
    OWT("/outgoing_wire_transfers/watch"),
    AUTHENTICATION_FACTORS("/authentication_factors/watch"),
    CORPORATE_BENEFICIARY_KYB("/corporates/kyb/beneficiaries/watch"),
    ODD_MANDATE("/outgoing_direct_debits/mandates/watch"),
    ODD_COLLECTION("/outgoing_direct_debits/collections/watch"),
    MANUAL_TRANSACTION ("/manual_transactions/watch"),
    CHARGE_FEES("/fees/watch"),
    AUTH_FORWARDING("/managed_cards_authorisation_forwarding"),
    STEP_UP("/stepup/watch"),
    LOGIN ("/login/watch"),
    BENEFICIARIES_BATCH ("/beneficiaries/batch/watch"),
    CORPORATES_DEACTIVATED ("/corporates/corporate_deactivated/watch"),
    CONSUMERS_DEACTIVATED ("/consumers/consumer_deactivated/watch"),
    CORPORATES_ACTIVATED ("/corporates/corporate_activated/watch"),
    CONSUMERS_ACTIVATED ("/consumers/consumer_activated/watch");

    private final String value;

    WebhookType(final String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
