# AI Improvements

## AI omniscience (vision)

AI systems (`AiControllerSystem`, `PathingSystem`, attack targeting) don't consult `VisionContext`. Enemies always know where the player is and can pathfind through unexplored areas.

`VisionContext` is currently a *player* concept. If we want per-entity vision (stealth, alert/patrol behaviour, enemies losing sight of the player when the player breaks LOS), each AI agent needs its own FOV/awareness state — same FOV machinery (`FOV.reuseFOV` + the existing `OpaqueGrid` seam), but per-viewer.

Not blocking; flagged for future AI work.
