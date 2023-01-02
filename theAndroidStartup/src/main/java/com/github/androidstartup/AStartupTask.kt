package com.github.androidstartup

/**
 ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2022/11/13<br>
 * Time: 20:14<br>
 * <P>DESC:
 *
 * </p>
 * ******************(^_^)***********************
 */
abstract class AStartupTask<T>: IStartupTask<T> {
    /**
     * 本启动任务所信赖的其他 task的数量
     */
    override var dependenceTaskCount: Int
        get() = dependentTask()?.size ?: 0
        set(value) {}

    /**
     * 本启动任务所信赖的其他启动任务的 Class数据集
     * 作用为：在本启动任务执行前，需要在所依赖其他任务执行完后再执行
     */
    override fun dependentTask(): List<Class<in IStartupTask<*>>>? {
        return null
    }
}