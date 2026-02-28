package edu.neu.cs6650.client;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SingleClient {

    private static final String TARGET_URL = "http://localhost:8080/products";
    private static final int TOTAL_REQUESTS = 10_000;

    public static void main(String[] args) throws Exception {

        long totalLatency = 0;
        int success = 0;
        int fail = 0;

        System.out.println("Running single-thread test: " + TOTAL_REQUESTS + " POST requests");
        System.out.println("Target URL = " + TARGET_URL);

        long startWall = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            long start = System.currentTimeMillis();
            int status = sendPost(TARGET_URL, "hello");
            long end = System.currentTimeMillis();

            long latency = end - start;
            totalLatency += latency;

            if (status == 201) {
                success++;
            } else {
                fail++;
            }

            // 你只输出前 10 条就行，避免屏幕爆炸
            if (i < 10) {
                System.out.println("Request " + (i+1) +
                        " → status=" + status +
                        ", latency=" + latency + " ms");
            }
        }

        long endWall = System.currentTimeMillis();
        long wallTime = endWall - startWall;

        double avgLatency = totalLatency * 1.0 / TOTAL_REQUESTS;
        double throughput = TOTAL_REQUESTS / (wallTime / 1000.0);

        System.out.println("\n===== Single Thread Test Summary =====");
        System.out.println("Total Requests: " + TOTAL_REQUESTS);
        System.out.println("Success: " + success);
        System.out.println("Fail: " + fail);
        System.out.println("Wall time: " + wallTime + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " requests/sec");
        System.out.println("Average Latency: " + String.format("%.3f", avgLatency) + " ms");
        System.out.println("======================================");
    }

    private static int sendPost(String urlStr, String body) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/plain");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            int code = conn.getResponseCode();
            conn.disconnect();
            return code;

        } catch (Exception ex) {
            return -1;
        }
    }
}