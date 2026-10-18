package ir.mmdkavir.testmod;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TestWandItem extends Item {

    public TestWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            Vec3d look = user.getRotationVec(1.0f);
            user.addVelocity(look.x * 1.2, 0.6, look.z * 1.2);
            user.velocityModified = true;
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100));
            serverWorld.spawnParticles(ParticleTypes.END_ROD, user.getX(), user.getY() + 1.0, user.getZ(), 40, 0.5, 0.5, 0.5, 0.05);
            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.0f, 1.0f);
            user.sendMessage(Text.literal("Test Wand works!"), true);
        }
        user.getItemCooldownManager().set(this, 20);
        return TypedActionResult.success(stack, world.isClient());
    }
}
