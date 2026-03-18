package interactic.util;

public interface ItemEntityRenderStateAccessor {

    float interactic$getCustomRotation();

    void interactic$setCustomRotation(float value);

    boolean interactic$isLayFlat();

    void interactic$setLayFlat(boolean value);

    float interactic$getYaw();

    void interactic$setYaw(float value);

    boolean interactic$isOnGround();

    void interactic$setOnGround(boolean value);

    float interactic$getTickDelta();

    void interactic$setTickDelta(float value);

    boolean interactic$isBlockItem();

    void interactic$setBlockItem(boolean value);
}
