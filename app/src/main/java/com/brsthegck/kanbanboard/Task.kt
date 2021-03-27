package com.brsthegck.kanbanboard

import java.util.UUID

enum class TaskColor(){YELLOW, PINK, BLUE, GREEN}

data class Task(val taskId: UUID = UUID.randomUUID(),
                var taskText: String = "",
                var color: TaskColor = TaskColor.YELLOW,
                var tasklistType: Int = -1)
