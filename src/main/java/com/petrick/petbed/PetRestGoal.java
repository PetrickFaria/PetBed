package com.petrick.petbed;

import com.mojang.logging.LogUtils;
import com.petrick.petbed.api.PetBedPets;
import com.petrick.petbed.block.PetBedArea;
import com.petrick.petbed.block.PetBedBlock;
import com.petrick.petbed.block.PetBedBlockEntity;
import com.petrick.petbed.block.PetBedMode;
import com.petrick.petbed.block.PetLinkService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** Server-side return/rest goal. Uses species navigation and never loads chunks or teleports along a route. */
public final class PetRestGoal extends Goal {
    public static final String RESTING_KEY = "PetBedResting";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PATH_CHECK_INTERVAL = 20;
    private static final int STUCK_AFTER_TICKS = 80;
    private static final int MAX_RETRY_DELAY = 400;

    private final Mob pet;
    private BlockPos bedPos;
    private BlockPos approachPos;
    private int retryDelay;
    private int progressCheck;
    private int stalledTicks;
    private int failedAttempts;
    private long lastFailureLogTime = -6000L;
    private long lastAccommodationLogTime = -1200L;
    private int stuckReplans;
    private Vec3 lastProgressPosition = Vec3.ZERO;
    private float bedYaw;

