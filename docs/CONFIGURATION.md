# Configuration

Clinging: Reoriented currently exposes one client-side presentation setting. The
first client launch creates:

`config/clinging-reoriented-client.json`

```json
{
  "cameraRotationSeconds": 0.25
}
```

`cameraRotationSeconds` accepts decimal values from `0.05` to `10`. Lower values
rotate faster. Restart Minecraft after editing the file.

The setting applies only to ordinary Gravity Changer visual transitions that
Clinging: Reoriented itself initiates: Clinging turns, Reorientation turns,
Reorientation-driven passenger/root orientation changes and their own animated
return to DOWN. Physical gravity changes immediately, world momentum is preserved
and the camera/body transforms remain synchronized.

An unrelated Gravity Changer change, Gravity Anchor/Core transition, command or
other mod does **not** inherit this configured duration. Gravity Changer keeps its
own timing and remains the animation/physics authority for those changes.

Invalid content, including a missing `cameraRotationSeconds` key, falls back to
0.25 seconds and produces a log warning. The invalid file is left untouched so
that it can be corrected manually.
