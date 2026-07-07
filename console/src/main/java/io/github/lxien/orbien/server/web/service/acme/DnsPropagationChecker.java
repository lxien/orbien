package io.github.lxien.orbien.server.web.service.acme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
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

    public enum TxtCheckStatus {
        MATCHED,
        NOT_FOUND,
        LOOKUP_UNAVAILABLE
    }

    public TxtCheckStatus checkTxt(String recordName, String expectedValue) {
        List<String> values = lookupTxt(recordName);
        if (values == null) {
            return TxtCheckStatus.LOOKUP_UNAVAILABLE;
        }
        if (values.isEmpty()) {
            return TxtCheckStatus.NOT_FOUND;
        }
        String normalizedExpected = normalizeTxt(expectedValue);
        boolean matched = values.stream().anyMatch(value -> normalizeTxt(value).equals(normalizedExpected));
        return matched ? TxtCheckStatus.MATCHED : TxtCheckStatus.NOT_FOUND;
    }

    /**
     * @return TXT 值列表；查询成功但无记录返回空列表；DNS 不可解析返回 null
     */
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
            return result;
        } catch (NamingException e) {
            logger.debug("DNS TXT 不可解析: {} ({})", recordName, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.debug("DNS TXT 查询异常: {} ({})", recordName, e.getMessage());
            return null;
        }
    }

    private String normalizeTxt(String value) {
        return value == null ? "" : value.trim();
    }
}
