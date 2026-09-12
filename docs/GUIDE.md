# Player guide

This guide contains the exact controls and less-obvious interactions for
Clinging: Reoriented 0.1.0-alpha.10 development builds.

## Controls

1. Leave your local floor by jumping, falling or being launched.
2. Release the configured jump key.
3. Look toward the cardinal surface that should become your new floor.
4. Press the configured jump key again while airborne.

The nearest cardinal direction to the rendered look wins. No wall, double tap or
previous jump is required; being genuinely airborne is the criterion. The same held
press cannot produce repeated turns.

Grounded Space keeps normal jump or mount behavior. Creative flight, spectator
mode, sleeping, gliding, held Gravity Anchors, pending forced retirement and foreign
gravity ownership block voluntary selection. While sprinting downward toward the
current local floor, Space is also reserved when the next simulation step predicts
landing contact; this lets normal sprint-jump chains queue the next jump without
accidentally triggering gravity.

## Effects and brewing

Clinging comes from Alex's Mobs. It permits **one** successful voluntary turn until
the entity lands on its gravity-relative floor. Once that charge is spent, every
further voluntary direction, including DOWN, is rejected until a real landing.
Failed or unchanged attempts do not consume the charge.

Add a shulker shell to an ordinary or extended Clinging potion to brew
Reorientation. Reorientation has no airborne charge limit. Redstone extends its
ordinary three-minute form to eight minutes; gunpowder and dragon's breath create
the usual splash and lingering variants.

Clinging is also available as a tier-two beacon power. Reorientation is never a
beacon option.

## Gravity snap and heading

A successful turn changes physical gravity immediately. Camera and body presentation
then settle with a short snap: **0.12 s** for a 90-degree turn and **0.18 s** for an
opposite 180-degree turn. The snap uses a fast ease-out and is not configurable.

For perpendicular gravity changes, Clinging rotates only around the single axis
required to carry the old gravity vector onto the new one. It does not interpolate
between Gravity Changer's arbitrary canonical twists. For opposite directions, the
player's current heading projected onto the old horizontal plane becomes the
180-degree axis; looking straight along gravity falls back to the current right axis.
The same transport is applied to the logical view so the player finishes looking in
the intuitive transported direction rather than inheriting an arbitrary yaw flip.

Rapid Reorientation inputs do not queue camera turns. A new snap starts from the
frame actually being displayed at that instant and settles toward the latest target.
Mouse input remains live during the transition.

## Momentum, collision and recovery

A voluntary turn preserves world momentum and validates the complete rotated
root/passenger hierarchy before changing gravity. It first tests the rotated box at
the current entity pivot. If that pivot alone would make the rotated body clip the
old floor or wall, Clinging may retry with a center-aligned pivot that preserves the
physical body's world-space center. A real obstruction still produces a failure
sound and leaves position, gravity, heading and momentum unchanged.

Gravity persists across jumps and temporary contact loss. When the last gravity
source **owned by Clinging: Reoriented** disappears, the mod first tries to return
to DOWN at the current position. If that does not fit, it searches only validated
loaded positions whose true displacement is at most **four blocks** from the
retirement origin. It never teleports to an old checkpoint and never scans an
unbounded vertical column.

Forced retirement is cleanup, not a voluntary use of Space, so a spent Clinging
charge does not prevent it. If no safe local DOWN placement exists, the current
frame remains temporarily and retirement becomes pending. The mod retries
periodically; ordinary movement can help the entity leave the obstruction, but new
voluntary gravity turns are blocked until retirement succeeds or a lifecycle
discontinuity invalidates the pending state.

## Elytra

An equipped, usable Elytra owns Space while airborne. It deploys normally instead
of turning gravity. Space cannot reorient an entity that is already gliding, and
gliding retains the gravity frame that existed at deployment.

## Mounts

Clinging cannot turn a mount. With Reorientation, ordinary grounded Space remains
the mount's normal action; any fresh Space while the root mount is airborne turns
the complete passenger hierarchy only if every destination box is clear. The
rider's heading uses the same gravity-transport policy as an ordinary player turn.
A failed candidate is atomic: neither root nor passengers are moved first and
checked later.

Mounted gravity uses the same generic contract for every compatible non-player
`LivingEntity` root vehicle. Tiny Mounts are not a separate Clinging concept.

Mounting with Reorientation can temporarily lend the rider's non-DOWN frame to the
root mount. Clinging instead adopts the mount's frame. When that loan ends, the
mount returns to the frame it had before the loan.

## Pets and passive mobs

Mobs never choose directions autonomously. A tamed animal using vanilla's
follow-owner goal can replay the owner's turns when it reaches the recorded place
where each turn occurred. The pet needs its own Clinging or Reorientation effect,
and Clinging still permits only one arbitrary turn per airborne stretch.

Sitting pets do not replay routes. Trails retain at most 64 turns for one minute
and are cleared when the owner teleports, changes dimension, dies or logs out.
This is positional replay, not new three-dimensional pathfinding.

## Sounds and feedback

A successful turn and a rejected attempt use different sounds. When nothing
happens, check that the entity is airborne, the jump key was released, the desired
direction differs from the current one, the effect still exists, the Clinging
airborne charge is not already spent, retirement is not pending and the complete
destination hierarchy has enough room.
