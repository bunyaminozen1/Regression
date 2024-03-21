package opc.models.admin;

import java.util.List;

public class SubscribersModel {

    private SubscriberIdModel subscriberId;
    private List<SubscriptionsModel> subscriptions;

    public SubscriberIdModel getSubscriberId() {
        return subscriberId;
    }

    public List<SubscriptionsModel> getSubscriptions() {
        return subscriptions;
    }
}
