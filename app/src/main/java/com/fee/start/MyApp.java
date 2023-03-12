package com.fee.start;

import android.app.Application;

import com.fee.start.tasks.Task1;
import com.fee.start.tasks.Task2;
import com.fee.start.tasks.Task3;
import com.fee.start.tasks.Task4;
import com.fee.start.tasks.Task5;
import com.github.androidstartup.StartupTasksManager;
import com.github.androidstartup.StartupTasksOrganizer;

/**
 * *****************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2022/11/23<br>
 * Time: 13:18<br>
 * <P>DESC:
 *
 * </p>
 * ******************(^_^)***********************
 */
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StartupTasksManager.Builder
                .addTask(new Task1())
                .addTask(new Task5())
                .addTask(new Task3())
                .addTask(new Task2())
                .addTask(new Task4())
                .startUp();

        new StartupTasksOrganizer.TasksBuilder()
                .addTask(new Task1())
                .addTask(new Task5())
                .addTask(new Task3())
                .addTask(new Task2())
                .addTask(new Task4())
                .build(this)
                .startUp();


    }
}
