package org.codice.ui.admin.ldap.config;

import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.BASE_GROUP_DN;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.BASE_USER_DN;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.BIND_USER_DN;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.BIND_USER_PASS;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.ENCRYPTION_METHOD;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.HOST_NAME;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.PORT;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.QUERY;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.QUERY_BASE;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.QUERY_RESULTS;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.USERNAME_ATTRIBUTE;

import org.codice.ui.admin.wizard.config.Configuration;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;

public class LdapConfiguration extends Configuration<LDAP_CONFIGURATION_KEYS> {

    public LdapConfiguration hostName(String hostname) {
        addValue(HOST_NAME, hostname);
        return this;
    }

    public static final String LDAPS = "ldaps";

    public static final String TLS = "tls";

    public static final String NONE = "none";

    public static final ImmutableList<String> LDAP_ENCRYPTION_METHODS = ImmutableList.of(LDAPS, TLS, NONE);

    public LdapConfiguration query(String searchBase, String ldapQuery) {
        queryBase(searchBase);
        addValue(QUERY, ldapQuery);
        return this;
    }

    public LdapConfiguration queryBase(String searchBase) {
        addValue(QUERY_BASE, searchBase);
        return this;
    }

    public String hostName() {
        return getValue(HOST_NAME) == null ? null : (String) getValue(HOST_NAME);
    }

    public String queryBase() {
        return getValue(QUERY_BASE) == null ? null : (String) getValue(QUERY_BASE);
    }

    public String query() {
        return getValue(QUERY) == null ? null : (String) getValue(QUERY);
    }

    public LdapConfiguration port(String port) {
        addValue(PORT, port);
        return this;
    }

    public int port() {
        return getValue(PORT) == null ? null : Integer.valueOf(((Double)getValue(PORT)).intValue());
    }

    public LdapConfiguration encryptionMethod(String encryptionMethod) {
        if(!LDAP_ENCRYPTION_METHODS.contains(encryptionMethod)) {
            // TODO: tbatie - 11/2/16 - Throw exception or something if they add an unsupported encryption method
        }
        addValue(ENCRYPTION_METHOD, encryptionMethod);
        return this;
    }

    public String encryptionMethod() {
        return getValue(ENCRYPTION_METHOD) == null ? null : (String) getValue(ENCRYPTION_METHOD);
    }

    public LdapConfiguration bindUserDN(String bindUserDN) {
        addValue(BIND_USER_DN, bindUserDN);
        return this;
    }

    public String bindUserDN() {
        return getValue(BIND_USER_DN) == null ? null : (String) getValue(BIND_USER_DN);
    }

    public LdapConfiguration bindUserPassword(String bindUserPass) {
        addValue(BIND_USER_PASS, bindUserPass);
        return this;
    }

    public String bindUserPassword() {
        return getValue(BIND_USER_PASS) == null ? null : (String) getValue(BIND_USER_PASS);
    }

    public List<Map<String, String>> queryResult() {
        return getValue(ENCRYPTION_METHOD) == null ? null : (List<Map<String, String>>) getValue(QUERY_RESULTS);

    }

    public String baseUserDn() {
        return getValue(BASE_USER_DN) == null ? null : (String) getValue(BASE_USER_DN);
    }

    public LdapConfiguration baseUserDn(String baseUserDn) {
        addValue(BASE_USER_DN, baseUserDn);
        return this;
    }

    public String baseGroupDn() {
        return getValue(BASE_GROUP_DN) == null ? null : (String) getValue(BASE_GROUP_DN);
    }

    public LdapConfiguration baseGroupDn(String baseGroupDn) {
        addValue(BASE_GROUP_DN, baseGroupDn);
        return this;
    }

    public LdapConfiguration queryResult(List<Map<String, String>> queryResults) {
        addValue(QUERY_RESULTS, queryResults);
        return this;
    }

    public LdapConfiguration userNameAttribute(String userNameAttriute) {
        addValue(USERNAME_ATTRIBUTE, userNameAttriute);
        return this;
    }

    public String userNameAttribute() {
        return getValue(USERNAME_ATTRIBUTE) == null ? null : (String) getValue(USERNAME_ATTRIBUTE);
    }

    @Override
    public LdapConfiguration copy() {
        LdapConfiguration newConfig = new LdapConfiguration();
        for(Map.Entry<LDAP_CONFIGURATION_KEYS, Object> entry : getValues().entrySet()) {
            newConfig.addValue(entry.getKey(), entry.getValue());
        }
        return newConfig;
    }

    public enum LDAP_CONFIGURATION_KEYS {
        QUERY, QUERY_BASE, HOST_NAME, PORT, ENCRYPTION_METHOD, BASE_USER_DN, USERNAME_ATTRIBUTE, BASE_GROUP_DN, BIND_USER_DN, BIND_USER_PASS, QUERY_RESULTS
    }
}
