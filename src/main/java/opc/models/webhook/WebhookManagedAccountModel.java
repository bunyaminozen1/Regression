package opc.models.webhook;

import opc.models.shared.BankDetailsModel;
import opc.models.shared.ManagedInstrumentBalance;

public class WebhookManagedAccountModel {

    private WebhookIdModel id;
    private String profileId;
    private String tag;
    private WebhookIdModel owner;
    private String friendlyName;
    private boolean active;
    private String currency;
    private ManagedInstrumentBalance balances;
    private WebhookManagedInstrumentState state;
    private String creationTimestamp;
    private BankDetailsModel bankAccountDetails;

    public WebhookManagedAccountModel() {
    }

    public WebhookIdModel getId() {
        return id;
    }

    public void setId(final WebhookIdModel id) {
        this.id = id;
    }

    public WebhookIdModel getOwner() {
        return owner;
    }

    public void setOwner(final WebhookIdModel owner) {
        this.owner = owner;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(final String profileId) {
        this.profileId = profileId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(final String tag) {
        this.tag = tag;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(final String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public ManagedInstrumentBalance getBalances() {
        return balances;
    }

    public void setBalances(final ManagedInstrumentBalance balances) {
        this.balances = balances;
    }

    public WebhookManagedInstrumentState getState() {
        return state;
    }

    public void setState(final WebhookManagedInstrumentState state) {
        this.state = state;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(final String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public BankDetailsModel getBankAccountDetails() {
        return bankAccountDetails;
    }

    public void setBankAccountDetails(final BankDetailsModel bankAccountDetails) {
        this.bankAccountDetails = bankAccountDetails;
    }
}
