package com.brsthegck.kanbanboard

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.lang.reflect.Type
import java.nio.Buffer
import java.util.*

private const val TASKLIST_TYPE_TODO = 0
private const val TASKLIST_TYPE_DOING = 1
private const val TASKLIST_TYPE_DONE = 2

private const val NUM_TASKLIST_PAGES = 3
private const val ARG_TASKLIST_TYPE = "tasklist_type"

class MainActivity : AppCompatActivity(), TasklistFragment.Callbacks {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    lateinit var taskViewModel: TaskViewModel
    //private val tasklistFragmentsList = mutableListOf<TasklistFragment>()

    override fun onOptionsItemSelected(item: MenuItem) = when(item.itemId){
        R.id.action_new_task -> {
            val currentTasklistType = viewPager.currentItem

            val currentTasklist = when(currentTasklistType){
                TASKLIST_TYPE_TODO -> taskViewModel.mockupTaskListTodo
                TASKLIST_TYPE_DOING -> taskViewModel.mockupTaskListDoing
                TASKLIST_TYPE_DONE -> taskViewModel.mockupTaskListDone
                else -> throw Exception("Unrecognized tasklist type")
            }

            addTaskToViewModel(Task()
                    .apply{taskText = resources.getString(R.string.new_task); tasklistType = currentTasklistType},
                    currentTasklistType,
                    getTaskListFromViewModel(currentTasklistType).size)

            (supportFragmentManager.fragments[currentTasklistType] as TasklistFragment).taskRecyclerView.scrollToPosition(currentTasklist.size - 1)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_action_bar, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)

        Log.d("MainActivity", "Reading json files...")
        readJsonToViewModel()

        viewPager = findViewById(R.id.view_pager)
        val viewPagerAdapter = TasklistFragmentStateAdapter(this)
        viewPager.apply{
            adapter = viewPagerAdapter
            offscreenPageLimit = 2
        }

        tabLayout = findViewById(R.id.tab_layout)

        TabLayoutMediator(tabLayout, viewPager) {tab, position ->
            tab.text = when(position){
                0 -> getString(R.string.tab_label_todo)
                1 -> getString(R.string.tab_label_doing)
                else -> getString(R.string.tab_label_done)
            }
        }.attach()
    }

    override fun onStop() {
        super.onStop()
        writeViewModelToJson()
    }

    private inner class TasklistFragmentStateAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa){
        override fun createFragment(position: Int): Fragment {
            val tasklistFragment: TasklistFragment
            val tasklistFragmentArguments = Bundle().apply{
                putInt(ARG_TASKLIST_TYPE, position)
            }

            tasklistFragment = TasklistFragment().apply{
                arguments = tasklistFragmentArguments;
            }

            //tasklistFragmentsList.add(tasklistFragment)
            return tasklistFragment
        }

        override fun getItemCount(): Int = NUM_TASKLIST_PAGES
    }

    override fun addTaskToViewModel(task: Task, tasklistType: Int, adapterPosition : Int) {
        val taskList = getTaskListFromViewModel(tasklistType)

        taskList.add(task)

        //tasklistFragmentsList[tasklistType].taskRecyclerView.adapter?.notifyItemInserted(taskList.size)
        (supportFragmentManager.fragments[tasklistType] as TasklistFragment).taskRecyclerView.adapter?.notifyItemInserted(taskList.size)
    }

    override fun getTaskListFromViewModel(tasklistType: Int): LinkedList<Task> =
        when(tasklistType){
            TASKLIST_TYPE_TODO -> taskViewModel.mockupTaskListTodo
            TASKLIST_TYPE_DOING -> taskViewModel.mockupTaskListDoing
            TASKLIST_TYPE_DONE -> taskViewModel.mockupTaskListDone
            else -> throw Exception("Unrecognized tasklist type")
        }

    override fun deleteTaskFromViewModel(task: Task, tasklistType: Int, adapterPosition: Int) {
        getTaskListFromViewModel(tasklistType).removeAt(adapterPosition)
        (supportFragmentManager.fragments[tasklistType] as TasklistFragment).taskRecyclerView.adapter?.notifyItemRemoved(adapterPosition)
    }

    fun readJsonToViewModel(){
        val gson = Gson()

        val type = object: TypeToken<LinkedList<Task>>() {}.type

        try {
            val todoReader = BufferedReader(InputStreamReader(applicationContext.openFileInput("todo_list_storage.json")))
            val doingReader = BufferedReader(InputStreamReader(applicationContext.openFileInput("doing_list_storage.json")))
            val doneReader = BufferedReader(InputStreamReader(applicationContext.openFileInput("done_list_storage.json")))

            val parsedJsonList = mutableListOf<String>()

            for(reader in listOf(todoReader, doingReader, doneReader)){
                val strBuilder = StringBuilder()
                reader.readLine().let{ strBuilder.append(it) }
                parsedJsonList.add(strBuilder.toString())
            }

            todoReader.close()
            doingReader.close()
            doneReader.close()

            taskViewModel.mockupTaskListTodo = gson.fromJson(parsedJsonList[0], type)
            taskViewModel.mockupTaskListDoing = gson.fromJson(parsedJsonList[1], type)
            taskViewModel.mockupTaskListDone = gson.fromJson(parsedJsonList[2], type)
        }catch(e: FileNotFoundException){
            Log.d("MainActivity", "JSON files could not be found. Creating empty lists...")
            writeViewModelToJson()
        }
    }

    fun writeViewModelToJson() {
        val gson = Gson()

        val todoJSON = gson.toJson(taskViewModel.mockupTaskListTodo)
        val doingJSON = gson.toJson(taskViewModel.mockupTaskListDoing)
        val doneJSON = gson.toJson(taskViewModel.mockupTaskListDone)


        val fosTodo : FileOutputStream = applicationContext.openFileOutput("todo_list_storage.json", Context.MODE_PRIVATE)
        val fosDoing : FileOutputStream = applicationContext.openFileOutput("doing_list_storage.json", Context.MODE_PRIVATE)
        val fosDone : FileOutputStream = applicationContext.openFileOutput("done_list_storage.json", Context.MODE_PRIVATE)

        fosTodo.write(todoJSON.toByteArray())
        fosDoing.write(doingJSON.toByteArray())
        fosDone.write(doneJSON.toByteArray())

        fosTodo.close()
        fosDoing.close()
        fosDone.close()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {

        Log.d("MainActivity", "${taskViewModel.mockupTaskListTodo}\n${taskViewModel.mockupTaskListDoing}\n${taskViewModel.mockupTaskListDone}")
        super.onRestoreInstanceState(savedInstanceState)
    }
}