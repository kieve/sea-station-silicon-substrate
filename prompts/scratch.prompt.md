# From Code Review
- AiController, don't store state specific data here. It should be stored in the state instance. 
  - Afterwards, can it be a record class?
- Rather than the behavior being a controller, we should specify the controller component,
  explicitly
- blocks.yaml use a parent to define others. Let's do something similar with the entities.yaml
- Maybe rename "StateContext"

# From Testing
- Attack state still hits you when it's dead.

# Eventually
- ChaseState defined and fills out a dijkstra map, but this should be cached and reused in the game
  context






-------
Stack

AiControllerSystem is constructing conditions from the behaviorFactory. This is fairly wasteful,
as they're defined at compile time and don't currently need to change at runtime.
Let's create a AiControllerContext (similar to other classes in the Context package) with a
reference stored in the GameContext where we cache all of the behavior data that is otherwise being
constructed on the spot. At least for the conditions. For the states themselves, this isn't
appropriate due to an upcoming change I will suggest.
