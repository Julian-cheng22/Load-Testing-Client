package edu.neu.cs6650.client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CS6650 Assignment 2 - Client Part 2
 *
 * Multithreaded load-testing client with latency recording and statistics.
 */
public class MultiThreadClient2 {

    // ===================== Config =====================

    // 你可以自己微调这些参数
    private static final String TARGET_URL = "http://localhost:8080/products";

    // 总共要发的请求数（作业要求 200_000）
    private static final int TOTAL_REQUESTS = 200_000;

    // worker 线程数（你之前 Part1 用的是 32，可以按需要改）
    private static final int NUM_WORKER_THREADS = 32;

    // 队列容量（生产者线程生成 Product 的缓冲区）
    private static final int QUEUE_CAPACITY = 100_000;

    // 每个请求最多重试次数（包括第一次）
    private static final int MAX_RETRIES = 5;

    // 输出 CSV 文件名
    private static final String CSV_FILE = "client-part2-results.csv";

    // 用于通知 worker“没有更多任务了”的 POISON PILL
    private static final String POISON_PILL = "__POISON_PILL__";

    // ===================== Data classes =====================

    /**
     * 单个请求的统计记录
     */
    private static class RequestRecord {
        final long startTimeMillis;
        final long latencyMillis;
        final String method;
        final int statusCode;

        RequestRecord(long startTimeMillis, long latencyMillis, String method, int statusCode) {
            this.startTimeMillis = startTimeMillis;
            this.latencyMillis = latencyMillis;
            this.method = method;
            this.statusCode = statusCode;
        }
    }

    // ===================== Main =====================

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("===== MultiThread Client (Part 2) =====");
        System.out.println("Target URL : " + TARGET_URL);
        System.out.println("Total requests : " + TOTAL_REQUESTS);
        System.out.println("Worker threads : " + NUM_WORKER_THREADS);
        System.out.println("Max retries : " + MAX_RETRIES);
        System.out.println("Queue capacity : " + QUEUE_CAPACITY);
        System.out.println();

        long wallStart = System.currentTimeMillis();

        // 用队列连接“生成产品”的线程和“发送请求”的 worker 线程
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

        // 用于统计总成功/失败
        AtomicInteger globalSuccess = new AtomicInteger(0);
        AtomicInteger globalFail = new AtomicInteger(0);

        // 每个 worker 自己的 RequestRecord 列表，最后再汇总
        List<List<RequestRecord>> allThreadRecords = new ArrayList<>();

