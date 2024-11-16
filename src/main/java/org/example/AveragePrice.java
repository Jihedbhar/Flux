package org.example;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public  class AveragePrice implements AggregateFunction<CryptoPrice, Tuple2<Double, Integer>, Double> {
    @Override
    public Tuple2<Double, Integer> createAccumulator() {
        return new Tuple2<>(0.0, 0);
    }

    @Override
    public Tuple2<Double, Integer> add(CryptoPrice value, Tuple2<Double, Integer> accumulator) {
        return new Tuple2<>(accumulator.f0 + value.getPrice(), accumulator.f1 + 1);
    }

    @Override
    public Double getResult(Tuple2<Double, Integer> accumulator) {
        return accumulator.f0 / accumulator.f1;
    }

    @Override
    public Tuple2<Double, Integer> merge(Tuple2<Double, Integer> a, Tuple2<Double, Integer> b) {
        return new Tuple2<>(a.f0 + b.f0, a.f1 + b.f1);
    }
}