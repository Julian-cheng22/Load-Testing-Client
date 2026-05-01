# Assignment 2 – Load Testing Client

**Group:** Qingyu Cheng & Yunhong Huang  

This project implements a **Java-based load testing system** for evaluating a Spring Boot server under high concurrency. It includes single-threaded and multi-threaded clients, performance analysis, and visualization.

---

## Project Structure

```bash
load-testing-client/
├── src/main/java/edu/neu/cs6650/client/
│   ├── SingleClient.java
│   ├── MultiThreadClient1.java
│   └── MultiThreadClient2.java
│
├── src/main/java/edu/neu/cs6650/server/
│   ├── ServerApp.java
│   └── ProductController.java
│
├── client_results/
│   ├── client-part2-results.csv
│   ├── throughput_over_time.csv
│   └── throughput_plot.png
│
├── analyze_results.py
├── plot_throughput.py
├── Dockerfile
└── README.md
```

---

## Additional Required Structure (Assignment Requirement)

```bash
clients/
├── single/
│   └── SingleClient.java
├── part1/
│   └── MultiThreadClient1.java
└── part2/
    ├── MultiThreadClient2.java
    └── client-part2-results.csv
```

These are copies of the runnable code located in:

```bash
src/main/java/edu/neu/cs6650/client/
```

---

## How to Run

### 1.Single-thread Client

```bash
cd src/main/java
javac edu/neu/cs6650/client/SingleClient.java
java edu.neu.cs6650.client.SingleClient
```

---

### 2.Multi-thread Client (Part 1)

```bash
javac edu/neu/cs6650/client/MultiThreadClient1.java
java edu.neu.cs6650.client.MultiThreadClient1
```

---

### 3.Multi-thread Client (Part 2)

Generates latency statistics and throughput analysis.

```bash
javac edu/neu/cs6650/client/MultiThreadClient2.java
java edu.neu.cs6650.client.MultiThreadClient2
```

---

## Server (Spring Boot)

Start the server:

```bash
mvn spring-boot:run
```

---

### API Endpoint

```bash
POST http://localhost:8080/products
```

Response:

```bash
201 Created
```

**Explanation:**  
HTTP `201` is returned because a new product resource is created on the server.

---

## Python Analysis (Optional)

```bash
python3 analyze_results.py
python3 plot_throughput.py
```

---

### Output Files

- `throughput_over_time.csv`
- `throughput_plot.png`

---

## Results (Included in /client_results)

- Single-thread throughput  
- Part 1 throughput and wall time  
- Part 2 latency statistics (mean / median / p99 / min / max)  
- Throughput-over-time visualization  

---

## Rubric Checklist

| Requirement | Status |
|------------|--------|
| Khoury Git repo | ✔ Included |
| README instructions | ✔ Complete |
| Java + Docker + Spring files | ✔ Included |
| Clients in 3 folders | ✔ Implemented |
| Design description | ✔ Provided (PDF) |
| 201 response explanation | ✔ Included |
| Single-thread performance | ✔ Included |
| Multi-thread scaling (Part 1) | ✔ Achieved |
| Statistical metrics (Part 2) | ✔ CSV generated |
| Throughput visualization | ✔ Included |

---

## Notes

- This project is designed for **performance testing and learning purposes**  
- Focuses on **concurrency, throughput, and latency analysis**  
- Demonstrates scaling behavior from single-thread to multi-thread environments  

---