    public PetRestGoal(Mob pet) {
        this.pet = pet;
        // LOOK prevents other goals from changing the resting direction while this goal owns the bed.
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    public static boolean isRestingLinkedPet(Entity entity) {
        return entity instanceof Mob mob && mob.getPersistentData().getBoolean(RESTING_KEY)
                && PetLinkService.hasLinkMarker(entity);
    }

    @Override public boolean canUse() {
        if (!(pet.level instanceof ServerLevel level) || !pet.isAlive() || !PetBedPets.isTamedPet(pet)) return false;
        bedPos = readBedPos(level);
        if (bedPos == null) return false;
        boolean night = isNight(level);
        // Recognition is a 3D radius around the mattress. Keep the bond intact and do not
        // release Vanilla sitting when either endpoint is outside the active, loaded area.
        if (night && (!level.hasChunkAt(bedPos) || !withinNightRecognitionRange(bedPos))) return false;
        // Night return outranks daytime sitting and any retry backoff carried over from roaming.
        // Clear stale rest protection before allowing the goal selector to choose another movement goal.
        if (night) {
            if (!onRestSurface() && PetBedPets.isOrderedToSit(pet)) {
                // The PetBed mode is stored separately; suspend only the entity's sit command.
                PetBedPets.setOrderedToSit(pet, false);
            }
            if (pet.getPersistentData().getBoolean(RESTING_KEY) && !onRestSurface()) {
                pet.getPersistentData().putBoolean(RESTING_KEY, false);
            }
        }
        if (retryDelay > 0 && !night) { retryDelay--; return false; }
        boolean seatedRest = PetLinkService.getMode(pet) == PetBedMode.SIT
                && pet.getPersistentData().getBoolean(RESTING_KEY) && onRestSurface();
        return night || seatedRest || (PetLinkService.getMode(pet) != PetBedMode.SIT && !insideArea());
    }

    @Override public boolean canContinueToUse() {
        if (!(pet.level instanceof ServerLevel level) || !pet.isAlive() || bedPos == null) return false;
        if (!PetLinkService.isLinkValid(pet, level, bedPos)) return false;
        boolean night = isNight(level);
        boolean seatedRest = PetLinkService.getMode(pet) == PetBedMode.SIT
                && pet.getPersistentData().getBoolean(RESTING_KEY) && onRestSurface();
        return night || seatedRest || (PetLinkService.getMode(pet) != PetBedMode.SIT && !insideArea());
    }

    @Override public void start() {
        retryDelay = 0;
        stalledTicks = 0;
        failedAttempts = 0;
        stuckReplans = 0;
        if (onRestSurface() || canAlignFromCurrentPosition()) rest();
        else if (!navigate()) scheduleRetry();
    }

    @Override public void tick() {
        if (!(pet.level instanceof ServerLevel level) || bedPos == null) return;
        boolean night = isNight(level);
        // Clear movement blockers before retry backoff. The selected mode remains persisted;
        // only Vanilla's temporary sit command is suspended while the pet returns at night.
        if (night) {
            if (!onRestSurface() && PetBedPets.isOrderedToSit(pet)) {
                PetBedPets.setOrderedToSit(pet, false);
            }
            if (pet.getPersistentData().getBoolean(RESTING_KEY) && !onRestSurface()) {
                pet.getPersistentData().putBoolean(RESTING_KEY, false);
            }
        }
        if (retryDelay > 0) { retryDelay--; return; }

        if (onRestSurface()) {
            rest();
            return;
        }
        // A short, collision-checked final alignment is attempted before retrying the normal navigation path.
        if ((night || !insideArea()) && canAlignFromCurrentPosition()) {
            rest();
            return;
        }

        if (atApproach()) {
            if (canAlignFromCurrentPosition()) {
                rest();
            } else {
                BlockPos failedApproach = approachPos;
                approachPos = null;
                if (!navigate(failedApproach)) scheduleRetry();
            }
            return;
        }

        pet.getPersistentData().putBoolean(RESTING_KEY, false);
        if (pet.getNavigation().isDone()) {
            // Navigation may stop within its tolerance radius. Confirm the reached waypoint is
            // actually close and collision-clear before the final bed alignment.
            if (approachPos != null && isNearApproach() && canAlignFromCurrentPosition()) {
                rest();
            } else if (night || !insideArea()) {
                BlockPos failedApproach = approachPos;
                approachPos = null;
                if (!navigate(failedApproach)) scheduleRetry();
            }
            return;
        }

        if (--progressCheck <= 0) {
            progressCheck = PATH_CHECK_INTERVAL;
            if (pet.position().distanceToSqr(lastProgressPosition) >= 0.09D) {
                lastProgressPosition = pet.position();
                stalledTicks = 0;
                failedAttempts = 0;
                stuckReplans = 0;
            } else {
                stalledTicks += PATH_CHECK_INTERVAL;
                if (stalledTicks >= STUCK_AFTER_TICKS) {
                    pet.getNavigation().stop();
                    stalledTicks = 0;
                    stuckReplans++;
                    if (stuckReplans >= 3) {
                        stuckReplans = 0;
                        scheduleRetry();
                    } else if (!navigate()) scheduleRetry();
                }
            }
        }
    }

    @Override public void stop() {
        pet.getPersistentData().remove(RESTING_KEY);
        if (pet.getNavigation().isInProgress()) pet.getNavigation().stop();
        bedPos = null;
        approachPos = null;
        stalledTicks = 0;
    }

    private boolean navigate() { return navigate(null); }

    private boolean navigate(BlockPos excludedApproach) {
        if (!(pet.level instanceof ServerLevel level) || bedPos == null || !level.hasChunkAt(bedPos)
                || !PetLinkService.isLinkValid(pet, level, bedPos)) return false;

        List<ApproachPath> candidates = new ArrayList<>(8);
        // Search each adjacent standing position with this species' own navigator. Reaching a
        // different side gives the pathfinder real alternatives around walls; final alignment stays short.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) != 1) continue;
                BlockPos candidate = bedPos.offset(dx, 0, dz);
                if (candidate.equals(excludedApproach) || !level.hasChunkAt(candidate)) continue;
                Path path = pet.getNavigation().createPath(candidate, 0);
                if (path != null && path.canReach()) candidates.add(new ApproachPath(candidate, path));
            }
        }
        candidates.sort(Comparator.comparingInt(candidate -> candidate.path().getNodeCount()));
        for (ApproachPath candidate : candidates) {
            if (pet.getNavigation().moveTo(candidate.path(), 1.0D)) {
                approachPos = candidate.pos();
                lastProgressPosition = pet.position();
                progressCheck = PATH_CHECK_INTERVAL;
                stalledTicks = 0;
                if (atApproach()) {
                    rest();
                }
                return true;
            }
        }
        return false;
    }

    private void scheduleRetry() {
        failedAttempts = Math.min(failedAttempts + 1, 4);
        retryDelay = Math.min(MAX_RETRY_DELAY, 50 << (failedAttempts - 1));
        if (pet.level instanceof ServerLevel level && level.getDayTime() - lastFailureLogTime >= 6000L) {
            LOGGER.warn("PetBed could not find a reachable path for pet {} to bed {} in {}. Retrying in {} ticks.",
                    pet.getUUID(), bedPos, level.dimension().location(), retryDelay);
            lastFailureLogTime = level.getDayTime();
        }
    }

    private boolean atApproach() {
        return isNearApproach() && pet.getNavigation().isDone();
    }

    private boolean isNearApproach() {
        if (approachPos == null) return false;
        double dx = pet.getX() - (approachPos.getX() + .5D);
        double dz = pet.getZ() - (approachPos.getZ() + .5D);
        return dx * dx + dz * dz <= 1.21D && Math.abs(pet.getY() - approachPos.getY()) <= 1.0D;
    }

    /**
     * Allows one short final placement only when the pet is physically close, on the bed's floor level,
     * and the complete swept body box is clear. The bed's own shape is ignored because it is the target.
     */
    private boolean canAlignFromCurrentPosition() {
        if (!(pet.level instanceof ServerLevel level) || bedPos == null || !level.hasChunkAt(bedPos)) return false;
        if (onRestSurface()) return true;

        double centerX = bedPos.getX() + .5D, centerZ = bedPos.getZ() + .5D;
        double surfaceY = bedPos.getY() + 4.0D / 16.0D;
        double dx = pet.getX() - centerX, dz = pet.getZ() - centerZ;
        if (dx * dx + dz * dz > 1.0D || Math.abs(pet.getY() - surfaceY) > 1.0D) {
            return rejectAccommodation(level, "outside one-block arrival envelope");
        }
        BlockPos current = pet.blockPosition();
        if (current.getY() != bedPos.getY() && current.getY() != bedPos.getY() + 1) {
            return rejectAccommodation(level, "pet is on a different floor level");
        }

        double moveX = centerX - pet.getX();
        double moveY = surfaceY - pet.getY();
        double moveZ = centerZ - pet.getZ();
        double distance = Math.sqrt(moveX * moveX + moveY * moveY + moveZ * moveZ);
        int samples = Math.max(1, (int) Math.ceil(distance / 0.1D));
        AABB startBox = pet.getBoundingBox();
        for (int sample = 0; sample <= samples; sample++) {
            double fraction = (double) sample / samples;
            AABB sampleBox = startBox.move(moveX * fraction, moveY * fraction, moveZ * fraction);
            String obstruction = findArrivalBlockCollision(level, sampleBox);
            if (obstruction != null) return rejectAccommodation(level, obstruction);
            if (!level.getEntities(pet, sampleBox, entity -> entity.isAlive() && entity.isPushable()).isEmpty()) {
                return rejectAccommodation(level, "another entity obstructs the arrival corridor");
            }
        }
        return true;
    }

    private String findArrivalBlockCollision(ServerLevel level, AABB box) {
        int minX = (int) Math.floor(box.minX), minY = (int) Math.floor(box.minY), minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX), maxY = (int) Math.floor(box.maxY), maxZ = (int) Math.floor(box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (pos.equals(bedPos)) continue;
            if (!level.hasChunkAt(pos)) return "arrival corridor crosses an unloaded chunk";
            var shape = level.getBlockState(pos).getCollisionShape(level, pos, CollisionContext.of(pet));
            for (AABB collision : shape.toAabbs()) {
                if (box.intersects(collision.move(pos.getX(), pos.getY(), pos.getZ()))) {
                    return "arrival corridor blocked by " + level.getBlockState(pos).getBlock() + " at " + pos;
                }
            }
        }
        return null;
    }

    private boolean rejectAccommodation(ServerLevel level, String reason) {
        long now = level.getGameTime();
        if (now - lastAccommodationLogTime >= 1200L) {
            LOGGER.warn("PetBed arrival alignment rejected for pet {} at {} toward bed {} in {}: {} (resting={}, navigationDone={})",
                    pet.getUUID(), pet.position(), bedPos, level.dimension().location(), reason,
                    pet.getPersistentData().getBoolean(RESTING_KEY), pet.getNavigation().isDone());
            lastAccommodationLogTime = now;
        }
        return false;
    }
    private boolean onRestSurface() {
        if (bedPos == null) return false;
        double dx = pet.getX() - (bedPos.getX() + .5D), dz = pet.getZ() - (bedPos.getZ() + .5D);
        double restY = bedPos.getY() + 4.0D / 16.0D;
        return dx * dx + dz * dz <= .04D && Math.abs(pet.getY() - restY) <= .08D;
    }

    private void rest() {
        if (bedPos == null || !canAlignFromCurrentPosition()) return;
        if (!PetLinkService.hasLinkMarker(pet) || !(pet.level instanceof ServerLevel level)
                || !level.hasChunkAt(bedPos) || !PetLinkService.isLinkValid(pet, level, bedPos)) return;

        pet.getNavigation().stop();
        double centerX = bedPos.getX() + .5D, centerZ = bedPos.getZ() + .5D;
        double surfaceY = bedPos.getY() + 4.0D / 16.0D;
        double dx = pet.getX() - centerX, dz = pet.getZ() - centerZ;
        if (dx * dx + dz * dz > .0004D || Math.abs(pet.getY() - surfaceY) > .025D) {
            // One exact arrival placement; later corrections occur only after meaningful external drift.
            pet.setPos(centerX, surfaceY, centerZ);
        }

        if (pet.level.getBlockState(bedPos).hasProperty(PetBedBlock.FACING)) {
            bedYaw = pet.level.getBlockState(bedPos).getValue(PetBedBlock.FACING).toYRot();
        }
        pet.setYRot(bedYaw);
        pet.yRotO = bedYaw;
        pet.yBodyRot = bedYaw;
        pet.yHeadRot = bedYaw;
        double yawRadians = Math.toRadians(bedYaw);
        pet.getLookControl().setLookAt(centerX - Math.sin(yawRadians), pet.getEyeY(), centerZ + Math.cos(yawRadians));
        pet.setDeltaMovement(Vec3.ZERO);
        pet.fallDistance = 0.0F;
        pet.getPersistentData().putBoolean(RESTING_KEY, true);
        if (PetLinkService.getMode(pet) == PetBedMode.SIT) PetBedPets.setOrderedToSit(pet, true);
    }

    private boolean withinNightRecognitionRange(BlockPos bed) {
        double dx = pet.getX() - (bed.getX() + .5D);
        double dy = pet.getY() - (bed.getY() + 4.0D / 16.0D);
        double dz = pet.getZ() - (bed.getZ() + .5D);
        return dx * dx + dy * dy + dz * dz <= 100.0D * 100.0D;
    }

    private boolean insideArea() { return bedPos != null && PetBedArea.contains(bedPos, pet.getX(), pet.getZ(), PetLinkService.getAreaSize(pet)); }
    private static boolean isNight(ServerLevel level) { long time = level.getDayTime() % 24000L; return time >= 13000L && time < 23000L; }

    private BlockPos readBedPos(ServerLevel level) {
        var data = pet.getPersistentData();
        if (!data.contains(PetLinkService.DATA_KEY)) return null;
        var marker = data.getCompound(PetLinkService.DATA_KEY);
        if (marker.hasUUID("Owner") && !marker.getUUID("Owner").equals(PetBedPets.getOwnerId(pet))) {
            BlockPos oldPos = BlockPos.of(marker.getLong("Pos"));
            if (level.hasChunkAt(oldPos) && level.getBlockEntity(oldPos) instanceof PetBedBlockEntity oldBed && pet.getUUID().equals(oldBed.getPetId())) oldBed.setLink(null, null);
            PetLinkService.removeLinkMarker(pet);
            return null;
        }
        if (!marker.getString("Dimension").equals(level.dimension().location().toString())) {
            PetLinkService.removeLinkMarker(pet);
            return null;
        }
        BlockPos pos = BlockPos.of(marker.getLong("Pos"));
        if (level.hasChunkAt(pos) && !PetLinkService.isLinkValid(pet, level, pos)) {
            PetLinkService.removeLinkMarker(pet);
            return null;
        }
        return pos;
    }

    private record ApproachPath(BlockPos pos, Path path) { }
}
