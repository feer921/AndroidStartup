package com.github.androidstartup

import android.content.Context

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2023/3/12<br>
 * Time: 15:46<br>
 * <P>DESC:
 * 任务的执行者
 * </p>
 * ******************(^_^)***********************
 */
internal class TaskRunner(appContext: Context,startupTask: IStartupTask<*>,taskListener: ITaskListener?) : Runnable {
    private val mCurStartupTask: IStartupTask<*> = startupTask
    private val mContext = appContext
    private var mTaskListener: ITaskListener? = taskListener
    override fun run() {
        android.os.Process.setThreadPriority(mCurStartupTask.indicateTaskPriority())
        //当当前的启动任务执行前，先等一等，看是否有依赖的上游任务，如果没有，则会进行下一步
        mCurStartupTask.letHoldOn()
        mTaskListener?.onTaskStart(mCurStartupTask)
        //执行启动任务的工作
        val doResult =  mCurStartupTask.doStartupTask(mContext)
        // TODO: 缓存任务执行的结果
        //启动任务执行完成后，通知子任务所依赖的本上游任务执行完成
        mTaskListener?.onTaskDone(mCurStartupTask,doResult)
    }

}