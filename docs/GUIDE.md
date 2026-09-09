# Player guide

This guide contains the exact controls and less-obvious interactions for
Clinging: Reoriented 0.1.0-alpha.6.

## Controls

1. Leave your local floor by jumping, falling or being launched.
2. Release the configured jump key.
3. Look toward the cardinal surface that should become your new floor.
4. Press the configured jump key again while airborne.

The nearest cardinal direction to the rendered look wins. No wall, double tap or
previous jump is required; being airborne is the criterion. The same held press
cannot produce repeated turns.

Grounded Space keeps normal jump or mount behavior. Creative flight, spectator
mode, sleeping, gliding, held Gravity Anchors and foreign gravity ownership block
voluntary selection.

## Effects and brewing

Clinging comes from Alex's Mobs. It permits one successful turn until the entity
lands on its gravity-relative floor. Failed or unchanged attempts do not consume
the charge.

Add a shulker shell to an ordinary or extended Clinging potion to brew
Reorientation. Reorientation has no airborne charge limit. Redstone extends its
ordinary three-minute form to eight minutes; gunpowder and dragon's breath create
the usual splash and lingering variants.

Clinging is also available as a tier-two beacon power. Reorientation is never a
beacon option.

## Momentum, collision and recovery

A voluntary turn keeps world position and momentum, then validates the complete
rotated root/passenger collision box before changing gravity. Obstruction produces
a failure sound and leaves gravity unchanged.

Gravity persists across jumps and temporary contact loss. When the last owned
Clinging or Reorientation source disappears, the entity returns to DOWN. If the
DOWN box cannot fit during forced retirement, the mod searches a small validated
nearby area rather than embedding the entity. It keeps retrying when no safe
position exists. Voluntary turns never relocate the player.

## Elytra

An equipped, usable Elytra owns Space while airborne. It deploys normally instead
of turning gravity. Space cannot reorient an entity that is already gliding, and
gliding retains the gravity frame that existed at deployment.

## Mounts

Clinging cannot turn a mount. With Reorientation, ordinary grounded Space remains
the mount's normal action; any fresh Space while the root mount is airborne turns
the complete passenger group if every destination box is clear.

Mounting with Reorientation transfers the rider's non-DOWN gravity to the mount.
Clinging instead adopts the mount's frame. On dismount, an unpowered mount returns
to DOWN. A mount with its own gravity effect retains its orientation until its last
effect source disappears.

## Pets and passive mobs

Mobs never choose directions autonomously. A tamed animal using vanilla's
follow-owner goal can replay the owner's turns when it reaches the recorded place
where each turn occurred. The pet needs its own Clinging or Reorientation effect,
and Clinging still permits only one turn per airborne stretch.

Sitting pets do not replay routes. Trails retain at most 64 turns for one minute
and are cleared when the owner teleports, changes dimension, dies or logs out.
This is positional replay, not new three-dimensional pathfinding.

## Sounds and feedback

A successful turn and a rejected attempt use different sounds. When nothing
happens, check that the entity is airborne, the jump key was released, the desired
direction differs from the current one, the effect still exists and the destination
has enough room.
