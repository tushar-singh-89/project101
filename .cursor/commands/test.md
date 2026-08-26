# Test

Analyze the current implementation and create a focused test strategy.

Do not modify production code.

First identify the test cases.

## Test Categories

### Happy Path

Cover the primary expected workflows.

### Boundary Cases

Cover:

* Minimum values
* Maximum values
* Empty collections
* Single-element cases
* Boundary state transitions

### Invalid Input

Cover malformed or unsupported inputs.

### Failure Scenarios

Cover:

* Missing data
* Exceptions
* External service failures
* Invalid state
* Duplicate operations

### Concurrency

If the implementation is concurrent or shared state exists, identify relevant race conditions and thread-safety tests.

## Output

For every proposed test provide:

1. Test name
2. Given state/input
3. Action
4. Expected result
5. Why the test matters

After presenting the test plan, wait for approval before modifying test files.
