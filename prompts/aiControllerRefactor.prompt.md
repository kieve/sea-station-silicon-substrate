## Task

Refactor the current AI system that controls NPCs:
- AiAttack
- AiChaser
- AiSeesawController

The ultimate goal will work as follows

## New AI System

A new Component and System is created
- AiController (The Component)
- AiControllerSystem (The System)

The component will hold a list of states, and their conditions
Current AI systems will be repurposed into these states (and thus, behaviors)
There will be (for now, eventually more) 3 states:
- AiAttack -> AttackState
- AiChaser -> ChaseState
- AiSeesawController -> WanderState
- IdleState

The behaviors of these states will be the same as the current system. IdleState simply does nothing.
I will describe how states are determined

### Deciding the State

In addition to the "State" base interface and above defined states, there will be `Conditions`,
base class `Condition`
The types of conditions are as follows:
- Distance From Entity
- Has Weapon (by weaponId, such as `chip_claws`)

The conditions will all have a priority value associated with them.

#### Distance From Entity condition

This condition will have two properties
- Distance in tiles
- Target Entity

Since these will be defined in the YAML, the target entity will not necessarily be referenceable.
So it will be nullable, but there will be a special value that can be used in the YAML to specify
the Player as the target.

#### Has Weapon condition

This condition will have one property, the ID of the weapon that the entity this behavior is
attached to needs to have in the equipment.

#### Choosing a Condition

The AiController component will have these behaviors, conditions + States.
It will choose the current state based on the first condition that applies when checking in priority
order. If none, it uses the IdleState.
If no condition is defined for a behavior, it's effectively always met and chosen based on priority.

#### Defining a Behavior

The behaviors are defined in YAML, loaded in a similar way as the current entity system does.
We will be able to define a behavior, including all the conditions, their priorities, and the states
they map to.

### Example YAML Behavior Definition

```behaviors.yaml
---
properties:
  - id: player_hunter
  - states:
    - state: ChaseState
      priority: 1
      conditions:
        - type: DistanceToEntity
          target: PLAYER
          distance: 30
    - state: AttackState
      priority: 2
      conditions:
        - type: DistanceToEntity
          target: PLAYER
          distance: 1
        - type: HasWeapon
          weaponId: chip_claws
    - state: WanderState
      priority: 3

---
properties:
  - id: wanderer
  - states:
    - state: WanderState
      priority: 1

---
properties:
  - id: idler
    - state: IdleState
      priority: 1
```

### Example YAML Behavior Usage

```entities.yaml
---
components:
  - type: Identifier
    key: debugMover
  - type: TileGlyph
    glyphId: S
  - type: Descriptor
    name: Moving Sign
    description: "Perhaps, there's many of them?"
  - type: RenderingHint
    zIndex: 1
  - type: Velocity
  - type: Collider
  - type: Behavior
    id: wanderer

---
components:
  - type: Identifier
    key: debugMover
  - type: TileGlyph
    glyphId: S
  - type: Descriptor
    name: Moving Sign
    description: "Perhaps, there's many of them?"
  - type: RenderingHint
    zIndex: 1
  - type: Velocity
  - type: Collider
  - type: Behavior
    id: idler

---
components:
  - type: Identifier
    key: aiAttacker
  - type: TileGlyph
    glyphId: A
  - type: Descriptor
    name: Attacker
    description: "A hostile attacker"
  - type: RenderingHint
    zIndex: 1
  - type: Health
    maxHp: 20
  - type: Speed
    val: 100
  - type: Equipment
    weaponId: chip_claws
  - type: Attackable
  - type: Collider
  - type: Solid
  - type: Examinable
  - type: Behavior
    id: player_hunter
```

## Final Notes

In general, the logic in the current ai system should be moved into the behavior state. The new
System should evaluate the conditions and priorities, before evoking the state to execute the logic.
