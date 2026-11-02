#!/usr/bin/env python3
"""
Generates the 8 emote animations for the Player Animation Library (PAL), Minecraft 1.21.11.

Run:  python3 tools/generate_animations.py
Out:  src/main/resources/assets/calisthenics/player_animations/<emote>.json
      (each file holds one animation named after the emote, so its id is "calisthenics:<emote>")

HOW TO TUNE (this is the file to edit when a pose looks off in game):
  1. Change a number below.
  2. Re-run this script.
  3. In game press F3+T (reload resources) - no restart needed.

My own convention for the pose numbers below (degrees, positions in 1/16 block "pixels"):
  - "torso" = the whole body. It maps to PAL's `body` bone, which pivots at the HIPS (0, 12, 0).
    Arms and legs are attached to it and rotate relative to it.
  - torso pitch +90  = body horizontal, chest facing the ground.
  - torso pitch +180 = fully upside down.
  - arm/leg pitch negative = swings FORWARD relative to the body.
  - roll on the right arm positive = out to the side (left arm uses the opposite sign).
  - y positive = up.
PAL files use Blockbench axes, so on output pitch is multiplied by PITCH_SIGN.
If something rotates the wrong way in game:  flip PITCH_SIGN (all pitches) or the sign of one pose value.
"""
import json
import os

PITCH_SIGN = -1   # my pitch convention -> Blockbench axes. Flip to +1 if EVERYTHING pitches the wrong way.
Y_SIGN = 1        # flip to -1 if body height offsets move the wrong way

TICK = 0.05       # seconds per game tick
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                       "assets", "calisthenics", "player_animations")

BONE_OF = {
    "torso": "body",          # whole body, pivot at the hips
    "head": "head",
    "rightArm": "right_arm",
    "leftArm": "left_arm",
    "rightLeg": "right_leg",
    "leftLeg": "left_leg",
}


def secs(tick):
    s = f"{tick * TICK:.3f}".rstrip("0")
    return s + "0" if s.endswith(".") else s


def part_vectors(values):
    """my pose values -> (rotation[x,y,z], position[x,y,z]) in Blockbench axes."""
    rot = [values.get("pitch", 0) * PITCH_SIGN, values.get("yaw", 0), values.get("roll", 0)]
    pos = [values.get("x", 0), values.get("y", 0) * Y_SIGN, values.get("z", 0)]
    return rot, pos


def build(emote_id, frames, loop):
    """frames: list of (tick, pose) with pose = {part: {pitch,yaw,roll,x,y,z}}."""
    parts = sorted({p for _, pose in frames for p in pose})
    bones = {BONE_OF[p]: {"rotation": {}, "position": {}} for p in parts}
    for tick, pose in frames:
        t = secs(tick)
        for p in parts:
            rot, pos = part_vectors(pose.get(p, {}))
            bones[BONE_OF[p]]["rotation"][t] = rot
            bones[BONE_OF[p]]["position"][t] = pos
    # drop position tracks that never move away from 0 (keeps the files small and avoids fighting the base pose)
    for bone in bones.values():
        if all(v == [0, 0, 0] for v in bone["position"].values()):
            del bone["position"]
        if all(v == [0, 0, 0] for v in bone["rotation"].values()):
            del bone["rotation"]
    return {
        "format_version": "1.8.0",
        "animations": {
            emote_id: {
                "loop": loop,
                "animation_length": round(max(t for t, _ in frames) * TICK, 3),
                "bones": bones,
            }
        },
    }


def hold(emote_id, pose, sway=1.5):
    """A held emote: same pose at 0 / 20 / 40 ticks with a tiny whole-body sway so it looks alive."""
    def swayed(roll):
        p = {k: dict(v) for k, v in pose.items()}
        p.setdefault("torso", {})
        p["torso"]["roll"] = p["torso"].get("roll", 0) + roll
        return p
    return build(emote_id, [(0, swayed(-sway)), (20, swayed(sway)), (40, swayed(-sway))], loop=True)


# ---------------------------------------------------------------- held poses
# Geometry used for the heights (all in pixels): the body pivots at the hips (y = 12), shoulders are
# at y = 22 when standing and an arm is 12 long, so:
#   handstand: after a 180 flip the shoulders sit at y = 2, hands would be at -10 -> lift the body by +10.
#   planche:   body horizontal around the hips -> shoulders at y = 12, straight arms reach the ground (y = 0).

HANDSTAND = {
    "torso": {"pitch": 180, "y": 10},
    "rightArm": {"pitch": -180},
    "leftArm": {"pitch": -180},
    "head": {"pitch": -25},
}

PLANCHE = {
    "torso": {"pitch": 90, "y": 0},
    "rightArm": {"pitch": -90},
    "leftArm": {"pitch": -90},
    "head": {"pitch": -45},
}

