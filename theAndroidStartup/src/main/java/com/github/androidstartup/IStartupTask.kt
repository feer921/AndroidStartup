package com.github.androidstartup

import android.content.Context

/**
 ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2022/11/13<br>
 * Time: 19:48<br>
 * <P>DESC:
 * 启动任务接口
 * </p>
 * ******************(^_^)***********************
 */
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