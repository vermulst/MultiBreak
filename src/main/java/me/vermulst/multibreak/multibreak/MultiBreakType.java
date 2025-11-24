package me.vermulst.multibreak.multibreak;

public enum MultiBreakType {
    NORMAL,
    STATIC,
    CANCELLED_STATIC;

    public boolean isStatic() {
        return this == STATIC || this == CANCELLED_STATIC;
    }
}
