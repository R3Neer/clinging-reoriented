# Player guide

This guide contains the exact controls and less-obvious interactions for
Clinging: Reoriented 0.1.0-alpha.12 development builds.

## Controls

1. Leave your local floor by jumping, falling or being launched.
2. Release the configured jump key.
3. Look toward the cardinal surface that should become your new floor.
4. Press the configured jump key again while airborne.

The nearest cardinal direction to the rendered look wins. No wall or prior jump is
required beyond being genuinely airborne. The same held press cannot produce
repeated turns.

Grounded Space keeps normal jump or mount behavior. Creative flight, spectator
mode, sleeping, gliding, held Gravity Anchors, pending forced retirement and foreign
gravity ownership block voluntary selection. While sprinting downward toward the
current local floor, Space is also reserved when a bounded prediction reaches real
landing support; this lets normal sprint-jump chains queue the next jump without
accidentally triggering gravity.

Normal effective jump power (`0.42`) keeps alpha.11's exact one-tick landing guard.
Stronger jump power expands only that near-landing prediction in proportion to the
effective value, with a hard **three-tick** cap. The calculation uses the player's
`JUMP_STRENGTH` attribute when present plus Vanilla Jump Boost power, so Leaping and
compatible modifiers participate without potion-specific exceptions. Ascending,
non-sprinting or still-distant players remain available to Clinging/Reorientation;
stronger jumps do not create a blanket input lockout.

### Water

Water keeps Vanilla's ordinary Space-to-ascend control. A single press or held
Space is only swimming input; Clinging/Reorientation watches the key passively and
does not consume or rewrite it.

To request a gravity turn while in water, press Space, release it, then press Space
again within **250 ms**. Only that second rising edge requests the turn. The second
press still remains available to Vanilla, so upward swimming continues even when
the gravity request is accepted or rejected. Holding the second press does not
repeat requests.

The double-tap pair is consumed as one gesture. A third rapid press starts a new
pair instead of becoming another immediate turn. Entering the water while Space is
already held cannot synthesize a tap, and leaving the water, losing gameplay focus,
opening a UI, dying/respawning or disconnecting clears any partial pair.

Clinging's charge policy does not change underwater. Water itself never restores a
spent turn. Recharge requires a genuine gravity-relative landing: the feet-side face
of the player's body must be supported by a solid collision surface. Touching a
block with the torso/side does not count. Standing on the seabed does.

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
then settle with a short snap: **0.18 s** for a 90-degree turn and **0.24 s** for an
opposite 180-degree turn. The snap uses quadratic ease-out and is not configurable.

The direction used to **select** gravity and the direction Clinging tries to
**preserve as your navigation heading** are intentionally different concepts. The
actual rendered camera forward chooses NORTH/SOUTH/EAST/WEST/UP/DOWN. At the same
input edge, Clinging also captures the direction represented by your yaw with pitch
treated as zero. Looking straight up or down merely to choose a ceiling/floor does
not erase that heading.

For perpendicular gravity changes, Clinging rotates only around the single axis
required to carry the old gravity vector onto the new one. For opposite directions,
the navigation heading itself is the 180-degree axis. For example, with gravity
DOWN while travelling north, you can glance straight UP, choose gravity UP and then
level the camera again while remaining oriented north rather than being reversed by
an arbitrary canonical frame.

The same yaw-gauge change is applied to view, body and head accumulators so third
person does not manufacture an extra lateral twist. Rapid Reorientation inputs do
not queue camera turns: a new snap starts from the frame actually being displayed at
that instant. Mouse input remains live during the transition.

The same short snap belongs to Clinging-owned mount and pet gravity changes. It is
not merely a player-camera effect. The mob's body/head yaw is transported on the
server and its tracked client animation is enrolled into the same visual ownership
path. Foreign Gravity Changer changes remain foreign and keep Gravity Changer's own
presentation.

