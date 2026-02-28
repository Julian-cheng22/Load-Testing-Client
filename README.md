Assignment2
group：Qingyu Cheng & Yunhong Huang
load-testing-client/
├── src/main/java/edu/neu/cs6650/client/
│ ├── SingleClient.java
│ ├── MultiThreadClient1.java
│ ├── MultiThreadClient2.java
│
├── src/main/java/edu/neu/cs6650/server/
│ ├── ServerApp.java
│ └── ProductController.java
│
├── client_results/
│ ├── client-part2-results.csv
│ ├── throughput_over_time.csv
│ ├── throughput_plot.png
│
├── analyze_results.py
├── plot_throughput.py
├── Dockerfile
└── README.md


To satisfy the assignment requirement of “three separate client folders”,  
this repo additionally includes:


clients/
├── single/
│ └── SingleClient.java
├── part1/
│ └── MultiThreadClient1.java
└── part2/
├── MultiThreadClient2.java
└── client-part2-results.csv


These are copies of the real runnable code from `/src/main/java/...`.  
The runnable version remains inside IntelliJ under `src/main/java`.

---

## 📌 How to Run

### 1️⃣ Single-thread Client

cd src/main/java
javac edu/neu/cs6650/client/SingleClient.java
java edu.neu.cs6650.client.SingleClient


### 2️⃣ Multi-thread Client (Part 1)

javac edu/neu/cs6650/client/MultiThreadClient1.java
java edu.neu.cs6650.client.MultiThreadClient1


### 3️⃣ Multi-thread Client (Part 2)
Generates CSV + latency statistics + throughput plot.


javac edu/neu/cs6650/client/MultiThreadClient2.java
java edu.neu.cs6650.client.MultiThreadClient2


---

## 📌 Server (Spring Boot)

Runs locally with:


mvn spring-boot:run


POST endpoint:


POST http://localhost:8080/products


Returns:


201 Created


(201 is used because a new product resource is created on the server.)

---

## 📌 Python Scripts (Optional for plots)


python3 analyze_results.py
python3 plot_throughput.py


Produces:

- `throughput_over_time.csv`
- `throughput_plot.png`

---

## 📌 Required Output (included in /client_results)

- Single-thread throughput screenshot
- Part 1 throughput & wall time
- Part 2 mean/median/p99/min/max
- Part 2 throughput-over-time plot

---

## ✔ Rubric Match Checklist

| Requirement | Status |
|------------|--------|
| Khoury Git repo | ✔ Included |
| README instructions | ✔ Yes |
| All Java, Docker, Spring files | ✔ Included |
| Clients in 3 folders | ✔ `clients/single`, `clients/part1`, `clients/part2` |
| Description of design | ✔ In PDF |
| Why server returns 201 | ✔ Included above |
| Single-thread performance | ✔ Output included |
| Client Part 1 scaling | ✔ Achieved |
| Client Part 2 statistics | ✔ CSV + mean/median/p99 |
| Throughput over time plot | ✔ Included |

---

If you have any questions, feel free to contact me.  
Thank you!  
