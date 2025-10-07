package com.xiamen.metro.message.service.pump;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 时间序列分析工具类
 *
 * @author Xiamen Metro System
 */
@Slf4j
@Component
public class TimeSeriesAnalyzer {

    /**
     * 移动平均
     */
    public static List<Double> movingAverage(List<Double> data, int windowSize) {
        if (data == null || data.isEmpty() || windowSize <= 0) {
            return new ArrayList<>();
        }

        List<Double> result = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            int start = Math.max(0, i - windowSize + 1);
            int end = i + 1;

            double sum = 0;
            int count = 0;
            for (int j = start; j < end; j++) {
                if (data.get(j) != null) {
                    sum += data.get(j);
                    count++;
                }
            }

            result.add(count > 0 ? sum / count : null);
        }

        return result;
    }

    /**
     * 计算标准差
     */
    public static double standardDeviation(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return 0;
        }

        double mean = mean(data);
        double sumSquaredDeviations = 0;
        int validCount = 0;

        for (Double value : data) {
            if (value != null) {
                sumSquaredDeviations += Math.pow(value - mean, 2);
                validCount++;
            }
        }

        return validCount > 1 ? Math.sqrt(sumSquaredDeviations / (validCount - 1)) : 0;
    }

    /**
     * 计算平均值
     */
    public static double mean(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return 0;
        }

        double sum = 0;
        int count = 0;
        for (Double value : data) {
            if (value != null) {
                sum += value;
                count++;
            }
        }

        return count > 0 ? sum / count : 0;
    }

    /**
     * 计算中位数
     */
    public static double median(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return 0;
        }

        List<Double> sortedData = data.stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (sortedData.isEmpty()) {
            return 0;
        }

        int size = sortedData.size();
        if (size % 2 == 0) {
            return (sortedData.get(size / 2 - 1) + sortedData.get(size / 2)) / 2;
        } else {
            return sortedData.get(size / 2);
        }
    }

    /**
     * 检测异常值（IQR方法）
     */
    public static List<Integer> detectOutliers(List<Double> data) {
        List<Integer> outlierIndices = new ArrayList<>();

        if (data == null || data.size() < 4) {
            return outlierIndices;
        }

        List<Double> sortedData = data.stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (sortedData.size() < 4) {
            return outlierIndices;
        }

        double q1 = percentile(sortedData, 25);
        double q3 = percentile(sortedData, 75);
        double iqr = q3 - q1;

        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;

        for (int i = 0; i < data.size(); i++) {
            Double value = data.get(i);
            if (value != null && (value < lowerBound || value > upperBound)) {
                outlierIndices.add(i);
            }
        }

        return outlierIndices;
    }

    /**
     * 计算百分位数
     */
    public static double percentile(List<Double> sortedData, double percentile) {
        if (sortedData == null || sortedData.isEmpty()) {
            return 0;
        }

        double index = (percentile / 100) * (sortedData.size() - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);

        if (lowerIndex == upperIndex) {
            return sortedData.get(lowerIndex);
        }

        double weight = index - lowerIndex;
        return sortedData.get(lowerIndex) * (1 - weight) + sortedData.get(upperIndex) * weight;
    }

    /**
     * 线性回归预测
     */
    public static LinearRegressionResult linearRegression(List<Double> x, List<Double> y) {
        if (x == null || y == null || x.size() != y.size() || x.isEmpty()) {
            return new LinearRegressionResult(0, 0, 0);
        }

        int n = x.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            if (x.get(i) != null && y.get(i) != null) {
                sumX += x.get(i);
                sumY += y.get(i);
                sumXY += x.get(i) * y.get(i);
                sumX2 += x.get(i) * x.get(i);
            }
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            return new LinearRegressionResult(0, 0, 0);
        }

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        // 计算R²
        double meanY = sumY / n;
        double totalSumSquares = 0, residualSumSquares = 0;

        for (int i = 0; i < n; i++) {
            if (x.get(i) != null && y.get(i) != null) {
                double predicted = slope * x.get(i) + intercept;
                totalSumSquares += Math.pow(y.get(i) - meanY, 2);
                residualSumSquares += Math.pow(y.get(i) - predicted, 2);
            }
        }

        double rSquared = totalSumSquares > 0 ? 1 - (residualSumSquares / totalSumSquares) : 0;

        return new LinearRegressionResult(slope, intercept, rSquared);
    }

    /**
     * 趋势分析
     */
    public static TrendAnalysisResult analyzeTrend(List<Double> data) {
        if (data == null || data.size() < 2) {
            return new TrendAnalysisResult(TrendDirection.STABLE, 0, 0);
        }

        // 过滤空值
        List<Double> cleanData = data.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (cleanData.size() < 2) {
            return new TrendAnalysisResult(TrendDirection.STABLE, 0, 0);
        }

        // 创建时间索引
        List<Double> timeIndices = new ArrayList<>();
        for (int i = 0; i < cleanData.size(); i++) {
            timeIndices.add((double) i);
        }

        // 线性回归分析趋势
        LinearRegressionResult regression = linearRegression(timeIndices, cleanData);

        // 判断趋势方向
        TrendDirection direction;
        double threshold = cleanData.size() > 10 ? 0.01 : 0.05;

        if (Math.abs(regression.getSlope()) < threshold) {
            direction = TrendDirection.STABLE;
        } else if (regression.getSlope() > 0) {
            direction = TrendDirection.INCREASING;
        } else {
            direction = TrendDirection.DECREASING;
        }

        // 计算趋势强度（基于R²）
        double strength = Math.abs(regression.getRSquared());

        return new TrendAnalysisResult(direction, regression.getSlope(), strength);
    }

    /**
     * 检测突变点
     */
    public static List<Integer> detectChangePoints(List<Double> data, double threshold) {
        List<Integer> changePoints = new ArrayList<>();

        if (data == null || data.size() < 3) {
            return changePoints;
        }

        // 使用简单的差分方法检测突变点
        for (int i = 1; i < data.size() - 1; i++) {
            if (data.get(i) == null || data.get(i - 1) == null || data.get(i + 1) == null) {
                continue;
            }

            double prevDiff = Math.abs(data.get(i) - data.get(i - 1));
            double nextDiff = Math.abs(data.get(i + 1) - data.get(i));
            double avgDiff = (prevDiff + nextDiff) / 2;

            // 计算局部标准差
            int windowSize = Math.min(5, i);
            List<Double> window = new ArrayList<>();
            for (int j = Math.max(0, i - windowSize); j <= Math.min(data.size() - 1, i + windowSize); j++) {
                if (data.get(j) != null) {
                    window.add(data.get(j));
                }
            }

            if (window.size() > 2) {
                double localStd = standardDeviation(window);
                if (localStd > 0 && avgDiff / localStd > threshold) {
                    changePoints.add(i);
                }
            }
        }

        return changePoints;
    }

    /**
     * 线性回归结果
     */
    public static class LinearRegressionResult {
        private final double slope;
        private final double intercept;
        private final double rSquared;

        public LinearRegressionResult(double slope, double intercept, double rSquared) {
            this.slope = slope;
            this.intercept = intercept;
            this.rSquared = rSquared;
        }

        public double getSlope() { return slope; }
        public double getIntercept() { return intercept; }
        public double getRSquared() { return rSquared; }
    }

    /**
     * 趋势方向枚举
     */
    public enum TrendDirection {
        INCREASING("上升"),
        DECREASING("下降"),
        STABLE("稳定"),
        FLUCTUATING("波动");

        private final String description;

        TrendDirection(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 趋势分析结果
     */
    public static class TrendAnalysisResult {
        private final TrendDirection direction;
        private final double slope;
        private final double strength;

        public TrendAnalysisResult(TrendDirection direction, double slope, double strength) {
            this.direction = direction;
            this.slope = slope;
            this.strength = strength;
        }

        public TrendDirection getDirection() { return direction; }
        public double getSlope() { return slope; }
        public double getStrength() { return strength; }
    }
}