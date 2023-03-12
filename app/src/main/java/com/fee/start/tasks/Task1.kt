package com.fee.start.tasks

import android.content.Context
import com.github.androidstartup.AStartupTask
import java.util.concurrent.Executor

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2023/3/11<br>
 * Time: 22:15<br>
 * <P>DESC:
 *
 * </p>
 * ******************(^_^)***********************
 */
class Task1: AStartupTask<String>() {

    override fun doStartupTask(context: Context): String {
        return "task1 result"
    }

    /**
     * 标识启动任务是否依赖主线程的执行
     */
    override fun isDependonMainThread(): Boolean {
        return true
    }

    /**
     * 启动任务所依赖的任务 执行器 [Executor]
     */
    override fun dependonTaskExecutor(): Executor? {
        return null
    }
}