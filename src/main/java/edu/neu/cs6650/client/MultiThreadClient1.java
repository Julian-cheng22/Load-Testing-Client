package edu.neu.cs6650.client;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadClient1 {

    // ===== 可调参数 =====
    private static final String TARGET_URL = "http://localhost:8080/products";
    private static final int TOTAL_REQUESTS = 200_000;      // 总请求数
    private static final int NUM_WORKERS = 32;              // worker 线程数
    private static final int MAX_RETRIES = 5;               // 最大重试次数
    private static final int QUEUE_CAPACITY = 10_000;       // 队列容量

    private static final String POISON_PILL = "__POISON_PILL__";

    public static void main(String[] args) throws Exception {

        System.out.println("===== MultiThread Client (Part 1) =====");
        System.out.println("Target URL      : " + TARGET_URL);
        System.out.println("Total requests  : " + TOTAL_REQUESTS);
        System.out.println("Worker threads  : " + NUM_WORKERS);
        System.out.println("Max retries     : " + MAX_RETRIES);
        System.out.println("Queue capacity  : " + QUEUE_CAPACITY);
        System.out.println("=======================================\n");

        BlockingQueue<String> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        // 1️⃣ 启动生产者线程
        Thread producer = new Thread(new ProductProducer(queue, TOTAL_REQUESTS));
        producer.start();

        // 2️⃣ 启动 worker 线程
        Thread[] workers = new Thread[NUM_WORKERS];
        long startWall = System.currentTimeMillis();

        for (int i = 0; i < NUM_WORKERS; i++) {
            workers[i] = new Thread(new Worker(queue, success, fail));
            workers[i].start();
        }

        // 3️⃣ 等待生产者结束，然后投放毒丸
        producer.join();
        for (int i = 0; i < NUM_WORKERS; i++) {
            queue.put(POISON_PILL);
        }

        // 4️⃣ 等待所有 worker 结束
        for (Thread t : workers) {
            t.join();
        }

        long endWall = System.currentTimeMillis();
        long wallTime = endWall - startWall;

        int successCount = success.get();
        int failCount = fail.get();
        int totalSent = successCount + failCount;

        double throughput = totalSent / (wallTime / 1000.0);

        System.out.println("\n===== MultiThread Client Summary (Part 1) =====");
        System.out.println("Configured Requests : " + TOTAL_REQUESTS);
        System.out.println("Actual Sent         : " + totalSent);
        System.out.println("Success             : " + successCount);
        System.out.println("Fail                : " + failCount);
        System.out.println("Wall time           : " + wallTime + " ms");
        System.out.println("Throughput          : " + String.format("%.2f", throughput) + " requests/sec");
        System.out.println("===============================================");
    }

    // ==================== 生产者：生成 Product 请求体 ====================

    private static class ProductProducer implements Runnable {
        private final BlockingQueue<String> queue;
        private final int total;
        private final Random random = new Random();

        public ProductProducer(BlockingQueue<String> queue, int total) {
            this.queue = queue;
            this.total = total;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < total; i++) {
                    String body = generateRandomProductJson(i + 1);
                    queue.put(body);
                    // 这里不打印日志，否则 IO 太慢
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 这里随便造一个 JSON，server 现在不校验 schema，只要能收就行
        private String generateRandomProductJson(long seq) {
            long productId = 1L + (Math.abs(random.nextLong()) % 4_294_967_295L);
            String sku = randomSku(10);
            String manufacturer = "Maker-" + seq;
            int categoryId = 1 + random.nextInt(100_000);
            int weight = 1000 + random.nextInt(9001); // 1000~10000
            long someOtherId = 1L + (Math.abs(random.nextLong()) % Integer.MAX_VALUE);

            return String.format(
                    "{\"productID\":%d,\"sku\":\"%s\",\"manufacturer\":\"%s\",\"categoryID\":%d,\"weight\":%d,\"someOtherID\":%d}",
                    productId, sku, manufacturer, categoryId, weight, someOtherId
            );
        }

        private String randomSku(int len) {
            char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append(chars[random.nextInt(chars.length)]);
            }
            return sb.toString();
        }
    }

    // ==================== Worker：从队列取任务并发送 POST ====================

    private static class Worker implements Runnable {
        private final BlockingQueue<String> queue;
        private final AtomicInteger success;
        private final AtomicInteger fail;

        public Worker(BlockingQueue<String> queue, AtomicInteger success, AtomicInteger fail) {
            this.queue = queue;
            this.success = success;
            this.fail = fail;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String body = queue.take();
                    if (POISON_PILL.equals(body)) {
                        break; // 收到毒丸，退出
                    }

                    int status = sendPostWithRetry(TARGET_URL, body, MAX_RETRIES);
                    if (status == 201) {
                        success.incrementAndGet();
                    } else {
                        fail.incrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ==================== HTTP 工具方法 ====================

    private static int sendPostWithRetry(String urlStr, String body, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            attempts++;
            int code = sendPostOnce(urlStr, body);
            if (code == 201) {
                return code;
            }
            // 4xx / 5xx / -1 都重试
        }
        return -1;
    }

    private static int sendPostOnce(String urlStr, String body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            return conn.getResponseCode();

        } catch (Exception ex) {
            return -1;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}