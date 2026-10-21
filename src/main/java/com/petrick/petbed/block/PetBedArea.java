package com.petrick.petbed.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** Square, block-aligned movement area. Even sizes use offsets [-size/2, size/2-1]. */
public final class PetBedArea {
    public static final int DEFAULT_SIZE = 50;
    public static final int[] OPTIONS = {15, 30, 45, 50, 75};
    private PetBedArea() {}

    public static boolean isValid(int size) {
        for (int option : OPTIONS) if (option == size) return true;
        return false;
    }

    public static int minOffset(int size) { return -(size / 2); }
    public static int maxOffset(int size) { return minOffset(size) + size - 1; }

    public static boolean contains(BlockPos bed, double x, double z, int size) {
        int minX = bed.getX() + minOffset(size), maxX = bed.getX() + maxOffset(size);
        int minZ = bed.getZ() + minOffset(size), maxZ = bed.getZ() + maxOffset(size);
        int blockX = Mth.floor(x), blockZ = Mth.floor(z);
        return blockX >= minX && blockX <= maxX && blockZ >= minZ && blockZ <= maxZ;
    }

    public static double clampX(BlockPos bed, double x, int size) {
        return Mth.clamp(x, bed.getX() + minOffset(size) + .5D, bed.getX() + maxOffset(size) + .5D);
    }

    public static double clampZ(BlockPos bed, double z, int size) {
        return Mth.clamp(z, bed.getZ() + minOffset(size) + .5D, bed.getZ() + maxOffset(size) + .5D);
    }
}
