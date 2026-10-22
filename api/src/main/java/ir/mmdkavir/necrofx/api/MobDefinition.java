package ir.mmdkavir.necrofx.api;
import java.util.Map;
import java.util.Objects;
public record MobDefinition(String id,String displayName,String fallbackEntity,String model,String texture,float scale,Map<String,String> animations,int summonDurationTicks) {
 public MobDefinition { Objects.requireNonNull(id); Objects.requireNonNull(fallbackEntity); if(!id.matches("[a-z0-9_]{1,64}")) throw new IllegalArgumentException("Invalid id"); if(scale<=0.1f||scale>10f) throw new IllegalArgumentException("Invalid scale"); if(summonDurationTicks<0||summonDurationTicks>1200) throw new IllegalArgumentException("Invalid duration"); animations=animations==null?Map.of():Map.copyOf(animations); displayName=displayName==null?id:displayName; model=model==null?id:model; texture=texture==null?id:texture; }
 public String animation(String key,String fallback){return animations.getOrDefault(key,fallback);}
}
