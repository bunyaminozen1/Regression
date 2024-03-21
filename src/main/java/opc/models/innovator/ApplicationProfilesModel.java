package opc.models.innovator;

import java.util.List;

public class ApplicationProfilesModel {

    private String programmeName;
    private String programmeCode;
    private String programmeId;
    private List<String> corporatesProfileId;
    private List<String> consumersProfileId;
    private List<String> managedAccountsProfileId;
    private List<String> managedCardsProfileId;
    private List<String> sendProfileId;
    private List<String> transfersProfileId;
    private List<String> owtProfileId;
    private String sharedKey;
    private String secretKey;

    public String getProgrammeName() {
        return programmeName;
    }

    public ApplicationProfilesModel setProgrammeName(String programmeName) {
        this.programmeName = programmeName;
        return this;
    }

    public String getProgrammeCode() {
        return programmeCode;
    }

    public ApplicationProfilesModel setProgrammeCode(String programmeCode) {
        this.programmeCode = programmeCode;
        return this;
    }

    public String getProgrammeId() {
        return programmeId;
    }

    public ApplicationProfilesModel setProgrammeId(String programmeId) {
        this.programmeId = programmeId;
        return this;
    }

    public List<String> getCorporatesProfileId() {
        return corporatesProfileId;
    }

    public ApplicationProfilesModel setCorporatesProfileId(List<String> corporatesProfileId) {
        this.corporatesProfileId = corporatesProfileId;
        return this;
    }

    public List<String> getConsumersProfileId() {
        return consumersProfileId;
    }

    public ApplicationProfilesModel setConsumersProfileId(List<String> consumersProfileId) {
        this.consumersProfileId = consumersProfileId;
        return this;
    }

    public List<String> getManagedAccountsProfileId() {
        return managedAccountsProfileId;
    }

    public ApplicationProfilesModel setManagedAccountsProfileId(List<String> managedAccountsProfileId) {
        this.managedAccountsProfileId = managedAccountsProfileId;
        return this;
    }

    public List<String> getManagedCardsProfileId() {
        return managedCardsProfileId;
    }

    public ApplicationProfilesModel setManagedCardsProfileId(List<String> managedCardsProfileId) {
        this.managedCardsProfileId = managedCardsProfileId;
        return this;
    }

    public List<String> getSendProfileId() {
        return sendProfileId;
    }

    public ApplicationProfilesModel setSendProfileId(List<String> sendProfileId) {
        this.sendProfileId = sendProfileId;
        return this;
    }

    public List<String> getTransfersProfileId() {
        return transfersProfileId;
    }

    public ApplicationProfilesModel setTransfersProfileId(List<String> transfersProfileId) {
        this.transfersProfileId = transfersProfileId;
        return this;
    }

    public List<String> getOwtProfileId() {
        return owtProfileId;
    }

    public ApplicationProfilesModel setOwtProfileId(List<String> owtProfileId) {
        this.owtProfileId = owtProfileId;
        return this;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public ApplicationProfilesModel setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
        return this;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public ApplicationProfilesModel setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }
}
