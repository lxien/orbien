package io.github.lxien.orbien.core.filetransfer;

import io.github.lxien.orbien.core.message.Message;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

/**
 * 文件列表排序常量与比较器。
 */
public final class FileListSort {

    public static final String NAME = "name";
    public static final String KIND = "kind";
    public static final String LAST_OPENED = "last_opened";
    public static final String DATE_ADDED = "date_added";
    public static final String MODIFIED = "modified";
    public static final String CREATED = "created";
    public static final String SIZE = "size";

    private static final Set<String> VALID = Set.of(
            NAME, KIND, LAST_OPENED, DATE_ADDED, MODIFIED, CREATED, SIZE);

    private static final Collator NAME_COLLATOR = Collator.getInstance(Locale.SIMPLIFIED_CHINESE);
    static {
        NAME_COLLATOR.setStrength(Collator.PRIMARY);
    }

    private FileListSort() {
    }

    public static boolean isValid(String sort) {
        return sort != null && !sort.isBlank() && VALID.contains(sort);
    }

    public static Comparator<Message.FileEntry> comparator(String sort) {
        if (!isValid(sort)) {
            return null;
        }
        Comparator<Message.FileEntry> ascending = switch (sort) {
            case NAME -> Comparator.comparing(Message.FileEntry::getName, NAME_COLLATOR);
            case KIND -> Comparator.comparing(FileListSort::kindKey, NAME_COLLATOR);
            case LAST_OPENED -> Comparator.comparingLong(Message.FileEntry::getLastAccessTime);
            case DATE_ADDED, CREATED -> Comparator.comparingLong(Message.FileEntry::getCreatedTime);
            case MODIFIED -> Comparator.comparingLong(Message.FileEntry::getModifiedTime);
            case SIZE -> Comparator.comparingLong(e -> e.getDirectory() ? -1L : e.getSize());
            default -> null;
        };
        return ascending == null ? null : ascending.reversed();
    }

    private static String kindKey(Message.FileEntry entry) {
        if (entry.getDirectory()) {
            return "\u0000folder";
        }
        String name = entry.getName();
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot >= name.length() - 1) {
            return "\u0001";
        }
        return "\u0001" + name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
