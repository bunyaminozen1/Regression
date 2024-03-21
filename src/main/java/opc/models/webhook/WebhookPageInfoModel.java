package opc.models.webhook;

public class WebhookPageInfoModel {
    private int total_count;
    private int excluded_count;
    private String start_cursor;
    private String end_cursor;
    private int count;

    public int getTotal_count() {
        return total_count;
    }

    public WebhookPageInfoModel setTotal_count(int total_count) {
        this.total_count = total_count;
        return this;
    }

    public int getExcluded_count() {
        return total_count;
    }

    public WebhookPageInfoModel setExcluded_count(int excluded_count) {
        this.excluded_count = excluded_count;
        return this;
    }

    public String getStart_cursor() {
        return start_cursor;
    }

    public WebhookPageInfoModel setStart_cursor(String start_cursor) {
        this.start_cursor = start_cursor;
        return this;
    }

    public String getEnd_cursor() {
        return end_cursor;
    }

    public WebhookPageInfoModel setEnd_cursor(String end_cursor) {
        this.end_cursor = end_cursor;
        return this;
    }

    public int getCount() {
        return count;
    }

    public WebhookPageInfoModel setCount(int count) {
        this.count = count;
        return this;
    }
}
