package com.github.androidstartup

import java.util.ArrayDeque

/**
 ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2022/11/13<br>
 * Time: 20:59<br>
 * <P>DESC:
 *
 * </p>
 * ******************(^_^)***********************
 */
class StartupManager {
    companion object Helper{

        /**
         * 对启动任务数据集按照 相应的关系(依赖)进行排序
         */
        fun sortStartupTasks(startupTasks: List<IStartupTask<*>> ){
            //启动任务的 Class 映射 该任务对象的依赖任务数量
            val classMapDependenceCount = mutableMapOf<Class<out IStartupTask<*>>, Int>()
            // 启动任务的 Class 映射 启动任务
            val classMapStartupTask = mutableMapOf<Class<out IStartupTask<*>>,IStartupTask<*>>()

            //启动任务 的  Class 映射 所依赖的 其他 启动任务的Class 数据集 List<Class>
            val classMapDependenceTaskClasses = mutableMapOf<Class<in IStartupTask<*>>,
                    List<Class<IStartupTask<*>>>>()

            // 所依赖的任务数量为 0 的 启动任务 Class
            val zeroDeque = ArrayDeque<Class<out IStartupTask<*>>>()

            startupTasks.forEach { task ->
                val taskClass = task::class.java
                classMapStartupTask[taskClass] = task
                val dependenceTaskCount = task.dependenceTaskCount
                classMapDependenceCount[taskClass] = dependenceTaskCount
                //记录 所依赖的任务数量为 0 的 当前任务 Class
                if (dependenceTaskCount == 0) {
                    zeroDeque.offer(taskClass)
                } else {
                    task.dependentTask()?.forEach { parentTaskClass ->
                        var s = classMapDependenceTaskClasses[parentTaskClass]
                        if (s == null) {
                            s = mutableListOf()
                            classMapDependenceTaskClasses[parentTaskClass] = s
                        }

                    }
                }
            }
        }
    }
}