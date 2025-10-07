package com.xiamen.metro.message.service.pump;

import com.xiamen.metro.message.dto.pump.PumpAnalysisRequestDTO;
import com.xiamen.metro.message.dto.pump.PumpAnalysisResponseDTO;
import com.xiamen.metro.message.entity.PumpDataEntity;
import com.xiamen.metro.message.repository.PumpDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 水泵模型准确率验证测试
 * 生成已知异常的数据并验证模型检测准确率
 *
 * @author Xiamen Metro System
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PumpModelAccuracyValidationTest {

    @Autowired
    private PumpIntelligentAnalysisService intelligentAnalysisService;

    @Autowired
    private PumpAnomalyDetectionService anomalyDetectionService;

    @Autowired
    private PumpFaultPredictionService faultPredictionService;

    @Autowired
    private PumpDataRepository pumpDataRepository;

    private static final String TEST_DEVICE_ID = "ACCURACY_TEST_PUMP";
    private LocalDateTime testStartTime;
    private LocalDateTime testEndTime;

    @BeforeEach
    void setUp() {
        testStartTime = LocalDateTime.now().minusDays(30);
        testEndTime = LocalDateTime.now();

        // 清理测试数据
        pumpDataRepository.deleteAll();
    }

    @Test
    @DisplayName("验证启泵频率异常检测准确率")
    void validateStartupFrequencyAccuracy() {
        ModelAccuracyResult result = new ModelAccuracyResult("启泵频率异常检测");

        // 测试正常数据（应该被识别为正常）
        List<PumpDataEntity> normalData = generateNormalStartupFrequencyData();
        result.addTestCase("正常启泵频率", normalData, false);

        // 测试异常数据（应该被识别为异常）
        List<PumpDataEntity> anomalyData = generateHighStartupFrequencyData();
        result.addTestCase("高频启泵异常", anomalyData, true);

        // 测试低频数据
        List<PumpDataEntity> lowFreqData = generateLowStartupFrequencyData();
        result.addTestCase("低频启泵异常", lowFreqData, true);

        // 执行测试
        executeAccuracyTest(result);

        // 验证准确率
        assertTrue(result.getOverallAccuracy() >= 0.8,
            String.format("启泵频率异常检测准确率应不低于80%%，实际: %.2f%%", result.getOverallAccuracy() * 100));

        System.out.println(result.generateReport());
    }

    @Test
    @DisplayName("验证运行时间异常检测准确率")
    void validateRuntimeAccuracy() {
        ModelAccuracyResult result = new ModelAccuracyResult("运行时间异常检测");

        // 正常运行时间
        List<PumpDataEntity> normalRuntimeData = generateNormalRuntimeData();
        result.addTestCase("正常运行时间", normalRuntimeData, false);

        // 过长运行时间
        List<PumpDataEntity> longRuntimeData = generateLongRuntimeData();
        result.addTestCase("运行时间过长", longRuntimeData, true);

        // 过短运行时间
        List<PumpDataEntity> shortRuntimeData = generateShortRuntimeData();
        result.addTestCase("运行时间过短", shortRuntimeData, true);

        // 执行测试
        executeAccuracyTest(result);

        // 验证准确率
        assertTrue(result.getOverallAccuracy() >= 0.75,
            String.format("运行时间异常检测准确率应不低于75%%，实际: %.2f%%", result.getOverallAccuracy() * 100));

        System.out.println(result.generateReport());
    }

    @Test
    @DisplayName("验证能耗趋势分析准确率")
    void validateEnergyTrendAccuracy() {
        ModelAccuracyResult result = new ModelAccuracyResult("能耗趋势分析");

        // 稳定能耗
        List<PumpDataEntity> stableEnergyData = generateStableEnergyData();
        result.addTestCase("稳定能耗趋势", stableEnergyData, false);

        // 上升趋势能耗
        List<PumpDataEntity> increasingEnergyData = generateIncreasingEnergyData();
        result.addTestCase("能耗上升趋势", increasingEnergyData, true);

        // 下降趋势能耗
        List<PumpDataEntity> decreasingEnergyData = generateDecreasingEnergyData();
        result.addTestCase("能耗下降趋势", decreasingEnergyData, true);

        // 波动能耗
        List<PumpDataEntity> fluctuatingEnergyData = generateFluctuatingEnergyData();
        result.addTestCase("能耗波动", fluctuatingEnergyData, true);

        // 执行测试
        executeAccuracyTest(result);

        // 验证准确率
        assertTrue(result.getOverallAccuracy() >= 0.7,
            String.format("能耗趋势分析准确率应不低于70%%，实际: %.2f%%", result.getOverallAccuracy() * 100));

        System.out.println(result.generateReport());
    }

    @Test
    @DisplayName("验证振动异常检测准确率")
    void validateVibrationAccuracy() {
        ModelAccuracyResult result = new ModelAccuracyResult("振动异常检测");

        // 正常振动
        List<PumpDataEntity> normalVibrationData = generateNormalVibrationData();
        result.addTestCase("正常振动", normalVibrationData, false);

        // 高振动
        List<PumpDataEntity> highVibrationData = generateHighVibrationData();
        result.addTestCase("高振动异常", highVibrationData, true);

        // 间歇性振动异常
        List<PumpDataEntity> intermittentVibrationData = generateIntermittentVibrationData();
        result.addTestCase("间歇性振动异常", intermittentVibrationData, true);

        // 振动趋势上升
        List<PumpDataEntity> increasingVibrationData = generateIncreasingVibrationData();
        result.addTestCase("振动趋势上升", increasingVibrationData, true);

        // 执行测试
        executeAccuracyTest(result);

        // 验证准确率
        assertTrue(result.getOverallAccuracy() >= 0.8,
            String.format("振动异常检测准确率应不低于80%%，实际: %.2f%%", result.getOverallAccuracy() * 100));

        System.out.println(result.generateReport());
    }

    @Test
    @DisplayName("验证故障预测准确率")
    void validateFaultPredictionAccuracy() {
        ModelAccuracyResult result = new ModelAccuracyResult("故障预测");

        // 正常设备（低故障概率）
        List<PumpDataEntity> healthyDeviceData = generateHealthyDeviceData();
        result.addTestCase("健康设备", healthyDeviceData, false, 0.3); // 阈值30%

        // 早期故障迹象（中等故障概率）
        List<PumpDataEntity> earlyFaultData = generateEarlyFaultData();
        result.addTestCase("早期故障迹象", earlyFaultData, true, 0.3); // 阈值30%

        // 严重故障迹象（高故障概率）
        List<PumpDataEntity> severeFaultData = generateSevereFaultData();
        result.addTestCase("严重故障迹象", severeFaultData, true, 0.3); // 阈值30%

        // 执行测试
        executeFaultPredictionTest(result);

        // 验证准确率
        assertTrue(result.getOverallAccuracy() >= 0.65,
            String.format("故障预测准确率应不低于65%%，实际: %.2f%%", result.getOverallAccuracy() * 100));

        System.out.println(result.generateReport());
    }

    @Test
    @DisplayName("验证综合分析准确率")
    void validateOverallAnalysisAccuracy() {
        ModelAccuracyResult result = new ModelAccuracyResult("综合分析");

        // 综合正常情况
        List<PumpDataEntity> overallNormalData = generateOverallNormalData();
        result.addTestCase("综合正常情况", overallNormalData, false);

        // 综合异常情况
        List<PumpDataEntity> overallAnomalyData = generateOverallAnomalyData();
        result.addTestCase("综合异常情况", overallAnomalyData, true);

        // 执行测试
        executeOverallAnalysisTest(result);

        // 验证准确率
        assertTrue(result.getOverallAccuracy() >= 0.75,
            String.format("综合分析准确率应不低于75%%，实际: %.2f%%", result.getOverallAccuracy() * 100));

        System.out.println(result.generateReport());
    }

    @Test
    @DisplayName("生成模型准确率验证报告")
    void generateAccuracyReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("水泵问题模型引擎 - 准确率验证报告");
        System.out.println("=".repeat(80));

        // 运行所有验证测试
        validateStartupFrequencyAccuracy();
        validateRuntimeAccuracy();
        validateEnergyTrendAccuracy();
        validateVibrationAccuracy();
        validateFaultPredictionAccuracy();
        validateOverallAnalysisAccuracy();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("验证测试完成");
        System.out.println("=".repeat(80));
    }

    // 测试执行方法
    private void executeAccuracyTest(ModelAccuracyResult result) {
        for (ModelAccuracyResult.TestCase testCase : result.getTestCases()) {
            try {
                // 保存测试数据
                pumpDataRepository.saveAll(testCase.getData());

                // 创建分析请求
                PumpAnalysisRequestDTO request = createAnalysisRequest();
                request.setAnalysisTypes(determineAnalysisTypes(result.getModelName()));

                // 执行分析
                PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);

                // 判断检测结果
                boolean hasAnomaly = hasAnomalyDetected(response, result.getModelName());

                // 更新测试结果
                testCase.setDetectedAnomaly(hasAnomaly);
                testCase.setCorrect(hasAnomaly == testCase.isExpectedAnomaly());
                testCase.setResponse(response);

            } catch (Exception e) {
                testCase.setError(e.getMessage());
                testCase.setCorrect(false);
            }
        }
    }

    private void executeFaultPredictionTest(ModelAccuracyResult result) {
        for (ModelAccuracyResult.TestCase testCase : result.getTestCases()) {
            try {
                // 保存测试数据
                pumpDataRepository.saveAll(testCase.getData());

                // 创建分析请求
                PumpAnalysisRequestDTO request = createAnalysisRequest();
                request.setAnalysisTypes(List.of(PumpAnalysisRequestDTO.AnalysisType.FAULT_PREDICTION));

                // 执行分析
                PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);

                // 判断预测结果
                double failureProbability = response.getPredictionInfo().getFailureProbability();
                boolean predictedFault = failureProbability > testCase.getThreshold();

                // 更新测试结果
                testCase.setDetectedAnomaly(predictedFault);
                testCase.setCorrect(predictedFault == testCase.isExpectedAnomaly());
                testCase.setResponse(response);
                testCase.setActualValue(failureProbability);

            } catch (Exception e) {
                testCase.setError(e.getMessage());
                testCase.setCorrect(false);
            }
        }
    }

    private void executeOverallAnalysisTest(ModelAccuracyResult result) {
        for (ModelAccuracyResult.TestCase testCase : result.getTestCases()) {
            try {
                // 保存测试数据
                pumpDataRepository.saveAll(testCase.getData());

                // 创建综合分析请求
                PumpAnalysisRequestDTO request = createAnalysisRequest();
                request.setAnalysisDepth(PumpAnalysisRequestDTO.AnalysisDepth.COMPREHENSIVE);

                // 执行分析
                PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);

                // 判断综合结果
                boolean hasIssues = response.getOverallHealthScore() < 70 ||
                                  !response.getRiskLevel().equals("LOW");

                // 更新测试结果
                testCase.setDetectedAnomaly(hasIssues);
                testCase.setCorrect(hasIssues == testCase.isExpectedAnomaly());
                testCase.setResponse(response);
                testCase.setActualValue((double) response.getOverallHealthScore());

            } catch (Exception e) {
                testCase.setError(e.getMessage());
                testCase.setCorrect(false);
            }
        }
    }

    // 辅助方法
    private boolean hasAnomalyDetected(PumpAnalysisResponseDTO response, String analysisType) {
        return response.getAnalysisResults().stream()
                .anyMatch(result ->
                    result.getAnalysisType().contains(analysisType) && result.getSeverityLevel() > 1);
    }

    private List<PumpAnalysisRequestDTO.AnalysisType> determineAnalysisTypes(String modelName) {
        if (modelName.contains("启泵频率")) {
            return List.of(PumpAnalysisRequestDTO.AnalysisType.STARTUP_FREQUENCY);
        } else if (modelName.contains("运行时间")) {
            return List.of(PumpAnalysisRequestDTO.AnalysisType.RUNTIME_ANALYSIS);
        } else if (modelName.contains("能耗")) {
            return List.of(PumpAnalysisRequestDTO.AnalysisType.ENERGY_TREND);
        } else if (modelName.contains("振动")) {
            return List.of(PumpAnalysisRequestDTO.AnalysisType.ANOMALY_CLASSIFICATION); // 振动包含在异常分类中
        } else {
            return List.of(PumpAnalysisRequestDTO.AnalysisType.ANOMALY_CLASSIFICATION);
        }
    }

    private PumpAnalysisRequestDTO createAnalysisRequest() {
        PumpAnalysisRequestDTO request = new PumpAnalysisRequestDTO();
        request.setDeviceId(TEST_DEVICE_ID);
        request.setStartTime(testStartTime);
        request.setEndTime(testEndTime);
        request.setAnalysisDepth(PumpAnalysisRequestDTO.AnalysisDepth.STANDARD);
        request.setEnableCache(false);
        return request;
    }

    // 数据生成方法（简化版本，实际实现中需要更复杂的数据生成逻辑）
    private List<PumpDataEntity> generateNormalStartupFrequencyData() {
        return generatePumpDataWithPattern(24, 2.0, false); // 每2小时启动一次
    }

    private List<PumpDataEntity> generateHighStartupFrequencyData() {
        return generatePumpDataWithPattern(24, 0.5, true); // 每30分钟启动一次
    }

    private List<PumpDataEntity> generateLowStartupFrequencyData() {
        return generatePumpDataWithPattern(24, 8.0, true); // 每8小时启动一次
    }

    private List<PumpDataEntity> generateNormalRuntimeData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(24, 2.0, false);
        data.forEach(entity -> {
            if (entity.getPumpStatus() == 1) {
                entity.setRuntimeMinutes(30 + ThreadLocalRandom.current().nextDouble(20));
            }
        });
        return data;
    }

    private List<PumpDataEntity> generateLongRuntimeData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(24, 2.0, true);
        data.forEach(entity -> {
            if (entity.getPumpStatus() == 1) {
                entity.setRuntimeMinutes(500 + ThreadLocalRandom.current().nextDouble(100));
            }
        });
        return data;
    }

    private List<PumpDataEntity> generateShortRuntimeData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(24, 1.0, true);
        data.forEach(entity -> {
            if (entity.getPumpStatus() == 1) {
                entity.setRuntimeMinutes(5 + ThreadLocalRandom.current().nextDouble(5));
            }
        });
        return data;
    }

    private List<PumpDataEntity> generateStableEnergyData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(24, 2.0, false);
        data.forEach(entity -> {
            entity.setEnergyConsumptionKwh(10 + ThreadLocalRandom.current().nextGaussian() * 1);
        });
        return data;
    }

    private List<PumpDataEntity> generateIncreasingEnergyData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(24, 2.0, true);
        for (int i = 0; i < data.size(); i++) {
            data.get(i).setEnergyConsumptionKwh(10 + i * 0.5 + ThreadLocalRandom.current().nextDouble(2));
        }
        return data;
    }

    private List<PumpDataEntity> generateDecreasingEnergyData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(24, 2.0, true);
        for (int i = 0; i < data.size(); i++) {
            data.get(i).setEnergyConsumptionKwh(20 - i * 0.3 + ThreadLocalRandom.current().nextDouble(2));
        }
        return data;
    }

    private List<PumpDataEntity> generateFluctuatingEnergyData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(24, 2.0, true);
        data.forEach(entity -> {
            entity.setEnergyConsumptionKwh(10 + ThreadLocalRandom.current().nextDouble(10));
        });
        return data;
    }

    private List<PumpDataEntity> generateNormalVibrationData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(24, 2.0, false);
        data.forEach(entity -> {
            entity.setVibrationMmS(2.0 + ThreadLocalRandom.current().nextGaussian() * 0.5);
        });
        return data;
    }

    private List<PumpDataEntity> generateHighVibrationData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(24, 2.0, true);
        data.forEach(entity -> {
            entity.setVibrationMmS(6.0 + ThreadLocalRandom.current().nextDouble(3));
        });
        return data;
    }

    private List<PumpDataEntity> generateIntermittentVibrationData() {
        List<PumpDataEntity> data = generateNormalVibrationData();
        // 随机插入高振动值
        for (int i = 5; i < data.size(); i += 8) {
            data.get(i).setVibrationMmS(8.0 + ThreadLocalRandom.current().nextDouble(2));
        }
        return data;
    }

    private List<PumpDataEntity> generateIncreasingVibrationData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(24, 2.0, true);
        for (int i = 0; i < data.size(); i++) {
            data.get(i).setVibrationMmS(2.0 + i * 0.1 + ThreadLocalRandom.current().nextDouble(0.5));
        }
        return data;
    }

    private List<PumpDataEntity> generateHealthyDeviceData() {
        return generatePumpDataWithPattern(48, 3.0, false);
    }

    private List<PumpDataEntity> generateEarlyFaultData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(48, 3.0, true);
        data.forEach(entity -> {
            entity.setVibrationMmS(3.5 + ThreadLocalRandom.current().nextDouble(1));
            entity.setPowerKw(16.0 + ThreadLocalRandom.current().nextDouble(2));
            entity.setCurrentAmperage(32.0 + ThreadLocalRandom.current().nextDouble(3));
        });
        return data;
    }

    private List<PumpDataEntity> generateSevereFaultData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(48, 2.0, true);
        data.forEach(entity -> {
            entity.setVibrationMmS(7.0 + ThreadLocalRandom.current().nextDouble(3));
            entity.setPowerKw(20.0 + ThreadLocalRandom.current().nextDouble(4));
            entity.setCurrentAmperage(40.0 + ThreadLocalRandom.current().nextDouble(5));
            entity.setWaterTemperatureCelsius(70 + ThreadLocalRandom.current().nextDouble(15));
            if (ThreadLocalRandom.current().nextDouble() > 0.7) {
                entity.setFaultCode("VIB_001");
                entity.setAlarmLevel(2);
            }
        });
        return data;
    }

    private List<PumpDataEntity> generateOverallNormalData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(72, 4.0, false);
        data.forEach(entity -> {
            if (entity.getPumpStatus() == 1) {
                entity.setRuntimeMinutes(45 + ThreadLocalRandom.current().nextDouble(15));
                entity.setPowerKw(15 + ThreadLocalRandom.current().nextDouble(2));
                entity.setVibrationMmS(2.5 + ThreadLocalRandom.current().nextGaussian() * 0.5);
                entity.setEnergyConsumptionKwh(12 + ThreadLocalRandom.current().nextGaussian() * 2);
            }
        });
        return data;
    }

    private List<PumpDataEntity> generateOverallAnomalyData() {
        List<PumpDataEntity> data = generatePumpDataWithPattern(72, 1.0, true);
        data.forEach(entity -> {
            if (entity.getPumpStatus() == 1) {
                entity.setRuntimeMinutes(300 + ThreadLocalRandom.current().nextDouble(200));
                entity.setPowerKw(18 + ThreadLocalRandom.current().nextDouble(4));
                entity.setVibrationMmS(5 + ThreadLocalRandom.current().nextDouble(3));
                entity.setEnergyConsumptionKwh(15 + ThreadLocalRandom.current().nextDouble(5));
                entity.setWaterTemperatureCelsius(65 + ThreadLocalRandom.current().nextDouble(10));
            }
        });
        return data;
    }

    private List<PumpDataEntity> generatePumpDataWithPattern(int hours, double intervalHours, boolean hasAnomaly) {
        List<PumpDataEntity> data = new ArrayList<>();
        LocalDateTime currentTime = testStartTime;

        for (int i = 0; i < hours; i++) {
            for (int j = 0; j < (int)(60 / intervalHours); j++) {
                PumpDataEntity entity = new PumpDataEntity();
                entity.setDeviceId(TEST_DEVICE_ID);
                entity.setTimestamp(currentTime.plusMinutes((int)(j * intervalHours * 60)));
                entity.setPumpStatus(j % 4 == 0 ? 1 : 0);

                if (entity.getPumpStatus() == 1) {
                    entity.setRuntimeMinutes(45 + ThreadLocalRandom.current().nextDouble(20));
                    entity.setPowerKw(15 + ThreadLocalRandom.current().nextDouble(3));
                    entity.setEnergyConsumptionKwh(12 + ThreadLocalRandom.current().nextDouble(3));
                    entity.setCurrentAmperage(30 + ThreadLocalRandom.current().nextDouble(5));
                    entity.setVoltage(380 + ThreadLocalRandom.current().nextGaussian() * 10);
                    entity.setWaterPressureKpa(200 + ThreadLocalRandom.current().nextDouble(50));
                    entity.setFlowRateM3h(50 + ThreadLocalRandom.current().nextDouble(10));
                    entity.setWaterTemperatureCelsius(25 + ThreadLocalRandom.current().nextDouble(10));
                    entity.setVibrationMmS(2.5 + ThreadLocalRandom.current().nextGaussian() * 0.8);
                    entity.setNoiseLevelDb(60 + ThreadLocalRandom.current().nextDouble(10));
                }

                data.add(entity);
            }
            currentTime = currentTime.plusHours(1);
        }

        return data;
    }

    // 结果统计类
    private static class ModelAccuracyResult {
        private final String modelName;
        private final List<TestCase> testCases = new ArrayList<>();

        public ModelAccuracyResult(String modelName) {
            this.modelName = modelName;
        }

        public void addTestCase(String name, List<PumpDataEntity> data, boolean expectedAnomaly) {
            testCases.add(new TestCase(name, data, expectedAnomaly));
        }

        public void addTestCase(String name, List<PumpDataEntity> data, boolean expectedAnomaly, double threshold) {
            TestCase testCase = new TestCase(name, data, expectedAnomaly);
            testCase.setThreshold(threshold);
            testCases.add(testCase);
        }

        public double getOverallAccuracy() {
            if (testCases.isEmpty()) return 0.0;
            long correct = testCases.stream().mapToLong(tc -> tc.isCorrect() ? 1 : 0).sum();
            return (double) correct / testCases.size();
        }

        public String generateReport() {
            StringBuilder report = new StringBuilder();
            report.append(String.format("\n【%s 准确率验证结果】\n", modelName));
            report.append("-".repeat(60) + "\n");

            for (TestCase testCase : testCases) {
                report.append(String.format("测试案例: %s\n", testCase.getName()));
                report.append(String.format("  预期异常: %s, 检测异常: %s, 结果: %s\n",
                    testCase.isExpectedAnomaly(), testCase.isDetectedAnomaly(),
                    testCase.isCorrect() ? "✓ 正确" : "✗ 错误"));
                if (testCase.getError() != null) {
                    report.append(String.format("  错误: %s\n", testCase.getError()));
                }
                if (testCase.getActualValue() != null) {
                    report.append(String.format("  实际值: %.3f\n", testCase.getActualValue()));
                }
                if (testCase.getResponse() != null) {
                    report.append(String.format("  置信度: %.2f, 处理时间: %dms\n",
                        testCase.getResponse().getConfidenceScore(),
                        testCase.getResponse().getProcessingTimeMs()));
                }
                report.append("\n");
            }

            report.append(String.format("总体准确率: %.2f%% (%d/%d)\n",
                getOverallAccuracy() * 100,
                (int)(getOverallAccuracy() * testCases.size()),
                testCases.size()));

            return report.toString();
        }

        public String getModelName() { return modelName; }
        public List<TestCase> getTestCases() { return testCases; }

        public static class TestCase {
            private final String name;
            private final List<PumpDataEntity> data;
            private final boolean expectedAnomaly;
            private boolean detectedAnomaly = false;
            private boolean correct = false;
            private String error;
            private PumpAnalysisResponseDTO response;
            private Double actualValue;
            private double threshold = 0.5;

            public TestCase(String name, List<PumpDataEntity> data, boolean expectedAnomaly) {
                this.name = name;
                this.data = data;
                this.expectedAnomaly = expectedAnomaly;
            }

            // Getters and Setters
            public String getName() { return name; }
            public List<PumpDataEntity> getData() { return data; }
            public boolean isExpectedAnomaly() { return expectedAnomaly; }
            public boolean isDetectedAnomaly() { return detectedAnomaly; }
            public void setDetectedAnomaly(boolean detectedAnomaly) { this.detectedAnomaly = detectedAnomaly; }
            public boolean isCorrect() { return correct; }
            public void setCorrect(boolean correct) { this.correct = correct; }
            public String getError() { return error; }
            public void setError(String error) { this.error = error; }
            public PumpAnalysisResponseDTO getResponse() { return response; }
            public void setResponse(PumpAnalysisResponseDTO response) { this.response = response; }
            public Double getActualValue() { return actualValue; }
            public void setActualValue(Double actualValue) { this.actualValue = actualValue; }
            public double getThreshold() { return threshold; }
            public void setThreshold(double threshold) { this.threshold = threshold; }
        }
    }
}