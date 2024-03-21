package opc.models.webhook;

import java.util.List;

public class WebhookResponse {

    private List<WebhookDataResponse> data;
    private int total;
    private int per_page;
    private int current_page;
    private boolean is_last_page;
    private int from;
    private int to;

    public List<WebhookDataResponse> getData() {
        return data;
    }

    public void setData(List<WebhookDataResponse> data) {
        this.data = data;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPer_page() {
        return per_page;
    }

    public void setPer_page(int per_page) {
        this.per_page = per_page;
    }

    public int getCurrent_page() {
        return current_page;
    }

    public void setCurrent_page(int current_page) {
        this.current_page = current_page;
    }

    public boolean isIs_last_page() {
        return is_last_page;
    }

    public void setIs_last_page(boolean is_last_page) {
        this.is_last_page = is_last_page;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }
}
