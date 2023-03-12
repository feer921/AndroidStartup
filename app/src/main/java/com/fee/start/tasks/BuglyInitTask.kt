package com.fee.start.tasks

import android.content.Context
import com.github.androidstartup.AStartupTask
import com.github.androidstartup.IStartupTask

/**
 ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2023/3/12<br>
 * Time: 21:21<br>
 * <P>DESC:
 * 第三方 SDK bugly的初始化任务
 * </p>
 * ******************(^_^)***********************
 */
class BuglyInitTask: AStartupTask<String>() {
    /**
     * 执行任务
     * @return [T] 执行了启动任务可能的 返回数据
     */
    override fun doStartupTask(context: Context): String {
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