package net.idothehax.rarays.laser;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import net.minecraft.block.Blocks;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Laser {
    private final World world;
    private final PlayerEntity player;
    private final Random random;
    private static final double MAX_DISTANCE = 100.0; // Maximum distance for the raycast
    private static final double MAX_HEIGHT = 200.0; // Maximum height for laser origin
    private static final double MIN_HEIGHT = 100.0; // Minimum height for laser origin
    private static final float OSCILLATION_AMPLITUDE = 0.3f; // How far in/out the laser will move
    private static final double OSCILLATION_SPEED = 0.03f; // Speed of oscillation
    private final List<BlockDisplayElement> glassElements = new ArrayList<>();
    private final List<BlockDisplayElement> laserElements = new ArrayList<>();

    private ElementHolder holder;
    private double oscillationOffset = 0; // Offset for oscillation
    private float oscillationTime = 0.0f;

    public Laser(World world, PlayerEntity player) {
        this.world = world;
        this.player = player;
        this.random = Random.create();
    }

    public void spawnLaser() {
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d eyePos = player.getEyePos();
        Vec3d targetPos = eyePos.add(lookVec.multiply(MAX_DISTANCE));

        BlockHitResult hitResult = world.raycast(new RaycastContext(
                eyePos,
                targetPos,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        Vec3d hitPos = hitResult.getPos();

        double randomHeight = MIN_HEIGHT + random.nextDouble() * (MAX_HEIGHT - MIN_HEIGHT);
        double randomAngle = random.nextDouble() * Math.PI * 2;
        double radius = random.nextDouble() * 20.0;

        Vec3d startPos = new Vec3d(
                hitPos.x + Math.cos(randomAngle) * radius,
                randomHeight,
                hitPos.z + Math.sin(randomAngle) * radius
        );

        double dipDepth = 1.5;
        Vec3d targetPosition = hitPos.add(0, -dipDepth, 0);

        double totalDistance = startPos.distanceTo(targetPosition);
        int numberOfBlocks = Math.max((int) (totalDistance * 4), 100);
        int numberOfGlassBlocks = numberOfBlocks / 2;

        holder = new ElementHolder();

        // Create glass blocks first
        for (int i = 0; i < numberOfGlassBlocks; i++) {
            BlockDisplayElement glassElement = new BlockDisplayElement();
            glassElement.setBlockState(Blocks.WHITE_STAINED_GLASS.getDefaultState());

            double progress = (double) i / (numberOfGlassBlocks - 1);
            Vec3d pos = startPos.lerp(targetPosition, progress);

            // Minimal offset for glass
            double offsetAmount = 0.02;
            Vec3d offset = new Vec3d(
                    random.nextDouble() * offsetAmount - offsetAmount / 2,
                    random.nextDouble() * offsetAmount - offsetAmount / 2,
                    random.nextDouble() * offsetAmount - offsetAmount / 2
            );

            glassElement.setScale(new Vector3f(1.0f, 1.0f, 1.0f));
            glassElement.setOverridePos(pos.add(offset).add(0.5, 0.5, 0.5));

            glassElements.add(glassElement);
            holder.addElement(glassElement);
        }

        // Create wool blocks
        for (int i = 0; i < numberOfBlocks; i++) {
            BlockDisplayElement laserElement = new BlockDisplayElement();
            laserElement.setBlockState(Blocks.LIGHT_BLUE_WOOL.getDefaultState());

            double progress = (double) i / (numberOfBlocks - 1);
            Vec3d pos = startPos.lerp(targetPosition, progress);

            // Very small offset for tighter beam
            double offsetAmount = 0.02;
            Vec3d offset = new Vec3d(
                    random.nextDouble() * offsetAmount - offsetAmount / 2,
                    random.nextDouble() * offsetAmount - offsetAmount / 2,
                    random.nextDouble() * offsetAmount - offsetAmount / 2
            );

            laserElement.setScale(new Vector3f(0.5f, 0.5f, 0.5f));
            laserElement.setOverridePos(pos.add(offset).add(0.75, 1, 0.75));

            laserElements.add(laserElement);
            holder.addElement(laserElement);
        }

        BlockPos holderPos = new BlockPos((int) startPos.x, (int) startPos.y, (int) startPos.z);
        ChunkAttachment.ofTicking(holder, (ServerWorld) world, holderPos);

        // Start the oscillation
        startOscillation();
    }

    private void startOscillation() {
        // Reset the oscillation offset
        oscillationOffset = 0;
    }

    public void updateOscillation() {
        oscillationTime += OSCILLATION_SPEED; // Increment time
        double oscillationFactor = Math.sin(oscillationTime); // Calculate oscillation factor

        // Calculate the scale factor based on the oscillation factor
        float scale = 1.0f + (float)(oscillationFactor * OSCILLATION_AMPLITUDE);

        // Update the size of the glass blocks
        for (BlockDisplayElement glassElement : glassElements) {
            glassElement.setScale(new Vector3f(scale, scale, scale)); // Update scale for flickering effect
        }

        for (BlockDisplayElement laserElement : laserElements) {
            laserElement.setScale(new Vector3f((float) (0.5 * scale), (float) (0.5 * scale), (float) (0.5 * scale))); // Update scale for flickering effect
        }
    }
}