# Light Improvements

## FOV radius / lighting

`VisionContext.recalculate()` runs FOV at radius `max(MAP_WIDTH, MAP_HEIGHT)` — effectively unlimited within the bounding box. Fine while maps are small and fully-lit.

When maps become dynamic / arbitrarily large, this stops being acceptable:
- We'll need a bounded vision radius per viewer.
- Some tiles should be dim or invisible without a light source (darkness in unlit rooms, torches, the player's own glow, etc.).
- squidgrid's `LightingManager` / `Radiance` (see `docs/squidgrid-lighting.md`) is the natural building block.

The `OpaqueGrid` interface in `util` is the seam — a future light system reuses the same opacity check that FOV does, and adds light propagation on top.
