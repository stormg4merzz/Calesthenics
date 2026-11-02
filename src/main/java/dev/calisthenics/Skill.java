package dev.calisthenics;

/**
 * Every emote the mod knows, in the order they appear in the UI (slots 1..8, left to right, top to bottom,
 * skipping the center cell which shows the player's head).
 *
 * The id doubles as the animation file name: assets/calisthenics/player_animation/<id>.json
 * oneShot = plays once and ends by itself (flips); otherwise it is held until the player stops it.
 */
public enum Skill {
    HANDSTAND("handstand", false),
    PLANCHE("planche", false),
    ELBOW_LEVER("elbow_lever", false),
    MALTESE("maltese", false),
    PUSHUP("pushup", false),
    HANDSTAND_PUSHUP("handstand_pushup", false),
    BACKFLIP("backflip", true),
    FLIP("flip", true);

    public final String id;
    public final boolean oneShot;

    Skill(String id, boolean oneShot) {
        this.id = id;
        this.oneShot = oneShot;
    }

    public String translationKey() {
        return "skill.calisthenics." + id;
    }

    /** Returns null for unknown ids (also for the empty "stop" id). */
    public static Skill byId(String id) {
        for (Skill s : values()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }
}
