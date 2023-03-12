package com.github.androidstartup

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import java.util.concurrent.Executor

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2023/3/12<br>
 * Time: 17:36<br>
 * <P>DESC:
 * 启动任务组织者(每个组织者组织一组启动任务)
 * </p>
 * ******************(^_^)***********************
 */
class StartupTasksOrganizer(private val mAppContext: Context?,private val mAddedTaskList: List<IStartupTask<*>>):ITaskListener {

    private var mTasksRelationMapper: StartupTasksRelationMapper? = null


    /**
     * 开始启动一组 启动任务
     */
    @MainThread
    fun startUp(){
        if (mAppContext == null || mAddedTaskList.isEmpty()) {
            return
        }
        //排序
        mTasksRelationMapper = StartupTasksManager.sortStartupTasksAndResult(mAddedTaskList).apply {
            sortedTasks.forEach { task ->
                val taskRunner = TaskRunner(mAppContext,task,this@StartupTasksOrganizer)
                if (task.isDependonMainThread()){
                    taskRunner.run()
                } else {
                    task.dependonTaskExecutor() ?: theSameTaskExecutor()?.execute(taskRunner)
                }
            }
        }
    }

    private fun theSameTaskExecutor(): Executor? {
        return StartupTasksManager.getTasksManager().getAssignExecutor()
    }

    //------------- 启动任务的执行监听 @start ---------------
    /**
     * 启动任务开始执行
     * @param theTask 当前正在执行的启动任务
     */
    override fun onTaskStart(theTask: IStartupTask<*>) {
    }

    /**
     * 一个启动任务执行、工作结束
     * @param theTask 当前正在执行的启动任务
     * @param result 执行的结果
     */
    override fun onTaskDone(theTask: IStartupTask<*>, result: Any?) {
        notifyTaskDone(theTask)
        // TODO: 缓存执行结果？？
    }
    //------------- 启动任务的执行监听 @end ---------------

    @WorkerThread
    @MainThread
    private fun notifyTaskDone(theTask: IStartupTask<*>){
        mTasksRelationMapper?.run {
            val classOfTask = theTask.javaClass
            if (this.parentTaskClassMapChildrenTaskClass.containsKey(classOfTask)) {
                this.parentTaskClassMapChildrenTaskClass[classOfTask]?.forEach {childTaskClass ->
                    this.classMapTask[childTaskClass]?.run {
                        this.letWillAction() //通知子任务可能可以开启执行了
                    }
                }
            }
        }
    }

    /**
     * 一组启动任务的构建者
     */
    companion object class TasksBuilder{
        private val mAddedTasks by lazy(LazyThreadSafetyMode.NONE){
            mutableListOf<IStartupTask<*>>()
        }

        private var mContext: Context? = null

        fun addTask(task: IStartupTask<*>): TasksBuilder{
            mAddedTasks.add(task)
            return this
        }

        fun build(context: Context): TasksBuilder {
            this.mContext = context
            return this
        }

        fun startUp(){
            StartupTasksOrganizer(mContext,mAddedTasks).startUp()
        }
    }
}