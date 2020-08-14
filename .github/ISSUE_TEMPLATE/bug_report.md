---
name: Bug report
about: Create a report to help us improve our plugin
title: ''
labels: bug
assignees: ''

---

### Bug description
A clear and concise description of what the bug is.

### Steps to reproduce
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

### Expected behavior
Description of what you expected to happen.

### Current behavior
Describe what is actually happening.

### Logs
Enable logging on debug level for this plugin by adding
```
binding
branchlayout
gitcore
gitmachete.backend
gitmachete.frontend.actions
gitmachete.frontend.externalsystem
gitmachete.frontend.graph
gitmachete.frontend.ui
```
to list under `Help` -> `Diagnostic Tools` -> `Debug Log Settings`.
Then reproduce a bug and attach logs here.
To find IntelliJ log file go to `Help` -> `Show Log in Files`.

Consider placing the logs within the `details` (aka "spoiler") tags as the may be very extensive.

<details>
<summary>Logs</summary>
Logs go here
</details>

### Screenshots
If applicable, add screenshots to help explain your problem.

### Running environment
 - Operating system [e.g. Windows]
 - IDE type [e.g. IntelliJ, PyCharm]
 - IDE version [e.g. 2020.1.4]

### Additional context
Add any other context about the problem here.
