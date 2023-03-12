package com.fee.start;

import android.app.Application;

import com.fee.start.tasks.Task1;
import com.fee.start.tasks.Task2;
import com.fee.start.tasks.Task3;
import com.fee.start.tasks.Task4;
import com.fee.start.tasks.Task5;
import com.github.androidstartup.StartupTasksManager;
import com.github.androidstartup.StartupTasksOrganizer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
        Executor executor = new ThreadPoolExecutor(1,3,1, TimeUnit.MINUTES,
                new SynchronousQueue<>()
                );
        StartupTasksManager.Builder
                .addTask(new Task1())
                .addTask(new Task5())
                .addTask(new Task3())
                .addTask(new Task2())
                .addTask(new Task4())
                .withTaskExecutor(executor)
                .withContext(this)
                .startUp();

//        new StartupTasksOrganizer.TasksBuilder()
//                .addTask(new Task1())
//                .addTask(new Task5())
//                .addTask(new Task3())
//                .addTask(new Task2())
//                .addTask(new Task4())
//                .build(this)
//                .startUp();


    }
}
