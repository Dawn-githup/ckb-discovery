package org.example;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Scanner;

import static io.restassured.RestAssured.given;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of threads: ");
        int numberOfThreads = scanner.nextInt();

        int numberOfRequests = 1000000;

        // 创建一个固定大小的线程池，可以同时执行多个请求
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // 计数器用于跟踪完成的任务数量
        AtomicInteger completedTasks = new AtomicInteger(0);

        // 计数器用于统计每秒请求数
        AtomicInteger requestsPerSecond = new AtomicInteger(0);

        // 提交任务给线程池执行
        for (int i = 0; i < numberOfRequests; i++) {
            Runnable task = new ApiRequestTask(i, completedTasks, requestsPerSecond);
            executorService.submit(task);
        }

        // 启动一个定时任务来统计每秒请求数并输出
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            int currentCompleted = completedTasks.getAndSet(0); // 获取并重置已完成任务数量
            System.out.println("Requests per Second: " + currentCompleted);
        }, 1, 1, TimeUnit.SECONDS);

        // 关闭线程池
        executorService.shutdown();

        try {
            // 等待所有任务完成，或者设定一个合适的超时时间
            executorService.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class ApiRequestTask implements Runnable {
    private int taskId;
    private AtomicInteger completedTasks;
    private AtomicInteger requestsPerSecond;

    public ApiRequestTask(int taskId, AtomicInteger completedTasks, AtomicInteger requestsPerSecond) {
        this.taskId = taskId;
        this.completedTasks = completedTasks;
        this.requestsPerSecond = requestsPerSecond;
    }

    @Override
    public void run() {
        // 在每个任务中执行API请求
        String result = given()
                .header("Content-Type", "application/json")
                .get("https://api-nodes.ckb.dev/peer?unknown_offline_timeout=10080&network=mirana")
                .toString();

        // 输出任务ID和结果
        System.out.println("Task " + taskId + " Result: " + result);

        // 增加已完成任务的计数
        completedTasks.incrementAndGet();

        // 增加每秒请求数的计数
        requestsPerSecond.incrementAndGet();
    }
}

