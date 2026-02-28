import pandas as pd
import matplotlib.pyplot as plt
import os

CSV_FILE = "throughput_over_time.csv"
PNG_FILE = "throughput_over_time.png"

if not os.path.exists(CSV_FILE):
    raise FileNotFoundError(f"{CSV_FILE} not found in current directory")

df = pd.read_csv(CSV_FILE)

# 打印一下列名，方便你确认
print("Columns in CSV:", list(df.columns))

# 优先用列名 second / throughput，如果没有就用前两列
if "second" in df.columns:
    x = df["second"]
else:
    x = df.iloc[:, 0]

if "throughput" in df.columns:
    y = df["throughput"]
else:
    y = df.iloc[:, 1]

plt.figure(figsize=(10, 5))
plt.plot(x, y, label="Instantaneous Throughput (req/s)")
plt.xlabel("Time (seconds)")
plt.ylabel("Throughput (requests/second)")
plt.title("Throughput Over Time - MultiThread Client (Part 2)")
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.savefig(PNG_FILE, dpi=200)
print(f"Saved plot to {PNG_FILE}")