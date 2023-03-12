package com.github.androidstartup


/**
 ******************(^_^)***********************<br>
 * Author: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2023/3/12<br>
 * Time: 16:38<br>
 * <P>DESC:
 * 启动任务关系映射
 * </p>
 * ******************(^_^)***********************
 */
internal class StartupTasksRelationMapper(
    val sortedTasks: MutableList<IStartupTask<*>>,
    val classMapTask: MutableMap<Class<out IStartupTask<*>>, IStartupTask<*>>,
    val parentTaskClassMapChildrenTaskClass: MutableMap<Class<out IStartupTask<*>>, MutableList<Class<out IStartupTask<*>>>>
)