# Elbow lever: body horizontal and lower than the planche, elbows tucked into the hips.
# (No elbow bend yet - see README "Known gaps".)
ELBOW_LEVER = {
    "torso": {"pitch": 90, "y": -5},
    "rightArm": {"pitch": -90, "roll": 8},
    "leftArm": {"pitch": -90, "roll": -8},
    "head": {"pitch": -45},
}

# Maltese: like the planche but with the arms spread wide to the sides (rings style).
MALTESE = {
    "torso": {"pitch": 90, "y": 1},
    "rightArm": {"roll": 85},
    "leftArm": {"roll": -85},
    "head": {"pitch": -45},
}


# ---------------------------------------------------------------- push-ups (loops)
def pushup():
    top = {
        "torso": {"pitch": 80, "y": 0},
        "rightArm": {"pitch": -80}, "leftArm": {"pitch": -80},
        "head": {"pitch": -40},
    }
    bottom = {
        "torso": {"pitch": 88, "y": -6},
        "rightArm": {"pitch": -88}, "leftArm": {"pitch": -88},
        "head": {"pitch": -40},
    }
    return build("pushup", [(0, top), (20, bottom), (40, top)], loop=True)


def handstand_pushup():
    top = {
        "torso": {"pitch": 180, "y": 10},
        "rightArm": {"pitch": -180}, "leftArm": {"pitch": -180},
        "head": {"pitch": -25},
    }
    bottom = {
        "torso": {"pitch": 180, "y": 3},
        "rightArm": {"pitch": -180}, "leftArm": {"pitch": -180},
        "head": {"pitch": -25},
    }
    return build("handstand_pushup", [(0, top), (25, bottom), (50, top)], loop=True)


# ---------------------------------------------------------------- flips (one-shot)
def flip(emote_id, direction):
    """
    One-shot somersault. direction = +1 front flip, -1 backflip.
    crouch -> take-off -> piked rotation in the air -> landing -> stand.
    The animation ends 0.2 tick after the full 360: the last frame is the same visual pose with
    pitch 0 so the character never spins backwards when the animation hands control back.
    """
    d = direction
    stand = {
        "torso": {"pitch": 0, "y": 0},
        "rightArm": {"pitch": 0}, "leftArm": {"pitch": 0},
        "rightLeg": {"pitch": 0}, "leftLeg": {"pitch": 0},
    }
    crouch = {
        "torso": {"pitch": 6 * d, "y": -4},
        "rightArm": {"pitch": 35}, "leftArm": {"pitch": 35},
        "rightLeg": {"pitch": -25}, "leftLeg": {"pitch": -25},
    }
    takeoff = {
        "torso": {"pitch": 50 * d, "y": 10},
        "rightArm": {"pitch": -165}, "leftArm": {"pitch": -165},
        "rightLeg": {"pitch": 0}, "leftLeg": {"pitch": 0},
    }
    tuck_a = {
        "torso": {"pitch": 150 * d, "y": 24},
        "rightArm": {"pitch": -80}, "leftArm": {"pitch": -80},
        "rightLeg": {"pitch": -95}, "leftLeg": {"pitch": -95},
    }
    tuck_b = {
        "torso": {"pitch": 260 * d, "y": 24},
        "rightArm": {"pitch": -80}, "leftArm": {"pitch": -80},
        "rightLeg": {"pitch": -95}, "leftLeg": {"pitch": -95},
    }
    open_up = {
        "torso": {"pitch": 330 * d, "y": 12},
        "rightArm": {"pitch": -40}, "leftArm": {"pitch": -40},
        "rightLeg": {"pitch": -20}, "leftLeg": {"pitch": -20},
    }
    land_full = {
        "torso": {"pitch": 360 * d, "y": -5},
        "rightArm": {"pitch": -30}, "leftArm": {"pitch": -30},
        "rightLeg": {"pitch": -30}, "leftLeg": {"pitch": -30},
    }
    land_zero = {k: dict(v) for k, v in land_full.items()}
    land_zero["torso"]["pitch"] = 0
    frames = [
        (0, stand), (4, crouch), (8, takeoff), (12, tuck_a), (16, tuck_b),
        (19, open_up), (21, land_full), (21.2, land_zero),
    ]
    return build(emote_id, frames, loop=False)


ANIMATIONS = {
    "handstand": hold("handstand", HANDSTAND),
    "planche": hold("planche", PLANCHE),
    "elbow_lever": hold("elbow_lever", ELBOW_LEVER),
    "maltese": hold("maltese", MALTESE),
    "pushup": pushup(),
    "handstand_pushup": handstand_pushup(),
    "backflip": flip("backflip", -1),
    "flip": flip("flip", +1),
}


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for emote_id, data in ANIMATIONS.items():
        path = os.path.join(OUT_DIR, emote_id + ".json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
        print("wrote", os.path.relpath(path))


if __name__ == "__main__":
    main()
