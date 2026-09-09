# Configuration

Clinging: Reoriented currently exposes one client-side presentation setting. The
first client launch creates:

`config/clinging-reoriented-client.json`

```json
{
  "cameraRotationSeconds": 1.0
}
```

`cameraRotationSeconds` accepts decimal values from `0.05` to `10`. Lower values
rotate faster. Restart Minecraft after editing the file.

The setting applies to Gravity Changer's ordinary visual transitions initiated by
Clinging, Reorientation and passenger orientation changes. Physical gravity changes
immediately, world momentum is preserved and the camera/body transforms remain
synchronized. Gravity Core's separate special animation remains owned by Gravity
Changer.

Invalid content falls back to one second and produces a log warning. The file is
left untouched so that it can be corrected manually.
