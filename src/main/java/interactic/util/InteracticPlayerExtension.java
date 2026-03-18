package interactic.util;

public interface InteracticPlayerExtension {
    void setDropPower(float power);
    float getDropPower();
    void setDropDirection(float pitch, float yaw);
    float getDropPitch();
    float getDropYaw();
}
