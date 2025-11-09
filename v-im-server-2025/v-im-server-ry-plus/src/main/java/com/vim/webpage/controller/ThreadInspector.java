package com.vim.webpage.controller;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ThreadInspector {

    // ...existing code...
    @GetMapping("/inspect-threads")
    public Map<String, Object> inspectThreads() {
        Map<String, Object> info = new HashMap<>();
        int hardwareThreads = Runtime.getRuntime().availableProcessors();
        info.put("硬件线程数 (Hardware Threads)", hardwareThreads);

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        int platformThreads = threadMXBean.getThreadCount();
        info.put("平台线程数 (Platform Threads/OS Threads)", platformThreads);

        var allStacks = Thread.getAllStackTraces();
        var allThreads = allStacks.keySet();

        long virtualThreads = allThreads.stream().filter(Thread::isVirtual).count();
        info.put("虚拟线程数 (Virtual Threads)", virtualThreads);

        // ForkJoinPool 的 worker 线程
        long fjpWorkers = allThreads.stream()
                .filter(t -> !t.isVirtual())
                .filter(t -> t.getName().matches("ForkJoinPool-\\d+-worker-\\d+"))
                .count();
        info.put("ForkJoinPool worker 估算", fjpWorkers);

        // 当前正承载虚拟线程执行的“活跃载体”
        long activeCarriers = allStacks.entrySet().stream()
                .filter(e -> !e.getKey().isVirtual())
                .filter(e -> e.getKey().getName().matches("ForkJoinPool-\\d+-worker-\\d+"))
                .filter(e -> {
                    for (StackTraceElement ste : e.getValue()) {
                        String cls = ste.getClassName();
                        if (cls.equals("java.lang.VirtualThread")
                                || cls.startsWith("jdk.internal.vm.Continuation")) {
                            return true; // 正在执行/恢复虚拟线程
                        }
                    }
                    return false;
                })
                .count();
        info.put("活跃 Carrier 估算 (正在承载VT)", activeCarriers);

        return info;
    }
}