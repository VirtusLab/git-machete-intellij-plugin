# Overview through Slide In use cases

This document is an overview through the use cases.
It should help to implement and understand the feature.

## Use cases

| UC ID | Local branch exists | Branch entry exists | Estimated frequency |
| :---: | :---: | :---: | :---: |
| 1 | false | false | ~ 75% |
| 2 | false | true  | < 1%  |
| 3 | true  | false | ~ 25% |
| 4 | true  | true  | < 1%  |

## Slide In - Logic

This specification covers all of the cases.

- When the **entry does not exist**, it must be added under the given branch.
- When the **entry exists under the given parent**, branch layout must not be affected.
- When the **entry exists under other parent or is root**, it must be reattached under the given parent.

**IMPORTANT NOTE:**
Sliding in an entry under itself or any of its descendants in the branch layout must not be allowed.
