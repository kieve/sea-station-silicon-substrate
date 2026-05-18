# Unify Connectors and Submaps with the Entity Model

## Goal

Drop the bespoke `ConnectorDefinition` / `SubmapDefinition` records and the
parallel editor UI for them. Connectors and submaps **are** entities —
they share the same on-disk shape (`id` + `components`), the same Java
record (`MapEntityDefinition`), and the same editor infrastructure
(`EditorEntity`, `ComponentPanel`, move tool, blueprint lookup). They
keep a dedicated **`connectors:`** / **`submaps:`** YAML section and a
dedicated **Submaps tab** in the editor, because the game treats them
specially at load time and the user wants them visually separated.

The win: every editor capability that already works for entities (select,
inline property editing, move tool, color/glyph resolution, override
indicators) works for connectors and submaps automatically — by deletion,
not by addition.

---

## YAML shape

Each map YAML keeps three top-level "entity-shaped" sections.
`MapEntityDefinition` (id + components) is reused verbatim for all three.

```yaml
floorGlyph: interpunct

blocks:
  ...
layers:
  ...

# Named anchors used at load time to align submaps.
# id = the connector's instance name (NOT a blueprint reference).
connectors:
  - id: east_door
    components:
      - type: Connector
        direction: east     # optional; required for localConnector use
      - type: Position
        x: 11
        y: 5
        z: 1

# Composition entries. id = the region's instance name.
submaps:
  # Offset mode — Position present, no localConnector/remoteConnector.
  - id: damaged_sub_root
    components:
      - type: Submap
        ref: ./damaged_sub.yaml
      - type: Position
        x: 0
        y: 0
        z: 0

  # Connector mode — no Position; localConnector/remoteConnector on Submap.
  - id: maintenance_east
    components:
      - type: Submap
        ref: ./maintenance_sub.yaml
        localConnector: east_door
        remoteConnector: west_door
        allowOverlap: false

# Ordinary ECS entities. id = a registered blueprint id.
entities:
  - id: player
    ...
```

### `id` semantics

- In `entities:` — `id` is a **blueprint reference**; must resolve via the
  entity registry (today's behavior, unchanged).
- In `connectors:` and `submaps:` — `id` is the **instance name** of the
  connector or region. Free-form. Never resolved against the registry.
  This is what other YAML refers to (`localConnector: east_door`,
  `remoteConnector: west_door`) and what shows in the editor.

This split is the only place `MapEntityDefinition` semantics differ
between sections, and it falls out naturally from where the loader reads
each list.

---

## New component classes

Two new POJO components, registered like every other component (just
classes under `ca.kieve.ssss.component` — picked up by
`ComponentTypeDeserializer` via classpath scan).

### Engine-only marker

Add a marker subinterface so we can tell at compile time and at runtime
which components are load-time / editor-only and must never land on a
real ECS entity:

```java
public interface EngineComponent extends Component {
}
```

`Connector` and `Submap` implement `EngineComponent`. Two enforcement
points:

- **Runtime guard.** In the entity factory's component-application path
  (`EntityFactory.createEntityWithOverrides`, or whichever method writes
  components onto the Dominion entity), reject any
  `EngineComponent`-typed override with `IllegalStateException`:
  *"`<Type>` is an engine component and cannot be attached to a runtime
  entity; it belongs in the connectors:/submaps: section."* Since
  `entities:` never has these components today, this is a guardrail for
  future authoring mistakes, not a behavior change.
- **Editor filter.** Anywhere the editor presents "components you could
  add" — the `Add Override` dialog (`ComponentAddDialog`,
  `ComponentIntrospector`-driven dropdown) — exclude classes assignable
  to `EngineComponent`. Same filter probably applies to the
  `ComponentTypeDeserializer`'s `getAllTypeNames()` consumer if it's
  used in editor pickers; one helper method on `ComponentIntrospector`
  (`isEngineComponent(Class<?>)`) keeps the test cheap.

The subinterface beats an `isEngine()` default method because the check
is `EngineComponent.class.isAssignableFrom(componentClass)` — no need to
instantiate the component just to ask. Static, zero-cost, surfaces in
IDE search.

### `Connector implements EngineComponent`
```java
public class Connector implements EngineComponent {
    public ConnectorDirection direction;  // nullable
}
```

### `Submap implements EngineComponent`
```java
public class Submap implements EngineComponent {
    public String ref;               // required
    public String localConnector;    // nullable
    public String remoteConnector;   // nullable
    public boolean allowOverlap;
}
```

Neither overrides `cleanup`. No system reads them. Their only purpose
is to carry data through YAML → `MapEntityDefinition` → loader.

`ConnectorDirection` enum stays where it is.

**Deleted:** `ConnectorDefinition`, `SubmapDefinition`.

### Mode is implicit (no `mode:` field)

For submaps, the placement mode is derived from which components are
present, not a `mode` field:

| Components on the submap entity                            | Mode      |
| ---------------------------------------------------------- | --------- |
| `Position` only                                            | offset    |
| `Submap.localConnector` + `Submap.remoteConnector` only    | connector |
| Both, or neither                                           | invalid   |

