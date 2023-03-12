package com.github.androidstartup

import java.util.concurrent.Executor

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2023/3/12<br>
 * Time: 14:10<br>
 * <P>DESC:
 * 启动任务的状态标识接口
 * </p>
 * ******************(^_^)***********************
 */
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