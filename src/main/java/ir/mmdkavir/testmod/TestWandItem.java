package ir.mmdkavir.testmod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TestWandItem extends Item {
    private static final double TELEPORT_DISTANCE = 8.0;
    private static final int BOLT_COOLDOWN_TICKS = 8;
    private static final int TELEPORT_COOLDOWN_TICKS = 20;
    public TestWandItem(Settings settings) { super(settings); }
    public static boolean isHoldingWand(PlayerEntity player) {
        return player.getMainHandStack().isOf(TestMod.TEST_WAND) || player.getOffHandStack().isOf(TestMod.TEST_WAND);
    }
    public static void notEnoughMana(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("Not enough mana").formatted(Formatting.RED), true);
    }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack=user.getStackInHand(hand);
        if(world.isClient || !(user instanceof ServerPlayerEntity player)) return TypedActionResult.success(stack,world.isClient());
        if(player.isSneaking()) {
            if(teleport(player)) player.getItemCooldownManager().set(this,TELEPORT_COOLDOWN_TICKS);
        } else if(shootBolt(player)) player.getItemCooldownManager().set(this,BOLT_COOLDOWN_TICKS);
        return TypedActionResult.success(stack,false);
    }
    private static boolean shootBolt(ServerPlayerEntity player) {
        if(!ManaManager.trySpend(player,ManaManager.COST_BOLT)){notEnoughMana(player);return false;}
        ServerWorld world=(ServerWorld)player.getWorld();
        WandBoltEntity bolt=new WandBoltEntity(world,player);
        bolt.setNoGravity(true); bolt.setVelocity(player,player.getPitch(),player.getYaw(),0.0f,2.0f,0.0f);
        world.spawnEntity(bolt);
        world.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.ENTITY_SNOWBALL_THROW,SoundCategory.PLAYERS,0.8f,1.3f);
        return true;
    }
    private static boolean teleport(ServerPlayerEntity player) {
        if(ManaManager.get(player)<ManaManager.COST_TELEPORT){notEnoughMana(player);return false;}
        ServerWorld world=(ServerWorld)player.getWorld(); Vec3d direction=player.getRotationVec(1.0f).normalize(); double best=0.0;
        for(double distance=0.5;distance<=TELEPORT_DISTANCE;distance+=0.5) {
            Box moved=player.getBoundingBox().offset(direction.multiply(distance));
            if(!world.isSpaceEmpty(player,moved)) break;
            best=distance;
        }
        if(best<1.0){player.sendMessage(Text.literal("No space to teleport").formatted(Formatting.RED),true);return false;}
        ManaManager.trySpend(player,ManaManager.COST_TELEPORT);
        Vec3d from=player.getPos(),to=from.add(direction.multiply(best));
        world.spawnParticles(ParticleTypes.PORTAL,from.x,from.y+1.0,from.z,30,.3,.5,.3,.2);
        player.networkHandler.requestTeleport(to.x,to.y,to.z,player.getYaw(),player.getPitch()); player.fallDistance=0.0f;
        world.spawnParticles(ParticleTypes.PORTAL,to.x,to.y+1.0,to.z,30,.3,.5,.3,.2);
        world.playSound(null,to.x,to.y,to.z,SoundEvents.ENTITY_ENDERMAN_TELEPORT,SoundCategory.PLAYERS,.8f,1.2f);
        return true;
    }
    public static void tryHeal(ServerPlayerEntity player) {
        if(!player.isAlive()||!isHoldingWand(player)) return;
        if(player.getHealth()>=player.getMaxHealth()){player.sendMessage(Text.literal("Health is already full").formatted(Formatting.GRAY),true);return;}
        if(!ManaManager.trySpend(player,ManaManager.COST_HEAL)){notEnoughMana(player);return;}
        player.setHealth(player.getMaxHealth()); ServerWorld world=(ServerWorld)player.getWorld();
        world.spawnParticles(ParticleTypes.HEART,player.getX(),player.getY()+1.0,player.getZ(),12,.4,.5,.4,.05);
        world.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.ENTITY_PLAYER_LEVELUP,SoundCategory.PLAYERS,.6f,1.4f);
        player.sendMessage(Text.literal("Healed!").formatted(Formatting.GREEN),true);
    }
}
