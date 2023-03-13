# 介绍

本框架目的为提供 **Android**  APP 在启动时的启动流程优化，统一管理和配置各初始化任务按照开发时所规划的依赖顺序和配置执行，可作为组件化的一个公共组件。

# 前言

我们所开发的 **Android** 项目，在APP启动的时候往往需要提前初始化一些业务模块、第三方SDK等，简单的写法是在 项目自定义的 **Application** 的 *onCreate()* 方法中根据可能的先后顺序一股脑的写上初始化代码，这样会让各模块的初始化工作太过于集中执行，可能引起主线程的耗时，因而非常有必要(尤其对于中大型项目来说)将项目中需要初始化的各功能模块及第三方SDK等的初始化任务按照一定的先后依赖顺序、有条理的 按照分任务的方式进行设计、规划。

# 使用

```kotlin
class LocalApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        val gloabalExecutor: Executor = ThreadPoolExecutor(
            1,
            3,
            1,
            TimeUnit.MINUTES,
            SynchronousQueue()
        )
        StartupTasksManager.getTasksManager()
            .withContext(this) //设置上下文 Context
            .withTaskExecutor(gloabalExecutor) //配置启动任务的全局 Executor(线程池)
            .withTask(Task1())
            .withTask(Task5())
            .withTask(Task3())
            .withTask(Task4())
            .withTask(Task2())
            .withTask(BuglyInitTask())
            .startUp() //开始启动任务
    }
}
```

# 依赖

项目的根目录下的 **build.gradle** 中添加 maven仓库(由于Jcenter已废，基本都有maven的默认仓库)

```groovy
buildscript {
    
    repositories {
        google()
        mavenCentral() //增加 maven仓库
    }
}
```

再在 **app** 模块目录下的 **build.gradle** 文件中增加对本组件库的依赖

```groovy
dependencies {
    implementation 'io.github.feer921:AndroidStartup:1.0'
}
```

AS提示需要 同步工程，同步成功后，即依赖成功。

# 核心类、APIs

初始化任务被定义为 启动任务接口，所有的启动初始化任务皆为该接口的子类：

```kotlin
interface IStartupTask<T> : ITaskStateIndicate{

    /**
     * 执行任务
     * @return [T] 执行了启动任务可能的 返回数据
     */
    fun doStartupTask(context: Context): T

    /**
     * 本启动任务所依赖的其他 task的数量，本任务所依赖的其他任务数量小于1时(即没有依赖其他任务)则优先执行
     */
    fun getDependentTaskCount(): Int


    /**
     * 本启动任务所依赖的其他启动任务的 Class数据集
     * 作用为：在本启动任务执行前，需要在所依赖其他任务执行完后再执行
     */
    fun dependentTask(): List<Class<out IStartupTask<*>>>?


}
```

其中类范型<T> 表示启动任务的执行时 **fun doStartupTask(context: Context): T** 可能需要返回执行结果，而 **ITaskStateIndicate** 接口意义为 启动任务的状态指示，为了让各任务子类对外标示优先级、运行需求的线程是否为主线程、提供自定义的任务执行器 **Executor(线程池)** 等：

```kotlin
interface ITaskStateIndicate {
    /**
     * 标识任务的执行优先级
     * def = Process.THREAD_PRIORITY_DEFAULT == 0
     */
    fun indicateTaskPriority(): Int = android.os.Process.THREAD_PRIORITY_DEFAULT

    /**
     * 标识启动任务是否依赖主线程的执行
     */
    fun isDependonMainThread(): Boolean

    /**
     * 启动任务所依赖的任务 执行器 [Executor]
     */
    fun dependonTaskExecutor(): Executor? = null

    /**
     * 标记当前启动任务先等一等
     */
    fun letHoldOn()

    /**
     * 当当前任务的上游任务完成时通知当前任务将可以执行、动作
     */
    fun letWillAction()
}
```

进而为了方便开发者编写自定义的启动任务类，框架有必要默认提供一个抽象的启动任务类,以提供通用的接口实现

