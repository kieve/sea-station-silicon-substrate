# From Code Review
- Maybe rename "StateContext"

# From Testing

# Eventually

--------

In FleeState, it calls "calculateManhattanDistance" which is defined within the file.
Let's move this method to the Vec3i class. Called like, "manhattanDistTo".

For the StateEvaluator, I wonder if it would make sense to initialize it with the common unchanging
parameters (like game context), then have methods that can be called only passing minimal parameters
Rather than using static methods where all parameters need to always be passed.



Refactoring:
RandomBranchState defines a RESET_PACKAGE which it uses for reflection to load YAML
Let's try and centralize all this yaml loading reflection code into the content package, and avoid
having it leak into other classes, like states. It would be okay for the states to call into a
static method to do what it needs to do.
