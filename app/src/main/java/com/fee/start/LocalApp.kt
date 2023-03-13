package com.fee.start

import android.app.Application
import com.fee.start.tasks.*
import com.github.androidstartup.StartupTasksManager
import com.github.androidstartup.StartupTasksOrganizer
import java.util.concurrent.Executor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2023/3/12<br>
 * Time: 20:49<br>
 * <P>DESC:
 *
 * </p>
 * ******************(^_^)***********************
 */
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

    }
}