        // 1. 启动生成产品的线程
        Thread producer = new Thread(() -> {
            try {
                ProductGenerator generator = new ProductGenerator();
                for (int i = 0; i < TOTAL_REQUESTS; i++) {
                    String productJson = generator.nextProductJson();
                    queue.put(productJson);
                }
                // 所有产品生成完毕后，放入 POISON_PILL，数量 = worker 数
                for (int i = 0; i < NUM_WORKER_THREADS; i++) {
                    queue.put(POISON_PILL);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "ProductGenerator");
        producer.start();

        // 2. 启动 worker 线程
        List<Thread> workers = new ArrayList<>();

        for (int i = 0; i < NUM_WORKER_THREADS; i++) {
            List<RequestRecord> threadRecords = Collections.synchronizedList(new ArrayList<>());
            allThreadRecords.add(threadRecords);

            Thread worker = new Thread(() -> {
                try {
                    while (true) {
                        String body = queue.take();
                        if (POISON_PILL.equals(body)) {
                            // 收到毒丸，结束
                            break;
                        }

                        boolean success = false;
                        int statusCode = -1;
                        long startTime = System.currentTimeMillis();

                        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                            long attemptStart = System.currentTimeMillis();
                            try {
                                statusCode = sendPost(TARGET_URL, body);
                                long latency = System.currentTimeMillis() - attemptStart;

                                // 记录这次 HTTP 调用
                                threadRecords.add(new RequestRecord(
                                        attemptStart,
                                        latency,
                                        "POST",
                                        statusCode
                                ));

                                if (statusCode >= 200 && statusCode < 300) {
                                    success = true;
                                    break;
                                }

                            } catch (IOException e) {
                                long latency = System.currentTimeMillis() - attemptStart;
                                // 网络/IO 异常也记一条记录，statusCode = -1
                                threadRecords.add(new RequestRecord(
                                        attemptStart,
                                        latency,
                                        "POST",
                                        -1
                                ));
                            }
                        }

                        if (success) {
                            globalSuccess.incrementAndGet();
                        } else {
                            globalFail.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Worker-" + i);

            workers.add(worker);
            worker.start();
        }

        // 3. 等待所有线程结束
        producer.join();
        for (Thread t : workers) {
            t.join();
        }

        long wallEnd = System.currentTimeMillis();
        long wallTimeMillis = wallEnd - wallStart;
        double wallTimeSeconds = wallTimeMillis / 1000.0;

        int successCount = globalSuccess.get();
        int failCount = globalFail.get();
        int actualRequests = successCount + failCount;

        // 4. 汇总所有 RequestRecord
        List<RequestRecord> allRecords = new ArrayList<>();
        for (List<RequestRecord> perThread : allThreadRecords) {
            allRecords.addAll(perThread);
        }

        // 5. 写 CSV
        writeCsv(allRecords, CSV_FILE);

        // 6. 计算统计信息
        StatsResult stats = computeStats(allRecords, wallTimeMillis);

        // 7. 打印最终 summary
        System.out.println();
        System.out.println("===== MultiThread Client Summary (Part 2) =====");
        System.out.println("Configured Requests : " + TOTAL_REQUESTS);
        System.out.println("Actual Sent         : " + actualRequests);
        System.out.println("Success             : " + successCount);
        System.out.println("Fail                : " + failCount);
        System.out.println("Wall time           : " + wallTimeMillis + " ms");
        System.out.println("Throughput          : " + String.format("%.2f", stats.throughput) + " requests/sec");
        System.out.println();
        System.out.println("Mean latency        : " + String.format("%.3f", stats.meanLatency) + " ms");
        System.out.println("Median latency      : " + stats.medianLatency + " ms");
        System.out.println("p99 latency         : " + stats.p99Latency + " ms");
        System.out.println("Min latency         : " + stats.minLatency + " ms");
        System.out.println("Max latency         : " + stats.maxLatency + " ms");
        System.out.println();
        System.out.println("CSV written to: " + CSV_FILE);
        System.out.println("===============================================");
    }

    // ===================== HTTP & Product generator =====================

    /**
     * 发送一次 POST 请求，返回 HTTP status code。
     * 这里使用最简单的 HttpURLConnection。
     */
    private static int sendPost(String targetUrl, String body) throws IOException {
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(5_000);
        conn.setRequestProperty("Content-Type", "application/json");

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        conn.getOutputStream().write(bytes);

        int statusCode = conn.getResponseCode();
        conn.disconnect();
        return statusCode;
    }

    /**
     * 根据作业要求生成随机 Product。
     * 这里只是简单版本，用 JSON 字符串表示。
     * 如果你已经有自己的 Product 类，可以把这里替换成你现有的生成逻辑。
     */
    private static class ProductGenerator {
        private final Random random = new Random();

        String nextProductJson() {
            long productId = nextUnsignedInt();
            String sku = randomSku(10);
            String manufacturer = randomWord(3);
            int categoryId = 1 + random.nextInt(100_000);
            int weight = 1_000 + random.nextInt(9_001); // [1000,10000]
            long someOtherId = nextUnsignedInt();

            // 简单 JSON 字符串，字段名可以随便，只要 server 接收的是 String body 就行
            return String.format(
                    "{\"productId\":%d,\"sku\":\"%s\",\"manufacturer\":\"%s\",\"categoryId\":%d,\"weight\":%d,\"someOtherId\":%d}",
                    productId, sku, manufacturer, categoryId, weight, someOtherId
            );
        }

        private long nextUnsignedInt() {
            // [1, 2^32 - 1]
            long value = (random.nextInt() & 0xffffffffL);
            if (value == 0) value = 1;
            return value;
        }

        private String randomSku(int length) {
            char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(chars[random.nextInt(chars.length)]);
            }
            return sb.toString();
        }

        private String randomWord(int numWords) {
            String[] syllables = {"la", "mi", "zo", "ka", "ne", "ra", "to", "xi", "ba", "lu"};
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numWords; i++) {
                if (i > 0) sb.append(' ');
                sb.append(
                        syllables[random.nextInt(syllables.length)]
                                + syllables[random.nextInt(syllables.length)]
                );
            }
            return sb.toString();
        }
    }

    // ===================== Stats & CSV =====================

    private static void writeCsv(List<RequestRecord> records, String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("startTimeMillis,requestType,latencyMillis,responseCode");
            writer.newLine();
            for (RequestRecord r : records) {
                writer.write(r.startTimeMillis + "," + r.method + "," + r.latencyMillis + "," + r.statusCode);
                writer.newLine();
            }
        }
    }

    private static class StatsResult {
        final double meanLatency;
        final long medianLatency;
        final long p99Latency;
        final long minLatency;
        final long maxLatency;
        final double throughput;

        StatsResult(double meanLatency, long medianLatency, long p99Latency,
                    long minLatency, long maxLatency, double throughput) {
            this.meanLatency = meanLatency;
            this.medianLatency = medianLatency;
            this.p99Latency = p99Latency;
            this.minLatency = minLatency;
            this.maxLatency = maxLatency;
            this.throughput = throughput;
        }
    }

    private static StatsResult computeStats(List<RequestRecord> records, long wallTimeMillis) {
        if (records.isEmpty()) {
            return new StatsResult(0, 0, 0, 0, 0, 0);
        }

        int n = records.size();
        long[] latencies = new long[n];
        long sum = 0;
        for (int i = 0; i < n; i++) {
            latencies[i] = records.get(i).latencyMillis;
            sum += latencies[i];
        }

        double mean = sum / (double) n;

        java.util.Arrays.sort(latencies);
        long median = latencies[n / 2];
        int p99Index = (int) Math.ceil(0.99 * n) - 1;
        if (p99Index < 0) p99Index = 0;
        if (p99Index >= n) p99Index = n - 1;
        long p99 = latencies[p99Index];

        long min = latencies[0];
        long max = latencies[n - 1];

        double throughput = (n * 1_000.0) / wallTimeMillis;

        return new StatsResult(mean, median, p99, min, max, throughput);
    }
}