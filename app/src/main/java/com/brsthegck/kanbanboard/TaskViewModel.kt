package com.brsthegck.kanbanboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import java.util.*

class TaskViewModel : ViewModel() {
    var mockupTaskListTodo = LinkedList<Task>();
    var mockupTaskListDoing = LinkedList<Task>();
    var mockupTaskListDone = LinkedList<Task>();
}