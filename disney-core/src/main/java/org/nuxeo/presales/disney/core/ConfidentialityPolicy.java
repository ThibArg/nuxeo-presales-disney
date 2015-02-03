package org.nuxeo.presales.disney.core;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.*;
import org.nuxeo.ecm.core.security.AbstractSecurityPolicy;
import org.nuxeo.ecm.core.security.SecurityPolicy;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import java.security.Principal;
import java.util.List;

/**
 * Created by Michaël on 03/02/2015.
 */
public class ConfidentialityPolicy
        extends AbstractSecurityPolicy implements SecurityPolicy {

    @Override
    public Access checkPermission(
            Document document, ACP mergedAcp, Principal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {

        String level = null;

        try {
            level = (String) document.getPropertyValue("security:level");
        } catch (Exception e) {
            e.printStackTrace();
            return Access.UNKNOWN;
        }

        if ("confidential".equals(level) && CredentialChecker.hasCredential(principal)) {
            return Access.GRANT;
        } else {
            return Access.DENY;
        }
    }

    @Override
    public boolean isExpressibleInQuery(String repositoryName) {
        return true;
    }

    @Override
    public SQLQuery.Transformer getQueryTransformer(String repositoryName) {
        return CONFIDENTIALITY_TRANSFORMER;
    }

    public static final SQLQuery.Transformer CONFIDENTIALITY_TRANSFORMER = new ConfidentialityTransformer();

    /**
     * Sample Transformer that adds {@code AND security:level <> 'confidential'} to the query.
     */
    public static class ConfidentialityTransformer implements SQLQuery.Transformer {

        /**
         * {@code security:level <> 'confidential'}
         */
        public static final Predicate NO_CONFIDENTIAL = new Predicate(
                new Reference("security:level"), Operator.NOTEQ, new StringLiteral("confidential"));

        public static final Predicate OTHERS = new Predicate(
                new Reference("security:level"), Operator.ISNULL, null);

        @Override
        public SQLQuery transform(Principal principal, SQLQuery query) {
            
            if (principal.getName().equals("system") || CredentialChecker.hasCredential(principal)) return query;
            
            WhereClause where = query.where;
            Predicate predicate;
            if (where == null || where.predicate == null) {
                predicate = new Predicate(NO_CONFIDENTIAL, Operator.OR, OTHERS);
            } else {
                // adds an AND security:level <> 'confidential' to the WHERE clause
                predicate = new Predicate(NO_CONFIDENTIAL, Operator.OR, OTHERS);
                predicate = new Predicate(predicate, Operator.AND, where.predicate);
            }
            // return query with updated WHERE clause
            return new SQLQuery(query.select, query.from, new WhereClause(predicate),
                    query.groupBy, query.having, query.orderBy, query.limit, query.offset);
        }
    }

    
    public static class CredentialChecker {
        public static boolean hasCredential(Principal principal) {
            UserManager um = Framework.getLocalService(UserManager.class);
            NuxeoPrincipal nxPrincipal = um.getPrincipal(principal.getName());
            List<String> groups = nxPrincipal.getGroups();
            return groups.contains("powers") || groups.contains("administrators") || 
                    "system".equals(principal.getName());
        }
    }

}