This is the same rule today (`SubmapDefinition` canonical constructor
enforces exactly-one) just relocated.

---

## `MapDefinition` shape

```java
public record MapDefinition(
    Map<String, MapBlockDefinition> blocks,
    Map<String, String> layers,
    String floorGlyph,
    List<MapEntityDefinition> entities,
    List<MapEntityDefinition> connectors,   // was List<ConnectorDefinition>
    List<MapEntityDefinition> submaps       // was List<SubmapDefinition>
)
```

Three lists of the same record. No bespoke types. Jackson handles
serialization through the existing `MapEntityDefinition` /
`ComponentDefinition` machinery — no custom serializer / deserializer
code path needed.

---

## Loading flow

### `CompositeMapLoader`

Today it reads `def.submaps()` (typed `SubmapDefinition`) and
`def.connectors()` (typed `ConnectorDefinition`). After the change it
reads `MapEntityDefinition` and pulls fields out of components:

```java
for (MapEntityDefinition submap : def.submaps()) {
    Submap sm = findComponent(submap, Submap.class);   // required
    Position pos = findComponent(submap, Position.class); // optional
    boolean offsetMode = pos != null;
    boolean connectorMode = sm.localConnector != null && sm.remoteConnector != null;
    // exactly-one validation (same as today)
    ...
    Vec3i childOffset = offsetMode
        ? worldOffset.add(pos.getPosition())
        : worldOffset.add(resolveConnectorOffset(def, sm, childDef));
    String childId = submap.id();  // already the instance name
    ...
}

ConnectorDefinition findConnector(MapDefinition def, String name) {
    for (MapEntityDefinition c : def.connectors()) {
        if (!c.id().equals(name)) continue;
        Connector conn = findComponent(c, Connector.class);
        Position pos = findComponent(c, Position.class);
        // pos is required; conn.direction may be null
        return new ConnectorView(name, pos.getPosition(), conn.direction);
    }
    return null;
}
```

A small private `ConnectorView` record (or similar) inside
`CompositeMapLoader` carries the resolved (position, direction) pair so
the rest of the loader doesn't have to dig through components repeatedly.

### `GameEngine.createEntities`

Unchanged. It only iterates `mapGenerator.getEntities()` (returning
`entities:` translated to world coords). Connectors and submaps never
hit the entity factory because they live in different lists.

This is the special handling the user wants: connectors and submaps are
entity-shaped data, but the game's ECS spawn loop never sees them. No
filter, no skip flag, no special blueprint — just two lists the game
ignores.

### `YamlMapGenerator`

Already translates `MapEntityDefinition.Position` x/y/z by world offset
for submap children. That code now also runs over the `connectors:` and
`submaps:` lists of nested submaps so their positions land in the right
world space (this part is new — today `ConnectorDefinition.position` is
local-space-only; under the new model a child's connector that the
parent doesn't reference still gets translated for editor preview).

Open question: do we want the runtime to expose composed connectors to
*anything* outside CompositeMapLoader? My read: no — they're load-time
only. So `YamlMapGenerator.getEntities()` still returns only translated
`entities:`, never connectors/submaps.

---

## Editor

### Data model: `EditorMapModel`

Today: `m_entities`, `m_connectors`, `m_submaps` (the last two typed
`List<ConnectorDefinition>` / `List<SubmapDefinition>`).

After: `m_entities`, `m_connectors`, `m_submaps` — all
`List<EditorEntity>`. `EditorEntity` already knows how to translate
to/from `MapEntityDefinition`, so all three round-trip through the
existing path.

Deleted: `EditorMapModel.getConnectors/addConnector/replaceConnector/
removeConnector` returning `ConnectorDefinition` — replaced by
`getConnectorEntities` etc. returning `EditorEntity`. Same for submaps.

### Submap tab

Two `ListView<Integer>` sections, same shape as today, but each row is
an `EditorEntity`. Display label:

- Connectors: `"east_door @ (11, 5, 1) east"` — derived by pulling the
  `Position` and `Connector.direction` out of the entity's components.
- Submaps: `"maintenance_east (offset (0, 0, 0))"` or
  `"maintenance_east (east_door <-> west_door)"` — derived from
  components.

Buttons collapse from `[Add | Edit | Remove | Open]` to `[Add | Remove |
Open]`. Edit is gone — selecting an entry routes to the
**properties panel** (the existing `ComponentPanel` on the right side)
for inline editing, exactly like entities do today.

Add still uses a small dialog (`AddConnectorDialog`,
`AddSubmapDialog`) — minimal: just enough fields to construct a valid
initial entity (id + ref for submaps; id for connectors). Further
edits happen in the properties panel.

### Properties panel: `ComponentPanel`

Today `showMapEntity(baseEntityId, overrides)` requires `baseEntityId`
to resolve via the entity registry. For connectors and submaps the id
is an instance name with no blueprint.

**Approach (decided):** Add
`ComponentPanel.showInlineEntity(displayLabel, components)` — same
TreeTable layout, no base merging, no override indicators. The
"no blueprint" case is honest about itself; override-status styling
simply doesn't apply. ~30 lines.

This reuses the panel's editing machinery (edit-on-click, type-aware
filters, commit/cancel) without faking a blueprint. "Just treat them
as entities" means "same data shape, same editor flow" — not
"fabricate a blueprint for them."

