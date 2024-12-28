package org.ulearnstatistics.model;

public enum Sex {
    UNKNOWN(0), FEMALE(1), MALE(2);

    private final int id;
    Sex(int id) { this.id = id; }
    public int getValue() { return id; }
}
