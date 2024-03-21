package opc.models.multi.managedaccounts;

public class PatchManagedAccountModel {
    private final String tag;
    private final String friendlyName;

    public PatchManagedAccountModel(final String tag, final String friendlyName) {
        this.tag = tag;
        this.friendlyName = friendlyName;
    }

    public String getTag() {
        return tag;
    }

    public String getFriendlyName() {
        return friendlyName;
    }
}
