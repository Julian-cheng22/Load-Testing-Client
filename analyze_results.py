#!/usr/bin/env python3
import csv
from collections import defaultdict
from statistics import mean
import math

INPUT_CSV = "client-part2-results.csv"
THROUGHPUT_CSV = "throughput_over_time.csv"


def read_records(filename):
    records = []
    with open(filename, newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            # CSV 头是: startTimeMillis,requestType,latencyMillis,responseCode
            start = int(row["startTimeMillis"])
            latency = int(row["latencyMillis"])
            code = int(row["responseCode"])
            records.append((start, latency, code))
    return records


def compute_basic_stats(records):
    latencies = [lat for _, lat, _ in records]
    latencies_sorted = sorted(latencies)

    n = len(latencies)
    avg = mean(latencies)
    median = latencies_sorted[n // 2]
    p99_index = max(0, min(n - 1, math.ceil(0.99 * n) - 1))
    p99 = latencies_sorted[p99_index]
    min_lat = latencies_sorted[0]
    max_lat = latencies_sorted[-1]

    return {
        "count": n,
        "mean": avg,
        "median": median,
        "p99": p99,
        "min": min_lat,
        "max": max_lat,
    }


def compute_throughput_over_time(records, bucket_size_ms=1000):
    """
    把时间轴按 bucket_size_ms（默认 1 秒）分桶，
    统计每个桶里的请求数量 = 每秒瞬时吞吐量。
    """
    if not records:
        return []

    # 先按 startTime 排序
    records_sorted = sorted(records, key=lambda r: r[0])
    base_time = records_sorted[0][0]

    buckets = defaultdict(int)
    for start, _, _ in records_sorted:
        bucket_index = (start - base_time) // bucket_size_ms
        buckets[bucket_index] += 1

    # 输出按时间顺序排好
    result = []
    for bucket_index in sorted(buckets.keys()):
        start_ms = base_time + bucket_index * bucket_size_ms
        count = buckets[bucket_index]
        result.append((bucket_index, start_ms, count))
    return result


def write_throughput_csv(buckets, filename):
    with open(filename, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["secondIndex", "bucketStartMillis", "requests"])
        for sec, start_ms, count in buckets:
            writer.writerow([sec, start_ms, count])


def main():
    records = read_records(INPUT_CSV)
    print(f"Loaded {len(records)} records from {INPUT_CSV}")

    stats = compute_basic_stats(records)
    print("Basic latency stats from CSV:")
    print(f"  Count   : {stats['count']}")
    print(f"  Mean    : {stats['mean']:.3f} ms")
    print(f"  Median  : {stats['median']} ms")
    print(f"  p99     : {stats['p99']} ms")
    print(f"  Min     : {stats['min']} ms")
    print(f"  Max     : {stats['max']} ms")

    buckets = compute_throughput_over_time(records)
    write_throughput_csv(buckets, THROUGHPUT_CSV)
    print(f"Wrote throughput-over-time data to {THROUGHPUT_CSV}")


if __name__ == "__main__":
    main()