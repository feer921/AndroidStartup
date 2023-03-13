package com.github.androidstartup

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import java.util.ArrayDeque
import java.util.concurrent.Executor

/**
 ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2022/11/13<br>
 * Time: 20:59<br>
 * <P>DESC:
 * 启动任务管理者(单例)
 * </p>
 * ******************(^_^)***********************
 */
class StartupTasksManager private constructor(): ITaskListener{

    private val mTag = "StartupTasksManager"
    /**
     * 各启动任务的执行需要 Context
     */
    private var mAppContext: Context? = null

    /**
     * 启动任务的执行器(在各启动任务未提供执行器时使用给本类设置的公用执行器，最好是全局共用的)
     */
    private var mTaskExecutor: Executor? = null

    companion object Builder{
        private val mTasksManager: StartupTasksManager by lazy(LazyThreadSafetyMode.NONE){
            StartupTasksManager()
        }

        /**
         * 添加启动任务
         */
        fun withTask(startupTask: IStartupTask<*>): StartupTasksManager {
            return getTasksManager().withTask(startupTask)
        }

        /**
         * 获取 启动任务管理器
         */
        fun getTasksManager(): StartupTasksManager = mTasksManager

        /**
         * 对启动任务数据集按照 相应的关系(依赖)进行排序 (拓扑排序--广度优先: BFS)
         */
        internal fun sortStartupTasksAndResult(startupTasks: List<IStartupTask<*>>): StartupTasksRelationMapper {
            //启动任务的 Class 映射 该任务对象的依赖任务数量
            val classMapDependenceCount = mutableMapOf<Class<out IStartupTask<*>>, Int>()
            // 启动任务的 Class 映射 启动任务
            val classMapStartupTask = mutableMapOf<Class<out IStartupTask<*>>, IStartupTask<*>>()

            //本启动任务所依赖的父任务  Class 映射 子任务 的Class 数据集 List<Class>
            val parentTaskClassMapChildrenTaskClasses = mutableMapOf<Class<out IStartupTask<*>>,
                    MutableList<Class<out IStartupTask<*>>>>()
            // 所依赖的任务数量为 0 的 启动任务 Class
            val zeroDeque = ArrayDeque<Class<out IStartupTask<*>>>()
            startupTasks.forEach { task ->
                val taskClass = task::class.java
                classMapStartupTask[taskClass] = task
                val dependenceTaskCount = task.getDependentTaskCount()
                classMapDependenceCount[taskClass] = dependenceTaskCount
                //记录 所依赖的任务数量为 0 的 当前任务 Class
                if (dependenceTaskCount <= 0) {
                    zeroDeque.offer(taskClass)
                } else {
                    task.dependentTask()
                        ?.forEach { parentTaskClass -> // 这里的 parentTaskClass 指本启动任务的所依赖的更优先的任务(看成父任务)
                            var s = parentTaskClassMapChildrenTaskClasses[parentTaskClass]
                            if (s == null) {
                                s = mutableListOf()
                                parentTaskClassMapChildrenTaskClasses[parentTaskClass] = s
                            }
                            s.add(taskClass)
                        }
                }
            }
            //排序
            val sortedTaskList = mutableListOf<IStartupTask<*>>()
            //启动任务需要在主线程上执行
            val tasksInMainThread = mutableListOf<IStartupTask<*>>()
            //启动任务需要在工作线程上执行
            val tasksInWorkThread = mutableListOf<IStartupTask<*>>()
            //从 没有依赖其他任务(父任务)的队列中遍历
            while (!zeroDeque.isEmpty()) {
                val taskClass = zeroDeque.poll() ?: continue
                val theStartupTask = classMapStartupTask[taskClass] ?: continue
                if (theStartupTask.isDependonMainThread()) {
                    tasksInMainThread.add(theStartupTask)
                } else {
                    tasksInWorkThread.add(theStartupTask)
                }
                //删除该 入度为0 的
                if (parentTaskClassMapChildrenTaskClasses.containsKey(taskClass)) {
                    parentTaskClassMapChildrenTaskClasses[taskClass]?.forEach { childTaskClass ->
                        //因为 theStartupTask 作为父任务时 自身没有了父任务，则其子任务也要把所依赖的数量减 1
                        val parentTaskCount = classMapDependenceCount[childTaskClass] ?: 0
                        val leftTaskCount = parentTaskCount - 1
                        classMapDependenceCount[childTaskClass] = leftTaskCount
                        if (leftTaskCount <= 0) {
                            zeroDeque.offer(childTaskClass)
                        }
                    }
                }
            }//end while
            sortedTaskList.addAll(tasksInWorkThread)
            sortedTaskList.addAll(tasksInMainThread)
            return StartupTasksRelationMapper(
                sortedTaskList,
                classMapStartupTask,
                parentTaskClassMapChildrenTaskClasses
            )
        }
    }

