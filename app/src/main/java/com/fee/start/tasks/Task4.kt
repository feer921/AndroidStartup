package com.fee.start.tasks

import android.content.Context
import com.github.androidstartup.AStartupTask
import com.github.androidstartup.IStartupTask

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2023/3/11<br>
 * Time: 22:20<br>
 * <P>DESC:
 *
 * </p>
 * ******************(^_^)***********************
 */
class Task4: AStartupTask<Int>() {
    /**
     * 执行任务
     * @return [T] 执行了启动任务可能的 返回数据
     */
    override fun doStartupTask(context: Context): Int {
        
        return 0
    }

    override fun dependentTask(): List<Class<out IStartupTask<*>>>? {
        return listOf(Task1::class.java)
    }
}