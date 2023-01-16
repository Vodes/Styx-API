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

            while (true) {
                try {
                    task.run()
                    delay(task.seconds * 1000L)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }
}