    /**
     * 添加的将要执行启动任务的 数据集
     */
    private val mAddedTasksList by lazy(LazyThreadSafetyMode.NONE){
        mutableListOf<IStartupTask<*>>()
    }

    /**
     * 已排序好的启动任务数据集
     */
    private val mSortedTasksList by lazy(LazyThreadSafetyMode.NONE) {
        mutableListOf<IStartupTask<*>>()
    }

    /**
     * 当前的所有 启动任务的 任务类 Class与对应 任务对象的映射
     */
    private val mClassMapTask by lazy(LazyThreadSafetyMode.NONE){
        mutableMapOf<Class<out IStartupTask<*>>,IStartupTask<*>>()
    }

    /**
     * 当前所有的启动任务中 父任务 Class 与其子任务 Class的映射
     */
    private val mParentTaskClassMapChildrenTaskClass by lazy(LazyThreadSafetyMode.NONE){
        mutableMapOf<Class<out IStartupTask<*>>,MutableList<Class<out IStartupTask<*>>>>()
    }



    /**
     * 添加启动任务
     */
    @MainThread
    fun withTask(startupTask: IStartupTask<*>): StartupTasksManager {
        mAddedTasksList.add(startupTask)
        return this
    }

    /**
     * 开始启动、执行各启动任务
     */
    @MainThread
    fun startUp(){
        if (mAppContext == null || mAddedTasksList.isEmpty()) {
            return
        }
        sortStartupTasks(mAddedTasksList)
        mAddedTasksList.clear()//这个数据集不需要了
        mSortedTasksList.forEach { task ->
            val taskRunner = TaskRunner(appContext = mAppContext!!, task, this)
            if (task.isDependonMainThread()){
                taskRunner.run()
            } else {
                task.dependonTaskExecutor() ?: mTaskExecutor?.execute(taskRunner)
            }
        }
    }

    /**
     * 通知一个启动任务执行结束,因为该启动任务可能作为被依赖的父任务(上游任务)在执行完成后需要通知其子任务可以开始了
     * @param theTask 当前执行完成的启动任务
     */
    @WorkerThread
    @MainThread
    private fun notifyTaskDone(theTask: IStartupTask<*>){
        val classOfTask = theTask.javaClass
        if (mParentTaskClassMapChildrenTaskClass.containsKey(classOfTask)) {
            mParentTaskClassMapChildrenTaskClass[classOfTask]?.forEach {childTaskClass ->
                 mClassMapTask[childTaskClass]?.run {
                    this.letWillAction() //通知子任务可能可以开启执行了
                }
            }
        }
    }

    /**
     * 对启动任务数据集按照 相应的关系(依赖)进行排序 (拓扑排序--广度优先 BFS)
     */
    @MainThread
    private fun sortStartupTasks(startupTasks: List<IStartupTask<*>>){
        sortStartupTasksAndResult(startupTasks).let {
            mSortedTasksList.addAll(it.sortedTasks)
            mParentTaskClassMapChildrenTaskClass.putAll(it.parentTaskClassMapChildrenTaskClass)
            mClassMapTask.putAll(it.classMapTask)
        }
    }

    fun getAssignExecutor() = mTaskExecutor

    /**
     * 指定外部的可能全局的 任务执行器
     * @param mayGlobalTaskExecutor [Executor],比如共用全局的线程池
     */
    fun withTaskExecutor(mayGlobalTaskExecutor: Executor): StartupTasksManager {
        this.mTaskExecutor = mayGlobalTaskExecutor
        return this
    }

    fun withContext(appContext: Context): StartupTasksManager {
        this.mAppContext = appContext
        return this
    }

    //------------- 启动任务的执行监听 @start ---------------
    /**
     * 启动任务开始执行
     * @param theTask 当前正在执行的启动任务
     */
    override fun onTaskStart(theTask: IStartupTask<*>) {
        Log.i(mTag,"--> onTaskStart() theTask = $theTask")
    }

    /**
     * 启动任务执行、工作结束
     * @param theTask 当前正在执行的启动任务
     * @param result 执行的结果
     */
    override fun onTaskDone(theTask: IStartupTask<*>, result: Any?) {
        notifyTaskDone(theTask)
        // TODO: 缓存执行结果？？
    }
    //------------- 启动任务的执行监听 @end ---------------

}