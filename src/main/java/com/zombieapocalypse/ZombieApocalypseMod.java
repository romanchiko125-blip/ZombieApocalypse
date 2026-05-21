package com.zombieapocalypse;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
codex/create-zombie-apocalypse-mod-for-minecraft-czw1ku
import net.minecraft.util.math.AxisAlignedBB;
=======
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
main
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@Mod(modid = ZombieApocalypseMod.MODID, name = ZombieApocalypseMod.NAME, version = ZombieApocalypseMod.VERSION)
public class ZombieApocalypseMod {
    public static final String MODID = "zombieapocalypse";
    public static final String NAME = "Zombie Apocalypse";
    public static final String VERSION = "1.0.0";

    private static final double DETECTION_RANGE = 1000.0D;
    private static final int JUMP_COOLDOWN_TICKS = 20 * 20;
    private static final int DASH_COOLDOWN_TICKS = 10 * 20;
    private static final int VILLAGER_RETALIATE_COOLDOWN_TICKS = 20 * 20;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @SubscribeEvent
    public void onZombieUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityZombie)) {
            return;
        }

        EntityZombie zombie = (EntityZombie) event.getEntityLiving();
        World world = zombie.world;
        if (world.isRemote) {
            return;
        }

        if (!zombie.getEntityData().getBoolean("za_speed_boost")) {
            zombie.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED)
                    .setBaseValue(0.46D); // ~2x обычного зомби
            zombie.getEntityData().setBoolean("za_speed_boost", true);
        }

        EntityLivingBase target = findNearestTarget(zombie, DETECTION_RANGE);
        if (target == null) {
            return;
        }

        zombie.getNavigator().tryMoveToEntityLiving(target, 1.4D);
        zombie.setAttackTarget(target);

        int jumpCd = zombie.getEntityData().getInteger("za_jump_cd");
        int dashCd = zombie.getEntityData().getInteger("za_dash_cd");

        if (jumpCd > 0) zombie.getEntityData().setInteger("za_jump_cd", jumpCd - 1);
        if (dashCd > 0) zombie.getEntityData().setInteger("za_dash_cd", dashCd - 1);

        double dist = zombie.getDistance(target);

        // Прыжок, если до цели <= 5 блоков, кд 20 секунд
        if (dist <= 5.0D && jumpCd <= 0) {
            Vec3d dir = new Vec3d(target.posX - zombie.posX, 0, target.posZ - zombie.posZ).normalize();
            zombie.motionX += dir.x * 0.9D;
            zombie.motionY = 0.55D;
            zombie.motionZ += dir.z * 0.9D;
            zombie.velocityChanged = true;
            zombie.getEntityData().setInteger("za_jump_cd", JUMP_COOLDOWN_TICKS);
        }

        // Рывок каждые 10 секунд: +2 блока импульс, отталкивание 1 блок и 0.5 сердца урона
        if (dist <= 4.0D && dashCd <= 0) {
            Vec3d dir = new Vec3d(target.posX - zombie.posX, 0, target.posZ - zombie.posZ).normalize();
            zombie.motionX += dir.x * 0.6D;
            zombie.motionZ += dir.z * 0.6D;
            zombie.velocityChanged = true;

            target.knockBack(zombie, 1.0F, -dir.x, -dir.z);
            target.attackEntityFrom(DamageSource.causeMobDamage(zombie), 1.0F); // 0.5 сердца

            spawnDashParticles(world, zombie);
            zombie.playSound(SoundEvents.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, 1.0F, 1.2F);
            zombie.getEntityData().setInteger("za_dash_cd", DASH_COOLDOWN_TICKS);
        }
    }

    @SubscribeEvent
    public void onVillagerUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntityLiving();
        World world = villager.world;
        if (world.isRemote) {
            return;
        }

        EntityZombie nearestZombie = findNearestZombie(villager, 24.0D);
        if (nearestZombie == null) {
            return;
        }

        // Житель убегает
        Vec3d away = new Vec3d(villager.posX - nearestZombie.posX, 0, villager.posZ - nearestZombie.posZ);
        if (away.lengthSquared() > 1.0E-4D) {
            away = away.normalize();
            double fleeX = villager.posX + away.x * 8.0D;
            double fleeZ = villager.posZ + away.z * 8.0D;
            villager.getNavigator().tryMoveToXYZ(fleeX, villager.posY, fleeZ, 0.95D);
        }

        int retaliateCd = villager.getEntityData().getInteger("za_villager_retaliate_cd");
        if (retaliateCd > 0) {
            villager.getEntityData().setInteger("za_villager_retaliate_cd", retaliateCd - 1);
        }

        // Контратака раз в 20 секунд: 1 сердце урона
        if (retaliateCd <= 0 && villager.getDistance(nearestZombie) <= 2.2D) {
            nearestZombie.attackEntityFrom(DamageSource.causeMobDamage(villager), 2.0F);
            nearestZombie.knockBack(villager, 0.6F, villager.posX - nearestZombie.posX, villager.posZ - nearestZombie.posZ);
            villager.getEntityData().setInteger("za_villager_retaliate_cd", VILLAGER_RETALIATE_COOLDOWN_TICKS);
            villager.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, 0.9F);
        }
    }

    private EntityLivingBase findNearestTarget(EntityZombie zombie, double range) {
        AxisAlignedBB box = zombie.getEntityBoundingBox().grow(range);
        List<EntityLivingBase> candidates = zombie.world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                e -> (e instanceof EntityPlayer || e instanceof EntityVillager) && e.isEntityAlive());

        EntityLivingBase nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (EntityLivingBase candidate : candidates) {
            if (candidate == zombie) continue;
            double d = zombie.getDistanceSq(candidate);
            if (d < nearestSq) {
                nearestSq = d;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private EntityZombie findNearestZombie(EntityVillager villager, double range) {
        AxisAlignedBB box = villager.getEntityBoundingBox().grow(range);
        List<EntityZombie> zombies = villager.world.getEntitiesWithinAABB(EntityZombie.class, box, Entity::isEntityAlive);

        EntityZombie nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (EntityZombie z : zombies) {
            double d = villager.getDistanceSq(z);
            if (d < nearestSq) {
                nearestSq = d;
                nearest = z;
            }
        }
        return nearest;
    }

    private void spawnDashParticles(World world, EntityCreature zombie) {
        for (int i = 0; i < 8; i++) {
            double ox = (world.rand.nextDouble() - 0.5D) * zombie.width;
            double oy = world.rand.nextDouble() * zombie.height;
            double oz = (world.rand.nextDouble() - 0.5D) * zombie.width;
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    zombie.posX + ox,
                    zombie.posY + oy,
                    zombie.posZ + oz,
                    0.0D,
                    0.02D,
                    0.0D);
        }
    }
}
