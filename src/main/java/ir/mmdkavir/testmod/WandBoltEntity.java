package ir.mmdkavir.testmod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class WandBoltEntity extends ThrownItemEntity {
    private static final float DAMAGE = 6.0f;
    private static final int MAX_AGE_TICKS = 30;
    public WandBoltEntity(EntityType<? extends WandBoltEntity> entityType, World world) { super(entityType, world); }
    public WandBoltEntity(World world, LivingEntity owner) { super(TestMod.WAND_BOLT_ENTITY, owner, world); }
    @Override protected Item getDefaultItem() { return TestMod.WAND_BOLT_ITEM; }
    @Override public void tick() {
        super.tick();
        if (getWorld().isClient) getWorld().addParticle(ParticleTypes.COMPOSTER, getX(), getY(), getZ(), 0, 0, 0);
        else if (age > MAX_AGE_TICKS) discard();
    }
    @Override protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        Entity target = hit.getEntity();
        target.damage(getDamageSources().thrown(this, getOwner()), DAMAGE);
    }
    @Override protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (!getWorld().isClient) {
            ((ServerWorld)getWorld()).spawnParticles(ParticleTypes.COMPOSTER, getX(), getY(), getZ(), 10, .15, .15, .15, .05);
            discard();
        }
    }
}
