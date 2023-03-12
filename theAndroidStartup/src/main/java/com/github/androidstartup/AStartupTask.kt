package com.github.androidstartup

import java.util.concurrent.CountDownLatch

/**
 ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2022/11/13<br>
 * Time: 20:14<br>
 * <P>DESC:
 * 启动任务接口的抽象实现基类
 * </p>
 * ******************(^_^)***********************
 */
abstract class AStartupTask<T>: IStartupTask<T> {
    protected val mTag: String by lazy(LazyThreadSafetyMode.NONE) {
        javaClass.simpleName
    }

    /**
     * 本启动任务所依赖的上游任务在完成后的 CountDown,当所有依赖的上游任务都完成后才通知本任务执行
     */
    protected val mDependentTaskCountDown by lazy(LazyThreadSafetyMode.NONE){
        CountDownLatch(getDependentTaskCount())
    }

    /**
     * 本启动任务所依赖的其他启动任务的数量
     * 之所以增加这个属性，避免在调用[getDependentTaskCount]时每次都去调用[dependentTask]
     * 重复生成 List<>
     * def = -1,子类可以赋值，本父类默认会调用[dependentTask] 来查询一次
     */
    protected var mDependentTaskCount = -1

    /**
     * 本启动任务所依赖的其他 task的数量，本任务所依赖的其他任务数量小于1时(即没有依赖其他任务)则优先执行
     */
    override fun getDependentTaskCount(): Int {
        if (mDependentTaskCount == -1) {
            mDependentTaskCount = dependentTask()?.size ?: 0
        }
        return mDependentTaskCount
    }

    /**
     * 本启动任务所依赖的其他启动任务的 Class数据集
     * 作用为：在本启动任务执行前，需要在所依赖其他任务执行完后再执行
     */
    override fun dependentTask(): List<Class<out IStartupTask<*>>>? {
        return null
    }

    //----------------
    /**
     * 标记当前启动任务先等一等
     */
    override fun letHoldOn() {
        try {
            mDependentTaskCountDown.await()
        }catch (ex: InterruptedException){
            ex.printStackTrace()
        }
    }

    /**
     * 当当前任务的上游任务完成时通知当前任务将可以执行、动作
     */
    override fun letWillAction() {
        mDependentTaskCountDown.countDown()
    }
    //----------------
}