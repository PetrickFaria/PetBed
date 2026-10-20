package com.petrick.petbed.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

final class PetBedBlockEntityTicker {
    private PetBedBlockEntityTicker() {}
    static void tick(Level level, BlockPos pos, PetBedBlockEntity bed) {
        // Intentionally no world-wide entity search; entities resolve their own UUID-backed link when they tick.
    }
}
