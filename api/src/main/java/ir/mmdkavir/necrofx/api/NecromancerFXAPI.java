package ir.mmdkavir.necrofx.api;
import java.util.Optional;
import java.util.UUID;
public interface NecromancerFXAPI { boolean isClientModAvailable(UUID playerId); Optional<String> getCustomMobId(UUID entityId); boolean registerMobDefinition(MobDefinition definition); boolean playAnimation(UUID entityId,String animationId); boolean playSummonEffect(UUID playerId,MobDefinition definition); void syncMob(UUID entityId); void unregisterMob(UUID entityId); int protocolVersion(); }