The `Add Override` flow inside the panel uses
`ComponentIntrospector` to find addable components. After this change
it filters out `EngineComponent` types **universally** — for entities,
connectors, and submaps alike. `Connector` and `Submap` components get
onto their respective entities at creation time (via the Add Connector
/ Add Submap dialogs), never through the picker.

### Move tool

Today the tool moves the currently-selected `EditorEntity` by setting
its `Position` component. After the change, connector and submap
entities also have `Position` components (when present), so the existing
tool moves them for free. No tool changes.

Selection state: `m_selectedEntityIndex` becomes
`m_selectedEntity` (or a pair `(SectionKind, index)` where Kind ∈
{ENTITY, CONNECTOR, SUBMAP}) so we know which list to dereference. The
moveEntityTo handler reads from the right list.

When a connector or submap is selected: ComponentPanel updates,
the on-canvas selection rectangle shows on the entity's Position,
and the move tool starts working.

### Selection overlay

Today's overlay groups items as `[B] block`, `[E] entity_id`, `[C]
connector_id`. After: `[B]`, `[E]`, `[C]`, `[S]` — connectors and
submaps both have separate prefixes, and clicking either routes to the
Submaps tab and selects the row.

The existing `ConnectorInfo` becomes a generic
`SectionItemInfo(kind, index, label)` so block/entity/connector/submap
can all flow through one type.

### Entities tab

Unchanged. Connectors and submaps don't appear here — they're in
`m_connectors` / `m_submaps`, which the EntitiesTab doesn't read.

### Renderer

`buildConnectorMarkers` and `buildSubmapOverlay` already iterate the
lists. After the change they pull `Position` and direction out of
components instead of off `ConnectorDefinition` / `SubmapDefinition`
fields. ~10 lines of accessor changes.

**Per Resolved Decision #2:** when drawing a submap (in single-map
"ghost" mode or in composed mode), also draw that submap's own
connectors, translated into world coords by the submap's offset, with
the same dimmed alpha as the submap's cells. A
`buildSubmapConnectorMarkers(z)` method on `MapViewPanel` walks
`ComposedWorld.regions()` (excluding root), pulls the connector entities
from each region's source `MapDefinition`, applies the region's offset,
and yields markers. The renderer takes them as a separate list so it can
choose the alpha.

### `ComposedWorld`

Same story — `resolveOffset` reads `Submap.localConnector` /
`Submap.remoteConnector` from a component instead of a record field.
`findConnector` walks the connector entity list and matches on entity
id. The math (`localPos + direction.unitVector() - remotePos`) is
unchanged.

---

## Migration

1. Add `Connector` and `Submap` component classes.
2. Change `MapDefinition` connectors/submaps list types.
3. Update `CompositeMapLoader`, `ComposedWorld`, renderer, editor model,
   editor panels.
4. Rewrite `core/src/main/resources/content/maps/home_base/*.yaml` and
   `core/src/test/resources/content/maps/test/composite/*.yaml` to the
   new shape.
5. Delete `ConnectorDefinition`, `SubmapDefinition`,
   `ConnectorEditDialog`, `SubmapEditDialog`.
6. `./gradlew check` passes; run the editor and the game on
   `sub_complex.yaml`.

No backward-compat shim — the legacy YAML shape is removed in the same
PR that introduces the new one. We've only got three production map
files and a handful of test fixtures.

---

## Resolved decisions

1. **`ComponentPanel`** — option B: `showInlineEntity(label, components)`,
   no blueprint required.

2. **Selection rectangle on connector-mode submaps** — not rendered.
   Instead, when the renderer draws a submap's cells, it also draws
   *that submap's* connectors translated into world coords (using the
   same ghost-alpha treatment as the cells). When two connectors line
   up, that's the visual confirmation the offset is correct — no
   reticle needed.

   Concretely: `MapRenderer.setConnectorMarkers` keeps taking the
   parent map's connectors at full opacity. A new
   `setSubmapConnectorMarkers` (or a per-marker alpha field) takes the
   composed-world connectors from submaps and draws them dimmed. Both
   are filtered by current Z.

3. **Add Connector / Add Submap dialogs** — kept, minimal, under
   `editor/component/`. Add Connector asks for `id`; Add Submap asks
   for `id` + `ref`. Both default the Position to `(0, 0, 0)` and let
   the user adjust via the move tool or the properties panel. Mode
   (offset vs connector) starts as offset (Position component present)
   — switching to connector mode means deleting the Position via the
   panel's per-row delete and adding `localConnector` /
   `remoteConnector` on the `Submap` component.

4. **Component order in saved YAML** — not enforced. Goal is the
   editor as the primary interface; YAML readability is good-enough.
