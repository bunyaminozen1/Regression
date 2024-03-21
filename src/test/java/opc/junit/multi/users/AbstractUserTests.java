package opc.junit.multi.users;

import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.models.multi.users.UsersModel;
import opc.tags.MultiTags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.SQLException;
import java.util.Map;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.USERS)
public abstract class AbstractUserTests extends BaseUsersSetup {

    protected abstract String getSecretKey();

    protected abstract String getAuthToken();

    protected abstract User createNewUser();

    protected abstract String createPassword(String userId);

    protected abstract String getRootEmail();

    protected static class User {

        protected String id;
        protected UsersModel userDetails;
        protected String identityId;
        protected IdentityType identityType;
        protected UserType userType;

        public User(final String id, final UsersModel userDetails, final String identityId, final IdentityType identityType, final UserType userType) {
            this.id = id;
            this.userDetails = userDetails;
            this.identityId = identityId;
            this.identityType = identityType;
            this.userType = userType;
        }
    }

    protected static String getBackofficeImpersonateToken(final String identityId, final IdentityType identityType){
        return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
    }

    protected static Map<Integer, Map<String, String>> getEmailNonce(final String userId, final IdentityType identityType) throws SQLException {
        return identityType.equals(IdentityType.CONSUMER) ?
                ConsumersDatabaseHelper.getEmailNonce(userId) :
                CorporatesDatabaseHelper.getEmailNonce(userId);
    }
}