```kotlin
abstract class AStartupTask<T>: IStartupTask<T> {
    protected val mTag: String by lazy(LazyThreadSafetyMode.NONE) {
        javaClass.simpleName
    }

    /**
     * 本启动任务所依赖的上游任务在完成后的 CountDown,当所有依赖的上游任务都完成后才通知本任务执行
     */
    private val mDependentTaskCountDown by lazy(LazyThreadSafetyMode.NONE){
        CountDownLatch(getDependentTaskCount())
    }

    /**
     * 本启动任务所依赖的其他启动任务的数量
     * 之所以增加这个属性，避免在调用[getDependentTaskCount]时每次都去调用[dependentTask]
     * 重复生成 List<>
     * def = -1,子类可以赋值，本父类默认会调用[dependentTask] 来查询一次
     */
    protected var mDependentTaskCount = -1

    /**
     * 本启动任务所依赖的其他 task的数量，本任务所依赖的其他任务数量小于1时(即没有依赖其他任务)则优先执行
     */
    override fun getDependentTaskCount(): Int {
        if (mDependentTaskCount == -1) {
            mDependentTaskCount = dependentTask()?.size ?: 0
        }
        return mDependentTaskCount
    }

    /**
     * 本启动任务所依赖的其他启动任务的 Class数据集
     * 作用为：在本启动任务执行前，需要在所依赖其他任务执行完后再执行
     */
    override fun dependentTask(): List<Class<out IStartupTask<*>>>? {
        return null
    }

    //----------------
    /**
     * 标记当前启动任务先等一等
     */
    override fun letHoldOn() {
        try {
            mDependentTaskCountDown.await()
        }catch (ex: InterruptedException){
            ex.printStackTrace()
        }
    }

    /**
     * 当当前任务的上游任务完成时通知当前任务将可以执行、动作
     */
    override fun letWillAction() {
        mDependentTaskCountDown.countDown()
    }
    //----------------

    override fun toString(): String {
        return "task: $mTag,mDependentTaskCount = $mDependentTaskCount"
    }
}
```

开发使用本框架基本上只需要 继承 以上该抽象启动类。

# 编写自定义的启动任务类

简单示例，项目中需要对 **Bugly** 此类第三方SDK的初始化，我们取名为 BuglyInitTask：

```kotlin
class BuglyInitTask: AStartupTask<String>() {
    /**
     * 执行任务
     * @return [T] 执行了启动任务可能的 返回数据
     */
    override fun doStartupTask(context: Context): String {
      	// Bugly.init()
        return "bugly sdk init finish"
    }

    /**
     * 标识启动任务是否依赖主线程的执行
     */
    override fun isDependonMainThread(): Boolean {
        return true
    }

    override fun dependentTask(): List<Class<out IStartupTask<*>>>? {
        return listOf(Task1::class.java)
    }
}
```

当然如果所自定义的启动任务还有其他的配置需求，可以查看其父类以重写实现。

# 单例模式启动任务组(简单)

大多数情况下，我们的项目中把1个～N个启动初始化任务集只规划成一组即可，由框架中 **StartupTasksManager** 懒汉式单例进行添加与管理：

```kotlin
// API有节选： 
/** <P>DESC:
 * 启动任务管理者(单例)
 * </p>
 * ******************(^_^)***********************
 */
class StartupTasksManager private constructor(): ITaskListener{
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
}
```

再开发编写完各自定义的启动任务后，在相应的位置(一般为 **Applicaton** 初始化的位置) 按照前方的 API 进行配置、启动：

```kotlin
class LocalApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        val gloabalExecutor: Executor = ThreadPoolExecutor(
            1,
            3,
            1,
            TimeUnit.MINUTES,
            SynchronousQueue()
        )
        StartupTasksManager.getTasksManager()
            .withContext(this) //设置上下文 Context
            .withTaskExecutor(gloabalExecutor) //配置启动任务的全局 Executor(线程池)
            .withTask(Task1())
            .withTask(Task5())
            .withTask(Task3())
            .withTask(Task4())
            .withTask(Task2())
            .withTask(BuglyInitTask())
            .startUp() //开始启动任务
    }
}
```



# 分组模式启动任务组(启动任务再分组)

这种模式即对应的项目中确实可能存在比较复杂的启动任务需求，比如我们需要把 Task1,Task2,Task3 看成一组启动任务集，另把 Task4,Task5,BuglyInitTask 看成另外一组任务集，然后再分别启动，可能这些分组的任务集之间又存在依赖顺序关系(框架中暂时还未实现)，这个需求目前被 **StartupTasksOrganizer** 类看成任务集的组织者来实现：

```kotlin
//API代码有节选
/** <P>DESC:
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
  
  /**
     * 一组启动任务的构建者
     */
    companion object class TasksBuilder{
        private val mAddedTasks by lazy(LazyThreadSafetyMode.NONE){
            mutableListOf<IStartupTask<*>>()
        }

        private var mContext: Context? = null

        fun withTask(task: IStartupTask<*>): TasksBuilder{
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
```

可见由 构建者模式的 **TasksBuilder** 来进行构建，则分组模式代码示例：

```kotlin
				//分组模式下的代码示例：
        //第一组：
        StartupTasksOrganizer.TasksBuilder()
            .withTask(Task1())
            .withTask(Task2())
            .withTask(Task3())
            .build(this)
            .startUp()
        //第二组：
        StartupTasksOrganizer.TasksBuilder()
            .withTask(Task4())
            .withTask(Task5())
            .withTask(BuglyInitTask())
            .build(this)
            .startUp()
```

至此，本启动任务框架基本上介绍完了，具体可以翻阅源码精读(有着较良好的代码注释)，感谢提供任何建议，为谢！



