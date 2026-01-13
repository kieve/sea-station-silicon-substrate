# From Code Review
- Maybe rename "StateContext"

# From Testing

# Eventually

--------




Refactoring:
RandomBranchState defines a RESET_PACKAGE which it uses for reflection to load YAML
Let's try and centralize all this yaml loading reflection code into the content package, and avoid
having it leak into other classes, like states. It would be okay for the states to call into a
static method to do what it needs to do.