## Momentum, falling, collision and recovery

A voluntary turn preserves world momentum and validates the complete rotated
root/passenger hierarchy before changing gravity. Every successful Clinging-owned
gravity-direction change starts a fresh vanilla fall-distance segment. Distance
accumulated while falling toward an earlier gravity direction therefore cannot be
combined with later Reorientation segments into one artificial mega-fall. Failed
and unchanged attempts do not reset fall distance.

The turn first tests the rotated box at the current entity pivot. If that pivot
alone would make the rotated body clip the old floor or wall, Clinging may retry
with a center-aligned pivot that preserves the physical body's world-space center.
A real obstruction still produces a failure sound and leaves position, gravity,
heading, momentum and fall state unchanged.

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
discontinuity invalidates the pending state. Clinging-owned mob retirement and
rider-loan restoration use the same short entity snap when they actually change a
mob's gravity frame.

Death/respawn replaces the Minecraft player entity but not the visual network epoch
for that connection. The first turn after respawn therefore keeps the same Clinging
snap/heading rules instead of falling back to Gravity Changer's long canonical
interpolation.

## Elytra

An equipped, usable Elytra owns Space while airborne. It deploys normally instead
of turning gravity. Space cannot reorient an entity that is already gliding, and
gliding retains the gravity frame that existed at deployment. While the player is
in water, the Elytra eligibility path yields to Vanilla swimming and the dedicated
underwater double-tap rule described above.

## Mounts

Clinging cannot turn a mount. With Reorientation, ordinary grounded Space remains
the mount's normal action; any fresh Space while the root mount is airborne turns
the complete passenger hierarchy only if every destination box is clear. The
rider's captured navigation heading uses the same gravity-transport policy as an
ordinary player turn. A failed candidate is atomic: neither root nor passengers are
moved first and checked later.

Rider and root mount now share **one physical rotation** for a mounted turn. A
perpendicular turn already has a unique axis. For an opposite 180-degree turn, the
rider's navigation heading chooses the common axis; the mount does not independently
invent another one from its own facing. The mount's own heading is instead rebased
through that shared physical plan to calculate its own yaw-gauge delta. Rider and
mount therefore follow the same 90/180-degree motion while each preserves its own
facing semantics and both use the 180/240 ms quadratic snap.

Mounted gravity uses the same generic contract for every compatible non-player
`LivingEntity` root vehicle. Tiny Mounts are not a separate Clinging concept.

Mounting with Reorientation can temporarily lend the rider's non-DOWN frame to the
root mount. Clinging instead adopts the mount's frame. When that loan ends, the
mount returns to the frame it had before the loan. A loan/restoration performed by
Clinging is presentation-owned; a direction written by another Gravity Changer
source is not.

## Pets and passive mobs

Mobs never choose directions autonomously. A tamed animal using vanilla's
follow-owner goal can replay the owner's turns when it reaches the recorded place
where each turn occurred. The pet needs its own Clinging or Reorientation effect,
and Clinging still permits only one arbitrary turn per airborne stretch. A replayed
mob gravity change also starts a new fall-distance segment.

A pet replay derives its physical turn from the pet's own gravity-relative heading,
including the axis for an opposite 180-degree change. Successful owned replays use
the same 180/240 ms quadratic snap as players. Failed collision preflight and
same-direction replay produce no visual transition and do not mutate the pet's yaw.

Sitting pets do not replay routes. Trails retain at most 64 turns for one minute
and are cleared when the owner teleports, changes dimension, dies or logs out.
This is positional replay, not new three-dimensional pathfinding.

## Sounds and feedback

A successful turn and a rejected attempt use different sounds. When nothing
happens, check that the entity is airborne (or that an underwater double tap was
completed), the jump key was released, the desired direction differs from the
current one, the effect still exists, the Clinging airborne charge is not already
spent, retirement is not pending and the complete destination hierarchy has enough
room.
