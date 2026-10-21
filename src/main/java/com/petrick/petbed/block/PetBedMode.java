package com.petrick.petbed.block;

/** Server-authoritative behavior selected for a linked pet. */
public enum PetBedMode {
    FREE("free"), SIT("sit"), FOLLOW("follow");

    private final String id;
    PetBedMode(String id) { this.id = id; }
    public String id() { return id; }

    public static PetBedMode fromId(String id) {
        for (PetBedMode mode : values()) if (mode.id.equals(id)) return mode;
        return FREE;
    }
}
