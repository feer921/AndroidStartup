package com.github.androidstartup

/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2023/3/12<br>
 * Time: 18:41<br>
 * <P>DESC:
 * 任务执行的监听者
 * </p>
 * ******************(^_^)***********************
 */
internal interface ITaskListener {
    /**
     * 启动任务开始执行
     * @param theTask 当前正在执行的启动任务
     */
    fun onTaskStart(theTask: IStartupTask<*>)

    /**
     * 启动任务执行、工作结束
     * @param theTask 当前正在执行的启动任务
     * @param result 执行的结果
     */
    fun onTaskDone(theTask: IStartupTask<*>, result: Any?)

}