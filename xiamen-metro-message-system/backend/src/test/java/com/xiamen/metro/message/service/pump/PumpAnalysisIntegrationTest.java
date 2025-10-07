package com.xiamen.metro.message.service.pump;

import com.xiamen.metro.message.dto.pump.PumpAnalysisRequestDTO;
import com.xiamen.metro.message.dto.pump.PumpAnalysisResponseDTO;
import com.xiamen.metro.message.entity.PumpDataEntity;
import com.xiamen.metro.message.repository.PumpDataRepository;
import com.xiamen.metro.message.repository.PumpAnalysisResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 水泵分析集成测试
 *
 * @author Xiamen Metro System
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PumpAnalysisIntegrationTest {

    @Autowired
    private PumpIntelligentAnalysisService intelligentAnalysisService;

    @Autowired
    private PumpAnomalyDetectionService anomalyDetectionService;

    @Autowired
    private PumpFaultPredictionService faultPredictionService;

    @Autowired
    private PumpPerformanceEvaluationService performanceEvaluationService;

    @Autowired
    private PumpMaintenanceRecommendationService maintenanceRecommendationService;

    @Autowired
    private PumpDataParsingService parsingService;

    @Autowired
    private PumpDataRepository pumpDataRepository;

    @Autowired
    private PumpAnalysisResultRepository analysisResultRepository;

    private final String TEST_DEVICE_ID = "TEST_PUMP_001";
    private LocalDateTime testStartTime;
    private LocalDateTime testEndTime;

    @BeforeEach
    void setUp() {
        testStartTime = LocalDateTime.now().minusDays(7);
        testEndTime = LocalDateTime.now();

        // 清理测试数据
        pumpDataRepository.deleteAll();
        analysisResultRepository.deleteAll();
    }

    @Test
    @DisplayName("测试完整的水泵智能分析流程")
    void testCompleteIntelligentAnalysis() {
        // 准备测试数据
        List<PumpDataEntity> testData = createTestPumpData();
        pumpDataRepository.saveAll(testData);

        // 创建分析请求
        PumpAnalysisRequestDTO request = createAnalysisRequest();

        // 执行分析
        PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);

        // 验证响应
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getAnalysisId());
        assertEquals(TEST_DEVICE_ID, response.getDeviceId());
        assertNotNull(response.getAnalysisTime());
        assertTrue(response.getProcessingTimeMs() > 0);

        // 验证分析结果
        assertNotNull(response.getAnalysisResults());
        assertFalse(response.getAnalysisResults().isEmpty());

        // 验证性能指标
        assertNotNull(response.getPerformanceMetrics());
        assertTrue(response.getOverallHealthScore() >= 0 && response.getOverallHealthScore() <= 100);

        // 验证预测信息
        assertNotNull(response.getPredictionInfo());
        assertTrue(response.getPredictionInfo().getFailureProbability() >= 0 && response.getPredictionInfo().getFailureProbability() <= 1);

        // 验证维护建议
        assertNotNull(response.getMaintenanceRecommendations());
        assertNotNull(response.getMaintenanceRecommendations().getUrgentActions());
        assertNotNull(response.getMaintenanceRecommendations().getScheduledActions());
        assertNotNull(response.getMaintenanceRecommendations().getPreventiveActions());

        // 验证风险等级
        assertNotNull(response.getRiskLevel());
        assertTrue(Arrays.asList("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(response.getRiskLevel()));
    }

    @Test
    @DisplayName("测试启泵频率异常检测")
    void testStartupFrequencyAnomalyDetection() {
        // 创建频繁启泵的测试数据
        List<PumpDataEntity> frequentStartData = createFrequentStartupData();
        pumpDataRepository.saveAll(frequentStartData);

        // 执行分析
        PumpAnalysisRequestDTO request = createAnalysisRequest();
        request.setAnalysisTypes(List.of(PumpAnalysisRequestDTO.AnalysisType.STARTUP_FREQUENCY));

        PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);

        // 验证检测结果
        assertNotNull(response);
        assertFalse(response.getAnalysisResults().isEmpty());

        PumpAnalysisResponseDTO.AnalysisResult result = response.getAnalysisResults().get(0);
        assertEquals("启泵频率异常检测", result.getAnalysisType());
        assertNotNull(result.getDetectedValue());
        assertNotNull(result.getExpectedValue());
        assertTrue(result.getConfidence() > 0);
    }

    @Test
    @DisplayName("测试运行时间异常分析")
    void testRuntimeAnomalyAnalysis() {
        // 创建运行时间异常的测试数据
        List<PumpDataEntity> runtimeAnomalyData = createRuntimeAnomalyData();
        pumpDataRepository.saveAll(runtimeAnomalyData);

        // 执行分析
        PumpAnalysisRequestDTO request = createAnalysisRequest();
        request.setAnalysisTypes(List.of(PumpAnalysisRequestDTO.AnalysisType.RUNTIME_ANALYSIS));

        PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);

        // 验证分析结果
        assertNotNull(response);
        assertFalse(response.getAnalysisResults().isEmpty());

        PumpAnalysisResponseDTO.AnalysisResult result = response.getAnalysisResults().get(0);
        assertEquals("运行时间异常分析", result.getAnalysisType());
        assertTrue(result.getSeverityLevel() >= 1);
    }

    @Test
    @DisplayName("测试能耗趋势分析")
    void testEnergyTrendAnalysis() {
        // 创建能耗增长趋势的测试数据
        List<PumpDataEntity> energyTrendData = createEnergyTrendData();
        pumpDataRepository.saveAll(energyTrendData);

        // 执行分析
        PumpAnalysisRequestDTO request = createAnalysisRequest();
        request.setAnalysisTypes(List.of(PumpAnalysisRequestDTO.AnalysisType.ENERGY_TREND));

        PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);

        // 验证分析结果
        assertNotNull(response);
        assertFalse(response.getAnalysisResults().isEmpty());

        PumpAnalysisResponseDTO.AnalysisResult result = response.getAnalysisResults().get(0);
        assertEquals("能耗趋势分析", result.getAnalysisType());
        assertNotNull(result.getTrendDirection());
    }

    @Test
    @DisplayName("测试振动异常检测")
    void testVibrationAnomalyDetection() {
        // 创建振动异常的测试数据
        List<PumpDataEntity> vibrationAnomalyData = createVibrationAnomalyData();
        pumpDataRepository.saveAll(vibrationAnomalyData);

        // 执行分析
        PumpAnalysisRequestDTO request = createAnalysisRequest();
        request.setThresholdConfig(createStrictThresholdConfig());

        PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);

        // 验证检测结果
        assertNotNull(response);
        assertFalse(response.getAnalysisResults().isEmpty());

        // 应该检测到振动异常
        boolean vibrationDetected = response.getAnalysisResults().stream()
                .anyMatch(r -> r.getAnalysisType().contains("振动"));
        assertTrue(vibrationDetected);
    }

    @Test
    @DisplayName("测试故障预测功能")
    void testFaultPrediction() {
        // 创建故障预测测试数据
        List<PumpDataEntity> faultPredictionData = createFaultPredictionData();
        pumpDataRepository.saveAll(faultPredictionData);

        // 执行分析
        PumpAnalysisRequestDTO request = createAnalysisRequest();
        request.setAnalysisTypes(List.of(PumpAnalysisRequestDTO.AnalysisType.FAULT_PREDICTION));
        request.setModelConfig(createAdvancedModelConfig());

        PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);

        // 验证预测结果
        assertNotNull(response);
        assertNotNull(response.getPredictionInfo());
        assertTrue(response.getPredictionInfo().getFailureProbability() >= 0);
        assertTrue(response.getPredictionInfo().getRemainingUsefulLifeDays() >= 0);
        assertNotNull(response.getPredictionInfo().getPredictedFailureTime());
    }

    @Test
    @DisplayName("测试性能评估功能")
    void testPerformanceEvaluation() {
        // 创建性能评估测试数据
        List<PumpDataEntity> performanceData = createPerformanceTestData();
        pumpDataRepository.saveAll(performanceData);

        // 执行分析
        PumpAnalysisRequestDTO request = createAnalysisRequest();
        request.setAnalysisTypes(List.of(PumpAnalysisRequestDTO.AnalysisType.PERFORMANCE_EVALUATION));

        PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);

        // 验证性能指标
        assertNotNull(response);
        assertNotNull(response.getPerformanceMetrics());
        assertTrue(response.getPerformanceMetrics().getEfficiencyScore() >= 0);
        assertTrue(response.getPerformanceMetrics().getReliabilityScore() >= 0);
        assertTrue(response.getPerformanceMetrics().getMaintenanceScore() >= 0);
    }

    @Test
    @DisplayName("测试批量分析功能")
    void testBatchAnalysis() {
        // 创建多个设备的测试数据
        List<PumpDataEntity> device1Data = createTestPumpData("DEVICE_001");
        List<PumpDataEntity> device2Data = createTestPumpData("DEVICE_002");
        pumpDataRepository.saveAll(device1Data);
        pumpDataRepository.saveAll(device2Data);

        // 创建批量分析请求
        List<PumpAnalysisRequestDTO> requests = Arrays.asList(
                createAnalysisRequest("DEVICE_001"),
                createAnalysisRequest("DEVICE_002")
        );

        // 执行批量分析
        List<PumpAnalysisResponseDTO> responses = intelligentAnalysisService.performBatchAnalysis(requests);

        // 验证结果
        assertNotNull(responses);
        assertEquals(2, responses.size());

        for (PumpAnalysisResponseDTO response : responses) {
            assertNotNull(response);
            assertEquals("SUCCESS", response.getStatus());
            assertFalse(response.getAnalysisResults().isEmpty());
        }
    }

    @Test
    @DisplayName("测试维护建议生成")
    void testMaintenanceRecommendationGeneration() {
        // 创建需要维护的测试数据
        List<PumpDataEntity> maintenanceData = createMaintenanceRequiredData();
        pumpDataRepository.saveAll(maintenanceData);

        // 执行分析
        PumpAnalysisRequestDTO request = createAnalysisRequest();
        PumpAnalysisResponseDTO response = intelligentAnalysisService.performIntelligentAnalysis(request);

        // 验证维护建议
        assertNotNull(response);
        assertNotNull(response.getMaintenanceRecommendations());

        // 应该生成一些建议
        assertTrue(!response.getMaintenanceRecommendations().getUrgentActions().isEmpty() ||
                   !response.getMaintenanceRecommendations().getScheduledActions().isEmpty() ||
                   !response.getMaintenanceRecommendations().getPreventiveActions().isEmpty());

        // 验证成本估算
        if (response.getMaintenanceRecommendations().getEstimatedCost() != null) {
            assertTrue(response.getMaintenanceRecommendations().getEstimatedCost() >= 0);
        }
    }

    @Test
    @DisplayName("测试时间序列分析工具")
    void testTimeSeriesAnalyzer() {
        // 创建测试数据
        List<Double> data = Arrays.asList(10.0, 12.0, 11.5, 13.0, 14.5, 13.5, 15.0, 16.0, 15.5, 17.0);

        // 测试移动平均
        List<Double> movingAvg = TimeSeriesAnalyzer.movingAverage(data, 3);
        assertNotNull(movingAvg);
        assertEquals(data.size(), movingAvg.size());

        // 测试标准差
        double stdDev = TimeSeriesAnalyzer.standardDeviation(data);
        assertTrue(stdDev > 0);

        // 测试趋势分析
        TimeSeriesAnalyzer.TrendAnalysisResult trend = TimeSeriesAnalyzer.analyzeTrend(data);
        assertNotNull(trend);
        assertNotNull(trend.getDirection());

        // 测试异常值检测
        List<Integer> outliers = TimeSeriesAnalyzer.detectOutliers(Arrays.asList(10.0, 12.0, 11.0, 50.0, 13.0));
        assertTrue(outliers.contains(3)); // 索引3的值50.0应该被检测为异常
    }

    // 辅助方法：创建测试数据
    private List<PumpDataEntity> createTestPumpData() {
        return createTestPumpData(TEST_DEVICE_ID);
    }

    private List<PumpDataEntity> createTestPumpData(String deviceId) {
        List<PumpDataEntity> data = new ArrayList<>();
        LocalDateTime baseTime = testStartTime;

        for (int i = 0; i < 100; i++) {
            PumpDataEntity entity = new PumpDataEntity();
            entity.setDeviceId(deviceId);
            entity.setTimestamp(baseTime.plusHours(i));
            entity.setPumpStatus(i % 4 == 0 ? 1 : 0); // 每4小时启泵一次
            entity.setRuntimeMinutes(i % 4 == 0 ? 45.0 + Math.random() * 10 : 0.0);
            entity.setPowerKw(i % 4 == 0 ? 15.0 + Math.random() * 2 : 0.0);
            entity.setEnergyConsumptionKwh(i % 4 == 0 ? 12.0 + Math.random() * 3 : 0.0);
            entity.setVibrationMmS(2.0 + Math.random() * 1.5);
            entity.setWaterPressureKpa(200.0 + Math.random() * 50);
            entity.setFlowRateM3h(50.0 + Math.random() * 10);
            entity.setCurrentAmperage(30.0 + Math.random() * 5);
            entity.setVoltage(380.0 + Math.random() * 20);
            entity.setWaterTemperatureCelsius(25.0 + Math.random() * 10);
            data.add(entity);
        }

        return data;
    }

    private List<PumpDataEntity> createFrequentStartupData() {
        List<PumpDataEntity> data = new ArrayList<>();
        LocalDateTime baseTime = testStartTime;

        for (int i = 0; i < 200; i++) {
            PumpDataEntity entity = new PumpDataEntity();
            entity.setDeviceId(TEST_DEVICE_ID);
            entity.setTimestamp(baseTime.plusMinutes(i * 10)); // 每10分钟一次
            entity.setPumpStatus(1); // 持续启动
            entity.setRuntimeMinutes(8.0 + Math.random() * 4);
            data.add(entity);
        }

        return data;
    }

    private List<PumpDataEntity> createRuntimeAnomalyData() {
        List<PumpDataEntity> data = new ArrayList<>();
        LocalDateTime baseTime = testStartTime;

        for (int i = 0; i < 50; i++) {
            PumpDataEntity entity = new PumpDataEntity();
            entity.setDeviceId(TEST_DEVICE_ID);
            entity.setTimestamp(baseTime.plusHours(i * 2));
            entity.setPumpStatus(1);
            entity.setRuntimeMinutes(600.0 + Math.random() * 120); // 运行时间过长
            entity.setPowerKw(18.0 + Math.random() * 4);
            data.add(entity);
        }

        return data;
    }

    private List<PumpDataEntity> createEnergyTrendData() {
        List<PumpDataEntity> data = new ArrayList<>();
        LocalDateTime baseTime = testStartTime;

        for (int i = 0; i < 80; i++) {
            PumpDataEntity entity = new PumpDataEntity();
            entity.setDeviceId(TEST_DEVICE_ID);
            entity.setTimestamp(baseTime.plusHours(i * 3));
            entity.setPumpStatus(1);
            entity.setEnergyConsumptionKwh(10.0 + i * 0.2 + Math.random() * 2); // 能耗持续增长
            entity.setPowerKw(15.0 + i * 0.1 + Math.random() * 2);
            data.add(entity);
        }

        return data;
    }

    private List<PumpDataEntity> createVibrationAnomalyData() {
        List<PumpDataEntity> data = createTestPumpData();

        // 添加一些振动异常值
        for (int i = 10; i < data.size(); i += 15) {
            data.get(i).setVibrationMmS(8.0 + Math.random() * 3); // 高振动值
        }

        return data;
    }

    private List<PumpDataEntity> createFaultPredictionData() {
        List<PumpDataEntity> data = new ArrayList<>();
        LocalDateTime baseTime = testStartTime;

        for (int i = 0; i < 120; i++) {
            PumpDataEntity entity = new PumpDataEntity();
            entity.setDeviceId(TEST_DEVICE_ID);
            entity.setTimestamp(baseTime.plusHours(i * 2));
            entity.setPumpStatus(1);

            // 模拟性能退化
            double degradationFactor = 1.0 + (i / 120.0) * 0.5;
            entity.setPowerKw((15.0 * degradationFactor) + Math.random() * 2);
            entity.setVibrationMmS((2.0 * degradationFactor) + Math.random() * 1.5);
            entity.setCurrentAmperage((30.0 * degradationFactor) + Math.random() * 5);
            entity.setWaterTemperatureCelsius(25.0 + (i / 120.0) * 20 + Math.random() * 5);

            if (i > 100) {
                entity.setFaultCode("VIB_001"); // 添加故障代码
                entity.setAlarmLevel(2);
            }

            data.add(entity);
        }

        return data;
    }

    private List<PumpDataEntity> createPerformanceTestData() {
        List<PumpDataEntity> data = new ArrayList<>();
        LocalDateTime baseTime = testStartTime;

        for (int i = 0; i < 60; i++) {
            PumpDataEntity entity = new PumpDataEntity();
            entity.setDeviceId(TEST_DEVICE_ID);
            entity.setTimestamp(baseTime.plusHours(i * 4));
            entity.setPumpStatus(1);
            entity.setRuntimeMinutes(40.0 + Math.random() * 20);
            entity.setPowerKw(14.0 + Math.random() * 4);
            entity.setEnergyConsumptionKwh(10.0 + Math.random() * 5);
            entity.setWaterPressureKpa(220.0 + Math.random() * 30);
            entity.setFlowRateM3h(55.0 + Math.random() * 15);
            entity.setVibrationMmS(2.5 + Math.random() * 1.0);
            data.add(entity);
        }

        return data;
    }

    private List<PumpDataEntity> createMaintenanceRequiredData() {
        List<PumpDataEntity> data = createVibrationAnomalyData();

        // 添加维护标志
        data.forEach(entity -> {
            if (entity.getVibrationMmS() > 5.0) {
                entity.setMaintenanceFlag(true);
            }
        });

        return data;
    }

    private PumpAnalysisRequestDTO createAnalysisRequest() {
        return createAnalysisRequest(TEST_DEVICE_ID);
    }

    private PumpAnalysisRequestDTO createAnalysisRequest(String deviceId) {
        PumpAnalysisRequestDTO request = new PumpAnalysisRequestDTO();
        request.setDeviceId(deviceId);
        request.setStartTime(testStartTime);
        request.setEndTime(testEndTime);
        request.setAnalysisTypes(Arrays.asList(PumpAnalysisRequestDTO.AnalysisType.values()));
        request.setAnalysisDepth(PumpAnalysisRequestDTO.AnalysisDepth.STANDARD);
        request.setEnableCache(false);
        request.setThresholdConfig(createDefaultThresholdConfig());
        request.setModelConfig(createDefaultModelConfig());
        return request;
    }

    private PumpAnalysisRequestDTO.ThresholdConfig createDefaultThresholdConfig() {
        PumpAnalysisRequestDTO.ThresholdConfig config = new PumpAnalysisRequestDTO.ThresholdConfig();
        config.setStartupFrequencyThreshold(10.0);
        config.setRuntimeThreshold(480.0);
        config.setPowerAnomalyThreshold(20.0);
        config.setVibrationThreshold(4.5);
        config.setEnergyIncreaseThreshold(15.0);
        config.setTemperatureThreshold(60.0);
        config.setPressureThreshold(100.0);
        return config;
    }

    private PumpAnalysisRequestDTO.ThresholdConfig createStrictThresholdConfig() {
        PumpAnalysisRequestDTO.ThresholdConfig config = createDefaultThresholdConfig();
        config.setVibrationThreshold(3.5); // 更严格的振动阈值
        return config;
    }

    private PumpAnalysisRequestDTO.ModelConfig createDefaultModelConfig() {
        PumpAnalysisRequestDTO.ModelConfig config = new PumpAnalysisRequestDTO.ModelConfig();
        config.setModelVersion("1.0");
        config.setPredictionWindowDays(7);
        config.setConfidenceThreshold(0.7);
        config.setTrainingDataDays(30);

        PumpAnalysisRequestDTO.ModelConfig.FeatureEngineering features = new PumpAnalysisRequestDTO.ModelConfig.FeatureEngineering();
        features.setUseTimeFeatures(true);
        features.setUseStatisticalFeatures(true);
        features.setUseFrequencyFeatures(false);
        features.setSlidingWindowSize(24);

        config.setFeatureEngineering(features);
        return config;
    }

    private PumpAnalysisRequestDTO.ModelConfig createAdvancedModelConfig() {
        PumpAnalysisRequestDTO.ModelConfig config = createDefaultModelConfig();
        config.setPredictionWindowDays(14);
        config.setTrainingDataDays(60);
        config.getFeatureEngineering().setUseFrequencyFeatures(true);
        config.getFeatureEngineering().setSlidingWindowSize(48);
        return config;
    }
}