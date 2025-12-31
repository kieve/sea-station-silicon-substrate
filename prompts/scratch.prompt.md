# From Code Review
- Rather than the behavior being a controller, we should specify the controller component,
  explicitly
- blocks.yaml use a parent to define others. Let's do something similar with the entities.yaml
- Maybe rename "StateContext"

# From Testing
- Attack state still hits you when it's dead.

# Eventually
- ChaseState defined and fills out a dijkstra map, but this should be cached and reused in the game
  context

--------

In AiController, we're storing state specific persistent data for the wander state.
We shouldn't be doing this. It should be stored in the state instance itself.
Let's refactor this to clean it up.


