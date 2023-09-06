package moe.styx.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun startTasks() {
    val taskerJob = Job()
    val scope = CoroutineScope(taskerJob)
    for (task in Tasks.values()) {
        scope.launch {
            if (task.initialWait > 0)
                delay(task.initialWait * 1000L)
            var failed = 0
            var lastfailed: Boolean
            while (true) {
                try {
                    lastfailed = false
                    task.run()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    failed++
                    lastfailed = true
                }
                if (!lastfailed || (lastfailed && failed < 3))
                    delay(task.seconds * 1000L)
            }
        }
    }
}