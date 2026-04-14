package com.sentinelcore.defense;

import java.util.List;

public record DefenseResult(
    boolean blocked,
    boolean refused,
    List<String> redactions,
    String reason
) {
    public static DefenseResult allowed() {
        return new DefenseResult(false, false, List.of(), null);
    }

    public static DefenseResult blocked(String reason) {
        return new DefenseResult(true, false, List.of(), reason);
    }

    public static DefenseResult refused(String reason) {
        return new DefenseResult(false, true, List.of(), reason);
    }
}