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

AS提示需要 同步工程，同步成功后，即依赖成功