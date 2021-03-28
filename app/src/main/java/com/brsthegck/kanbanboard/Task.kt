package com.brsthegck.kanbanboard

import java.util.UUID

/**
 * A data class representing single task on tasklists
 */
data class Task(val taskId: UUID = UUID.randomUUID(),
                var taskText: String = "",
                var color: TaskColor = TaskColor.YELLOW)
