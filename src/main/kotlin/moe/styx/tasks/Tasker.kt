package moe.styx.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun startTasks() {
    val taskerJob = Job()
    val scope = CoroutineScope(taskerJob)
    for (task in Tasks.entries) {
        scope.launch {
            if (task.initialWait > 0)
                delay(task.initialWait * 1000L)
            var failed = 0
            var lastFailed: Boolean
            while (true) {
                try {
                    lastFailed = false
                    task.run()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    failed++
                    lastFailed = true
                }
                if (!lastFailed || (lastFailed && failed < 3))
                    delay(task.seconds * 1000L)
            }
        }
    }
}