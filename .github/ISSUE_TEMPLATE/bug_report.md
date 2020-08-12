---
name: Bug report
about: Create a report to help us improve our plugin
title: ''
labels: bug
assignees: ''

---

## Bug description
Please include steps to reproduce (like `go to...`/`click on...` etc.) + expected and actual behavior.

For more non-trivial issues, we would also appreciate if you included the following details:

### Running environment
 - Git Machete plugin version
 - IDE type [e.g. IntelliJ, PyCharm]
 - IDE version [e.g. 2020.1.4]
 - Operating system [e.g. Windows]

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
to list under `Help` -> `Diagnostic Tools` -> `Debug Log Settings`.<br/>
Then reproduce a bug and attach the logs to the issue.<br/>
To find IntelliJ log file, go to `Help` -> `Show Log in Files`.

Consider placing the logs within the `details` (aka "spoiler") tags as the may be very extensive.

<details>
<summary>Logs</summary>
Logs go here
</details>

### Screenshots
If applicable, add screenshots (or screen recordings, see [Peek](https://github.com/phw/peek#peek---an-animated-gif-recorder) on Linux)
to help explain your problem.
