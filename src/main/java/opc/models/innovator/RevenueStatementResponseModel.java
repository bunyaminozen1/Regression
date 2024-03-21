package opc.models.innovator;

import java.util.ArrayList;
import java.util.List;

public class RevenueStatementResponseModel {

    private List<RevenueResponseModel> statement = new ArrayList<>();
    private int count;
    private int responseCount;

    public List<RevenueResponseModel> getStatement() {
        return statement;
    }

    public RevenueStatementResponseModel setStatement(List<RevenueResponseModel> statement) {
        this.statement = statement;
        return this;
    }

    public int getCount() {
        return count;
    }

    public RevenueStatementResponseModel setCount(int count) {
        this.count = count;
        return this;
    }

    public int getResponseCount() {
        return responseCount;
    }

    public RevenueStatementResponseModel setResponseCount(int responseCount) {
        this.responseCount = responseCount;
        return this;
    }
}
