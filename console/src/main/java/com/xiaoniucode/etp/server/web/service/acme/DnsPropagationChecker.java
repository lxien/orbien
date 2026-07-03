package com.xiaoniucode.etp.server.web.service.acme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@Component
public class DnsPropagationChecker {

    private static final Logger logger = LoggerFactory.getLogger(DnsPropagationChecker.class);

    public boolean txtExists(String recordName, String expectedValue) {
        List<String> values = lookupTxt(recordName);
        if (values.isEmpty()) {
            return false;
        }
        String normalizedExpected = normalizeTxt(expectedValue);
        return values.stream().anyMatch(value -> normalizeTxt(value).equals(normalizedExpected));
    }

    private List<String> lookupTxt(String recordName) {
        List<String> result = new ArrayList<>();
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");
        try {
            DirContext context = new InitialDirContext(env);
            Attributes attrs = context.getAttributes(recordName, new String[]{"TXT"});
            Attribute txt = attrs.get("TXT");
            if (txt == null) {
                return result;
            }
            NamingEnumeration<?> values = txt.getAll();
            while (values.hasMore()) {
                Object value = values.next();
                result.add(String.valueOf(value).replace("\"", ""));
            }
        } catch (Exception e) {
            logger.debug("DNS TXT 查询失败: {}", recordName, e);
        }
        return result;
    }

    private String normalizeTxt(String value) {
        return value == null ? "" : value.trim();
    }
}
