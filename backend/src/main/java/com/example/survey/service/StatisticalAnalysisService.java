package com.example.survey.service;

import com.example.survey.dto.*;
import com.example.survey.model.MenuItem;
import com.example.survey.model.Option;
import com.example.survey.model.Question;
import com.example.survey.repository.AnswerRepository;
import com.example.survey.repository.MenuItemRepository;
import com.example.survey.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.OneWayAnova;
import org.apache.commons.math3.stat.inference.TTest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticalAnalysisService {

    private static final Set<String> MEALS_CATEGORIES = Set.of(
            "pasta", "waffle", "meal", "meals"
    );

    public record RatingRecord(
            String userId,
            Long menuItemId,
            String menuItemName,
            String subcategory,
            String supercategory,
            String dimensionKey,
            String dimensionOriginal,
            int rating
    ) {}

    public record DbDimension(
            String key,
            String label,
            String icon,
            String sub,
            Long questionId,
            String questionText
    ) {}

    private final AnswerRepository answerRepository;
    private final MenuItemRepository menuItemRepository;
    private final QuestionRepository questionRepository;
    private final TTest tTest = new TTest();
    private final OneWayAnova anova = new OneWayAnova();

    /**
     * Executes an Independent Two-Sample T-Test comparing two groups.
     */
    public TTestResultDTO runTTest(TTestRequestDTO request) {
        String mode = (request.getMode() == null ? "ITEMS" : request.getMode()).toUpperCase().trim();
        String group1 = request.getGroup1() == null ? "" : request.getGroup1().trim();
        String group2 = request.getGroup2() == null ? "" : request.getGroup2().trim();
        String dimension = request.getDimension() == null ? "all" : request.getDimension().trim();
        double alpha = (request.getAlpha() == null || request.getAlpha() <= 0 || request.getAlpha() >= 1) ? 0.05 : request.getAlpha();

        List<RatingRecord> allRatings = loadAllRatingRecords();
        Map<String, String> userAge = loadUserDemographics("age");
        Map<String, String> userDining = loadUserDemographics("dine");

        List<Double> sample1List = new ArrayList<>();
        List<Double> sample2List = new ArrayList<>();
        String label1 = group1;
        String label2 = group2;

        switch (mode) {
            case "SUPER_CATEGORIES" -> {
                label1 = "Meals";
                label2 = "Beverages";
                for (RatingRecord r : allRatings) {
                    if (!matchesDimension(r, dimension)) continue;
                    if ("Meals".equalsIgnoreCase(r.supercategory())) {
                        sample1List.add((double) r.rating());
                    } else if ("Beverages".equalsIgnoreCase(r.supercategory())) {
                        sample2List.add((double) r.rating());
                    }
                }
            }
            case "SUBCATEGORIES" -> {
                for (RatingRecord r : allRatings) {
                    if (!matchesDimension(r, dimension)) continue;
                    if (r.subcategory() != null && r.subcategory().equalsIgnoreCase(group1)) {
                        sample1List.add((double) r.rating());
                    } else if (r.subcategory() != null && r.subcategory().equalsIgnoreCase(group2)) {
                        sample2List.add((double) r.rating());
                    }
                }
            }
            case "AGE_GROUPS" -> {
                for (RatingRecord r : allRatings) {
                    if (!matchesDimension(r, dimension)) continue;
                    String age = userAge.get(r.userId());
                    if (age != null && age.equalsIgnoreCase(group1)) {
                        sample1List.add((double) r.rating());
                    } else if (age != null && age.equalsIgnoreCase(group2)) {
                        sample2List.add((double) r.rating());
                    }
                }
            }
            case "DINING_FREQUENCY" -> {
                for (RatingRecord r : allRatings) {
                    if (!matchesDimension(r, dimension)) continue;
                    String freq = userDining.get(r.userId());
                    if (freq != null && freq.equalsIgnoreCase(group1)) {
                        sample1List.add((double) r.rating());
                    } else if (freq != null && freq.equalsIgnoreCase(group2)) {
                        sample2List.add((double) r.rating());
                    }
                }
            }
            case "DIMENSIONS" -> {
                label1 = capitalize(group1);
                label2 = capitalize(group2);
                for (RatingRecord r : allRatings) {
                    if (matchesDimensionKey(r, group1)) {
                        sample1List.add((double) r.rating());
                    } else if (matchesDimensionKey(r, group2)) {
                        sample2List.add((double) r.rating());
                    }
                }
            }
            case "ITEMS" -> {
                Long id1 = parseId(group1);
                Long id2 = parseId(group2);
                MenuItem m1 = id1 != null ? menuItemRepository.findById(id1).orElse(null) : null;
                MenuItem m2 = id2 != null ? menuItemRepository.findById(id2).orElse(null) : null;
                if (m1 != null) label1 = m1.getName();
                if (m2 != null) label2 = m2.getName();

                for (RatingRecord r : allRatings) {
                    if (!matchesDimension(r, dimension)) continue;
                    if (r.menuItemId() != null && r.menuItemId().equals(id1)) {
                        sample1List.add((double) r.rating());
                    } else if (r.menuItemId() != null && r.menuItemId().equals(id2)) {
                        sample2List.add((double) r.rating());
                    }
                }
            }
            default -> throw new IllegalArgumentException("Unsupported T-Test mode: " + mode);
        }

        double[] s1 = sample1List.stream().mapToDouble(Double::doubleValue).toArray();
        double[] s2 = sample2List.stream().mapToDouble(Double::doubleValue).toArray();

        return computeTTestResult(mode, dimension, label1, label2, s1, s2, alpha);
    }

    /**
     * Executes a One-Way ANOVA across multiple groups.
     */
    public AnovaResultDTO runAnova(AnovaRequestDTO request) {
        String factor = (request.getFactor() == null ? "SUBCATEGORIES" : request.getFactor()).toUpperCase().trim();
        String dimension = request.getDimension() == null ? "all" : request.getDimension().trim();
        Long filterItemId = request.getMenuItemId();
        double alpha = (request.getAlpha() == null || request.getAlpha() <= 0 || request.getAlpha() >= 1) ? 0.05 : request.getAlpha();

        List<RatingRecord> allRatings = loadAllRatingRecords();
        Map<String, String> userAge = loadUserDemographics("age");
        Map<String, String> userDining = loadUserDemographics("dine");

        Map<String, List<Double>> groupedData = new LinkedHashMap<>();

        switch (factor) {
            case "SUBCATEGORIES" -> {
                for (RatingRecord r : allRatings) {
                    if (!matchesDimension(r, dimension)) continue;
                    if (r.subcategory() != null && !r.subcategory().isBlank()) {
                        groupedData.computeIfAbsent(r.subcategory(), k -> new ArrayList<>()).add((double) r.rating());
                    }
                }
            }
            case "SUPER_CATEGORIES" -> {
                for (RatingRecord r : allRatings) {
                    if (!matchesDimension(r, dimension)) continue;
                    String superCat = r.supercategory();
                    if (superCat != null && !superCat.isBlank()) {
                        groupedData.computeIfAbsent(superCat, k -> new ArrayList<>()).add((double) r.rating());
                    }
                }
            }
            case "AGE_GROUPS" -> {
                for (RatingRecord r : allRatings) {
                    if (!matchesDimension(r, dimension)) continue;
                    String age = userAge.get(r.userId());
                    if (age != null && !age.isBlank()) {
                        groupedData.computeIfAbsent(age, k -> new ArrayList<>()).add((double) r.rating());
                    }
                }
            }
            case "DINING_FREQUENCY" -> {
                for (RatingRecord r : allRatings) {
                    if (!matchesDimension(r, dimension)) continue;
                    String freq = userDining.get(r.userId());
                    if (freq != null && !freq.isBlank()) {
                        groupedData.computeIfAbsent(freq, k -> new ArrayList<>()).add((double) r.rating());
                    }
                }
            }
            case "MOOD_DIMENSIONS", "WEATHER_DIMENSIONS" -> {
                List<StatisticalOverviewDTO.EvaluationQuestionDTO> evalQs = loadEvaluationQuestions();
                StatisticalOverviewDTO.EvaluationQuestionDTO targetQ = evalQs.stream()
                        .filter(eq -> eq.getFactorKey().equalsIgnoreCase(factor))
                        .findFirst()
                        .orElse(null);

                List<String> targetGroups;
                if (targetQ != null && targetQ.getDimensionLabels() != null && !targetQ.getDimensionLabels().isEmpty()) {
                    targetGroups = targetQ.getDimensionLabels();
                } else {
                    targetGroups = "MOOD_DIMENSIONS".equals(factor)
                            ? List.of("Relaxation", "Focus", "Celebrate", "Comfort", "Welcoming", "Socialize", "Enjoyment")
                            : List.of("Rainy", "Hot Dry", "Cool Dry");
                }

                for (String g : targetGroups) {
                    groupedData.put(g, new ArrayList<>());
                }

                for (RatingRecord r : allRatings) {
                    if (filterItemId != null && !filterItemId.equals(r.menuItemId())) continue;
                    for (String g : targetGroups) {
                        String gLower = g.toLowerCase();
                        if (r.dimensionKey().equalsIgnoreCase(gLower)
                                || r.dimensionOriginal().toLowerCase().contains(gLower)
                                || gLower.contains(r.dimensionKey())) {
                            groupedData.get(g).add((double) r.rating());
                            break;
                        }
                    }
                }
            }
            case "ALL_DIMENSIONS" -> {
                List<DbDimension> allDims = loadDatabaseEvaluationDimensions();
                for (DbDimension d : allDims) {
                    groupedData.put(d.label(), new ArrayList<>());
                }
                for (RatingRecord r : allRatings) {
                    if (filterItemId != null && !filterItemId.equals(r.menuItemId())) continue;
                    for (DbDimension d : allDims) {
                        if (r.dimensionKey().equalsIgnoreCase(d.key()) || r.dimensionOriginal().toLowerCase().contains(d.key())) {
                            groupedData.get(d.label()).add((double) r.rating());
                            break;
                        }
                    }
                }
            }
            default -> {
                if (factor.startsWith("QUESTION_")) {
                    List<StatisticalOverviewDTO.EvaluationQuestionDTO> evalQs = loadEvaluationQuestions();
                    StatisticalOverviewDTO.EvaluationQuestionDTO targetQ = evalQs.stream()
                            .filter(eq -> eq.getFactorKey().equalsIgnoreCase(factor))
                            .findFirst()
                            .orElse(null);
                    if (targetQ != null && targetQ.getDimensionLabels() != null && !targetQ.getDimensionLabels().isEmpty()) {
                        for (String g : targetQ.getDimensionLabels()) {
                            groupedData.put(g, new ArrayList<>());
                        }
                        for (RatingRecord r : allRatings) {
                            if (filterItemId != null && !filterItemId.equals(r.menuItemId())) continue;
                            for (String g : targetQ.getDimensionLabels()) {
                                String gLower = g.toLowerCase();
                                if (r.dimensionKey().equalsIgnoreCase(gLower)
                                        || r.dimensionOriginal().toLowerCase().contains(gLower)
                                        || gLower.contains(r.dimensionKey())) {
                                    groupedData.get(g).add((double) r.rating());
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
                throw new IllegalArgumentException("Unsupported ANOVA factor: " + factor);
            }
        }

        // Apply group filter if user selected specific groups
        if (request.getSelectedGroups() != null && !request.getSelectedGroups().isEmpty()) {
            Set<String> selected = request.getSelectedGroups().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            groupedData.entrySet().removeIf(entry -> !selected.contains(entry.getKey().toLowerCase()));
        }

        return computeAnovaResult(factor, dimension, groupedData, alpha);
    }

    /**
     * Builds comprehensive overview metadata, lists of options, and ready-to-display test previews.
     */
    public StatisticalOverviewDTO getStatisticalOverview() {
        List<RatingRecord> allRatings = loadAllRatingRecords();
        List<MenuItem> allItems = menuItemRepository.findAll();

        Map<Long, Integer> countByItem = new HashMap<>();
        for (RatingRecord r : allRatings) {
            if (r.menuItemId() != null) {
                countByItem.merge(r.menuItemId(), 1, Integer::sum);
            }
        }

        List<StatisticalOverviewDTO.MenuItemOptionDTO> itemOptions = allItems.stream()
                .map(m -> StatisticalOverviewDTO.MenuItemOptionDTO.builder()
                        .id(m.getId())
                        .name(m.getName())
                        .category(m.getCategory())
                        .ratingCount(countByItem.getOrDefault(m.getId(), 0))
                        .build())
                .sorted((a, b) -> Integer.compare(b.getRatingCount(), a.getRatingCount()))
                .toList();

        List<String> subcategories = allItems.stream()
                .map(MenuItem::getCategory)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(c -> !c.isBlank())
                .distinct()
                .sorted()
                .toList();

        List<String> supercategories = List.of("Meals", "Beverages");

        List<DbDimension> dbDimensions = loadDatabaseEvaluationDimensions();
        List<StatisticalOverviewDTO.DimensionOptionDTO> dimensionOptions = new ArrayList<>();
        List<String> dimensions = new ArrayList<>();
        dimensions.add("All (Overall Suitability)");

        Map<String, Integer> ratingCountByDim = new HashMap<>();
        for (RatingRecord r : allRatings) {
            ratingCountByDim.merge(r.dimensionKey(), 1, Integer::sum);
        }

        for (DbDimension d : dbDimensions) {
            dimensions.add(d.label());
            dimensionOptions.add(StatisticalOverviewDTO.DimensionOptionDTO.builder()
                    .key(d.key())
                    .label(d.label())
                    .icon(d.icon())
                    .sub(d.sub())
                    .questionId(d.questionId())
                    .questionText(d.questionText())
                    .ratingCount(ratingCountByDim.getOrDefault(d.key(), 0))
                    .build());
        }

        List<StatisticalOverviewDTO.EvaluationQuestionDTO> evalQuestions = loadEvaluationQuestions();

        Map<String, String> userAge = loadUserDemographics("age");
        List<String> ageGroups = userAge.values().stream().filter(Objects::nonNull).distinct().sorted().toList();

        Map<String, String> userDining = loadUserDemographics("dine");
        List<String> diningFrequencies = userDining.values().stream().filter(Objects::nonNull).distinct().sorted().toList();

        Set<String> uniqueUsers = allRatings.stream().map(RatingRecord::userId).filter(Objects::nonNull).collect(Collectors.toSet());

        // Compute sample default tests for instant display on tab open
        TTestResultDTO sampleTTest = null;
        try {
            sampleTTest = runTTest(TTestRequestDTO.builder()
                    .mode("SUPER_CATEGORIES")
                    .dimension("all")
                    .alpha(0.05)
                    .build());
        } catch (Exception e) {
            log.warn("Could not generate default sample T-Test: {}", e.getMessage());
        }

        AnovaResultDTO sampleAnova = null;
        try {
            sampleAnova = runAnova(AnovaRequestDTO.builder()
                    .factor("SUBCATEGORIES")
                    .dimension("all")
                    .alpha(0.05)
                    .build());
        } catch (Exception e) {
            log.warn("Could not generate default sample ANOVA: {}", e.getMessage());
        }

        return StatisticalOverviewDTO.builder()
                .items(itemOptions)
                .subcategories(subcategories)
                .supercategories(supercategories)
                .dimensions(dimensions)
                .dimensionOptions(dimensionOptions)
                .evaluationQuestions(evalQuestions)
                .ageGroups(ageGroups)
                .diningFrequencies(diningFrequencies)
                .totalRatingsCount(allRatings.size())
                .totalEvaluatorsCount(uniqueUsers.size())
                .sampleTTest(sampleTTest)
                .sampleAnova(sampleAnova)
                .build();
    }

    // =========================================================================
    // STATISTICAL COMPUTATION ENGINE
    // =========================================================================

    private TTestResultDTO computeTTestResult(
            String mode,
            String dimension,
            String label1,
            String label2,
            double[] s1,
            double[] s2,
            double alpha
    ) {
        int n1 = s1.length;
        int n2 = s2.length;

        DescriptiveStatistics stats1 = new DescriptiveStatistics(s1);
        DescriptiveStatistics stats2 = new DescriptiveStatistics(s2);

        double m1 = n1 > 0 ? stats1.getMean() : 0.0;
        double m2 = n2 > 0 ? stats2.getMean() : 0.0;
        double var1 = n1 > 1 ? stats1.getVariance() : 0.0;
        double var2 = n2 > 1 ? stats2.getVariance() : 0.0;
        double sd1 = Math.sqrt(var1);
        double sd2 = Math.sqrt(var2);

        double diff = m1 - m2;

        if (n1 < 2 || n2 < 2) {
            return TTestResultDTO.builder()
                    .testName("Two-Sample T-Test (Welch's)")
                    .mode(mode)
                    .dimension(dimension)
                    .hypothesis(String.format("H₀: μ(%s) = μ(%s) vs H₁: μ(%s) ≠ μ(%s)", label1, label2, label1, label2))
                    .group1Label(label1)
                    .group2Label(label2)
                    .n1(n1)
                    .n2(n2)
                    .mean1(round(m1, 2))
                    .mean2(round(m2, 2))
                    .sd1(round(sd1, 2))
                    .sd2(round(sd2, 2))
                    .meanDifference(round(diff, 2))
                    .standardError(0.0)
                    .tStatistic(0.0)
                    .degreesOfFreedom(0.0)
                    .pValue(1.0)
                    .alpha(alpha)
                    .isSignificant(false)
                    .cohensD(0.0)
                    .effectSizeLabel("N/A")
                    .conclusion("Insufficient sample size (minimum 2 observations per group required).")
                    .build();
        }

        // Standard error of difference (Welch)
        double se = Math.sqrt((var1 / n1) + (var2 / n2));

        // Degrees of freedom (Welch–Satterthwaite)
        double df;
        if (se == 0.0) {
            df = n1 + n2 - 2.0;
        } else {
            double num = Math.pow((var1 / n1) + (var2 / n2), 2);
            double denom = (Math.pow(var1 / n1, 2) / (n1 - 1)) + (Math.pow(var2 / n2, 2) / (n2 - 1));
            df = denom > 0 ? (num / denom) : (n1 + n2 - 2.0);
        }
        df = Math.max(1.0, df);

        double tStat = 0.0;
        double pVal = 1.0;

        if (se > 0.0) {
            tStat = diff / se;
            try {
                pVal = tTest.tTest(s1, s2);
            } catch (Exception e) {
                // Fallback to manual Student's T distribution CDF
                try {
                    TDistribution dist = new TDistribution(df);
                    pVal = 2.0 * (1.0 - dist.cumulativeProbability(Math.abs(tStat)));
                } catch (Exception ex) {
                    pVal = 1.0;
                }
            }
        } else {
            // Both groups have zero variance
            tStat = diff == 0.0 ? 0.0 : (diff > 0 ? 999.0 : -999.0);
            pVal = diff == 0.0 ? 1.0 : 0.0;
        }

        pVal = Math.clamp(pVal, 0.0, 1.0);

        // Confidence interval
        double tCrit = 1.96;
        try {
            TDistribution dist = new TDistribution(df);
            tCrit = dist.inverseCumulativeProbability(1.0 - (alpha / 2.0));
        } catch (Exception ignored) {}

        double ciLower = diff - (tCrit * se);
        double ciUpper = diff + (tCrit * se);

        // Cohen's d (pooled standard deviation)
        double sPooled = Math.sqrt((((n1 - 1) * var1) + ((n2 - 1) * var2)) / Math.max(1, (n1 + n2 - 2)));
        double cohensD = sPooled > 0 ? (diff / sPooled) : 0.0;

        String effectLabel = getCohensDLabel(Math.abs(cohensD));
        boolean isSig = pVal < alpha;

        String conclusion = generateTTestConclusion(label1, label2, m1, m2, tStat, df, pVal, alpha, isSig, effectLabel);

        return TTestResultDTO.builder()
                .testName("Two-Sample T-Test (Welch's Heteroscedastic)")
                .mode(mode)
                .dimension(dimension)
                .hypothesis(String.format("H₀: μ(%s) = μ(%s) vs H₁: μ(%s) ≠ μ(%s)", label1, label2, label1, label2))
                .group1Label(label1)
                .group2Label(label2)
                .n1(n1)
                .n2(n2)
                .mean1(round(m1, 2))
                .mean2(round(m2, 2))
                .sd1(round(sd1, 2))
                .sd2(round(sd2, 2))
                .meanDifference(round(diff, 2))
                .standardError(round(se, 3))
                .ciLower(round(ciLower, 2))
                .ciUpper(round(ciUpper, 2))
                .tStatistic(round(tStat, 3))
                .degreesOfFreedom(round(df, 1))
                .pValue(round(pVal, 4))
                .alpha(alpha)
                .isSignificant(isSig)
                .cohensD(round(cohensD, 2))
                .effectSizeLabel(effectLabel)
                .conclusion(conclusion)
                .build();
    }

    private AnovaResultDTO computeAnovaResult(
            String factor,
            String dimension,
            Map<String, List<Double>> groupedData,
            double alpha
    ) {
        List<AnovaGroupStatDTO> groupStats = new ArrayList<>();
        List<double[]> validSamples = new ArrayList<>();

        double grandSum = 0.0;
        int totalN = 0;

        for (Map.Entry<String, List<Double>> entry : groupedData.entrySet()) {
            String gName = entry.getKey();
            List<Double> vals = entry.getValue();
            if (vals.isEmpty()) continue;

            double[] arr = vals.stream().mapToDouble(Double::doubleValue).toArray();
            DescriptiveStatistics st = new DescriptiveStatistics(arr);
            double mean = st.getMean();
            double var = vals.size() > 1 ? st.getVariance() : 0.0;
            double sd = Math.sqrt(var);
            double se = Math.sqrt(var / vals.size());

            double ciLower = mean - (1.96 * se);
            double ciUpper = mean + (1.96 * se);

            groupStats.add(AnovaGroupStatDTO.builder()
                    .groupName(gName)
                    .n(vals.size())
                    .mean(round(mean, 2))
                    .stdDev(round(sd, 2))
                    .standardError(round(se, 3))
                    .ciLower(round(ciLower, 2))
                    .ciUpper(round(ciUpper, 2))
                    .build());

            if (vals.size() >= 2) {
                validSamples.add(arr);
            }
            grandSum += st.getSum();
            totalN += vals.size();
        }

        double grandMean = totalN > 0 ? (grandSum / totalN) : 0.0;
        int k = groupStats.size();

        if (validSamples.size() < 2 || totalN <= k) {
            return AnovaResultDTO.builder()
                    .testName("One-Way Analysis of Variance (ANOVA)")
                    .factor(factor)
                    .dimension(dimension)
                    .hypothesis("H₀: All group means are equal vs H₁: At least one group mean is different")
                    .groups(groupStats)
                    .grandMean(round(grandMean, 2))
                    .totalN(totalN)
                    .dfBetween(Math.max(0, k - 1))
                    .dfWithin(Math.max(0, totalN - k))
                    .dfTotal(Math.max(0, totalN - 1))
                    .fStatistic(0.0)
                    .pValue(1.0)
                    .alpha(alpha)
                    .isSignificant(false)
                    .etaSquared(0.0)
                    .effectSizeLabel("N/A")
                    .conclusion("Insufficient observations across groups (need at least 2 groups with ≥2 observations).")
                    .build();
        }

        // Compute ANOVA Sum of Squares
        double ssBetween = 0.0;
        double ssWithin = 0.0;

        for (Map.Entry<String, List<Double>> entry : groupedData.entrySet()) {
            List<Double> vals = entry.getValue();
            if (vals.isEmpty()) continue;
            DescriptiveStatistics st = new DescriptiveStatistics(vals.stream().mapToDouble(Double::doubleValue).toArray());
            double grpMean = st.getMean();
            ssBetween += vals.size() * Math.pow(grpMean - grandMean, 2);
            for (double v : vals) {
                ssWithin += Math.pow(v - grpMean, 2);
            }
        }

        double ssTotal = ssBetween + ssWithin;
        int dfBetween = k - 1;
        int dfWithin = totalN - k;
        int dfTotal = totalN - 1;

        double msBetween = dfBetween > 0 ? (ssBetween / dfBetween) : 0.0;
        double msWithin = dfWithin > 0 ? (ssWithin / dfWithin) : 0.0;

        double fStat = 0.0;
        double pVal = 1.0;

        if (msWithin > 0.0) {
            fStat = msBetween / msWithin;
            try {
                pVal = anova.anovaPValue(validSamples);
            } catch (Exception e) {
                try {
                    FDistribution fDist = new FDistribution(dfBetween, dfWithin);
                    pVal = 1.0 - fDist.cumulativeProbability(fStat);
                } catch (Exception ex) {
                    pVal = 1.0;
                }
            }
        } else {
            fStat = msBetween > 0 ? 999.0 : 0.0;
            pVal = msBetween > 0 ? 0.0 : 1.0;
        }

        pVal = Math.clamp(pVal, 0.0, 1.0);
        double etaSquared = ssTotal > 0 ? (ssBetween / ssTotal) : 0.0;
        String effectLabel = getEtaSquaredLabel(etaSquared);
        boolean isSig = pVal < alpha;

        String conclusion = generateAnovaConclusion(factor, k, totalN, fStat, dfBetween, dfWithin, pVal, alpha, isSig, etaSquared, effectLabel);

        return AnovaResultDTO.builder()
                .testName("One-Way Analysis of Variance (ANOVA)")
                .factor(factor)
                .dimension(dimension)
                .hypothesis("H₀: All group means are equal (μ₁ = μ₂ = ... = μₖ) vs H₁: At least one group mean differs")
                .groups(groupStats)
                .grandMean(round(grandMean, 2))
                .totalN(totalN)
                .dfBetween(dfBetween)
                .dfWithin(dfWithin)
                .dfTotal(dfTotal)
                .ssBetween(round(ssBetween, 3))
                .ssWithin(round(ssWithin, 3))
                .ssTotal(round(ssTotal, 3))
                .msBetween(round(msBetween, 3))
                .msWithin(round(msWithin, 3))
                .fStatistic(round(fStat, 3))
                .pValue(round(pVal, 4))
                .alpha(alpha)
                .isSignificant(isSig)
                .etaSquared(round(etaSquared, 3))
                .effectSizeLabel(effectLabel)
                .conclusion(conclusion)
                .build();
    }

    // =========================================================================
    // DATA LOADER & PARSING HELPERS
    // =========================================================================

    public List<DbDimension> loadDatabaseEvaluationDimensions() {
        Map<String, DbDimension> dimensionMap = new LinkedHashMap<>();

        try {
            List<Question> questions = questionRepository.findAllWithOptions();
            for (Question q : questions) {
                String qText = q.getText() == null ? "" : q.getText().toLowerCase();
                String qType = q.getQuestionType() == null ? "" : q.getQuestionType().toUpperCase();
                boolean isEval = "TEXT".equals(qType) || "MATRIX".equals(qType)
                        || qText.contains("mood") || qText.contains("weather")
                        || qText.contains("association") || qText.contains("suitable") || qText.contains("question");

                if (isEval && q.getOptions() != null && !q.getOptions().isEmpty()) {
                    for (Option opt : q.getOptions()) {
                        String label = opt.getLabel() == null ? "" : opt.getLabel().trim();
                        if (label.isBlank()) continue;
                        String key = label.toLowerCase();
                        dimensionMap.putIfAbsent(key, new DbDimension(
                                key,
                                label,
                                opt.getIcon(),
                                opt.getSubDescription(),
                                q.getId(),
                                q.getText()
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error reading question options for dimensions: {}", e.getMessage());
        }

        // Also check answer records for any dimensions present in submitted answers
        try {
            List<Object[]> rows = answerRepository.findAllItemRatingResponses();
            for (Object[] r : rows) {
                if (!isRatingAnswer(r)) continue;
                String resp = r[4] == null ? null : r[4].toString();
                if (resp == null || resp.isBlank()) continue;
                var parsedRating = RatingResponseParser.parse(resp);
                if (parsedRating.isPresent()) {
                    String dimRaw = parsedRating.get().dimension();
                    String cleanLabel = extractLabel(dimRaw);
                    String key = cleanLabel.toLowerCase();
                    if (!key.isBlank()) {
                        dimensionMap.putIfAbsent(key, new DbDimension(
                                key,
                                cleanLabel,
                                null,
                                null,
                                null,
                                null
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error scanning answer responses for dimensions: {}", e.getMessage());
        }

        return new ArrayList<>(dimensionMap.values());
    }

    public List<StatisticalOverviewDTO.EvaluationQuestionDTO> loadEvaluationQuestions() {
        List<StatisticalOverviewDTO.EvaluationQuestionDTO> list = new ArrayList<>();
        try {
            List<Question> questions = questionRepository.findAllWithOptions();
            for (Question q : questions) {
                String qText = q.getText() == null ? "" : q.getText().trim();
                String lower = qText.toLowerCase();
                boolean isEval = "TEXT".equalsIgnoreCase(q.getQuestionType())
                        || "MATRIX".equalsIgnoreCase(q.getQuestionType())
                        || lower.contains("mood") || lower.contains("weather")
                        || lower.contains("association") || lower.contains("suitable");

                if (isEval && q.getOptions() != null && !q.getOptions().isEmpty()) {
                    String title = qText;
                    if (qText.contains(":")) {
                        title = qText.split(":", 2)[0].trim();
                    }
                    List<String> optLabels = q.getOptions().stream()
                            .map(Option::getLabel)
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .toList();

                    String factorKey;
                    if (lower.contains("mood") || lower.contains("emotion") || lower.contains("question 1")) {
                        factorKey = "MOOD_DIMENSIONS";
                    } else if (lower.contains("weather") || lower.contains("question 2")) {
                        factorKey = "WEATHER_DIMENSIONS";
                    } else {
                        factorKey = "QUESTION_" + q.getId();
                    }

                    list.add(StatisticalOverviewDTO.EvaluationQuestionDTO.builder()
                            .questionId(q.getId())
                            .title(title)
                            .factorKey(factorKey)
                            .factorLabel(String.format("%s (%d Dimensions from DB)", title, optLabels.size()))
                            .dimensionCount(optLabels.size())
                            .dimensionLabels(optLabels)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Error loading evaluation questions: {}", e.getMessage());
        }
        return list;
    }

    private List<RatingRecord> loadAllRatingRecords() {
        List<DbDimension> knownDimensions = loadDatabaseEvaluationDimensions();
        List<Object[]> rows = answerRepository.findAllItemRatingResponses();
        List<RatingRecord> list = new ArrayList<>();

        for (Object[] r : rows) {
            if (!isRatingAnswer(r)) continue;
            String userId = r[0] == null ? null : r[0].toString();
            Long menuItemId = asLong(r[1]);
            String itemName = r[2] == null ? "Unknown" : r[2].toString();
            String subcategory = r[3] == null ? "Other" : r[3].toString();
            String response = r[4] == null ? null : r[4].toString();

            if (response == null || response.isBlank()) continue;

            var parsedRating = RatingResponseParser.parse(response);
            if (parsedRating.isPresent()) {
                String dimRaw = parsedRating.get().dimension();
                String cleanLabel = extractLabel(dimRaw);
                String normalizedKey = cleanLabel.toLowerCase();

                DbDimension matched = knownDimensions.stream()
                        .filter(d -> d.key().equalsIgnoreCase(normalizedKey) || d.label().equalsIgnoreCase(cleanLabel))
                        .findFirst()
                        .orElse(null);

                String dimKey = matched != null ? matched.key() : normalizedKey;
                String dimOriginal = matched != null ? matched.label() : cleanLabel;
                int rating = parsedRating.get().value();

                String superCategory = resolveSupercategory(subcategory);

                list.add(new RatingRecord(
                        userId,
                        menuItemId,
                        itemName,
                        subcategory,
                        superCategory,
                        dimKey,
                        dimOriginal,
                        rating
                ));
            }
        }
        return list;
    }

    private boolean isRatingAnswer(Object[] row) {
        // Older repository fixtures omit these columns; production query rows include both.
        if (row.length <= 6 || row[5] == null || row[6] == null) return row.length <= 6;
        String type = row[5].toString();
        return "MATRIX".equalsIgnoreCase(type) || "RADIO".equalsIgnoreCase(type);
    }

    private Map<String, String> loadUserDemographics(String keyword) {
        List<Object[]> rows = answerRepository.findAllUserDemographics();
        Map<String, String> map = new HashMap<>();

        for (Object[] r : rows) {
            String userId = r[0] == null ? null : r[0].toString();
            String qText = r[1] == null ? "" : r[1].toString().toLowerCase();
            String resp = r[2] == null ? null : r[2].toString().trim();

            if (userId != null && resp != null && qText.contains(keyword.toLowerCase())) {
                map.put(userId, resp);
            }
        }
        return map;
    }

    private boolean matchesDimension(RatingRecord r, String dimension) {
        if (dimension == null || dimension.equalsIgnoreCase("all") || dimension.isBlank()) {
            return true;
        }
        String target = dimension.trim().toLowerCase();
        return r.dimensionKey().equalsIgnoreCase(target)
                || r.dimensionOriginal().toLowerCase().contains(target)
                || r.dimensionKey().contains(target);
    }

    private boolean matchesDimensionKey(RatingRecord r, String targetDim) {
        if (targetDim == null || targetDim.isBlank()) return false;
        String target = targetDim.trim().toLowerCase();
        return r.dimensionKey().equalsIgnoreCase(target)
                || r.dimensionOriginal().toLowerCase().contains(target)
                || r.dimensionKey().contains(target);
    }

    private String extractLabel(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        int paren = s.indexOf('(');
        if (paren > 0) {
            s = s.substring(0, paren).trim();
        }
        return s;
    }

    private String normalizeDimension(String raw) {
        return extractLabel(raw).toLowerCase();
    }

    private String resolveSupercategory(String subcategory) {
        if (subcategory == null) return "Other";
        if (MEALS_CATEGORIES.contains(subcategory.trim().toLowerCase())) {
            return "Meals";
        }
        return "Beverages";
    }

    private String getCohensDLabel(double d) {
        if (d < 0.2) return "Negligible";
        if (d < 0.5) return "Small effect";
        if (d < 0.8) return "Medium effect";
        return "Large effect";
    }

    private String getEtaSquaredLabel(double etaSq) {
        if (etaSq < 0.01) return "Negligible";
        if (etaSq < 0.06) return "Small effect";
        if (etaSq < 0.14) return "Medium effect";
        return "Large effect";
    }

    private String generateTTestConclusion(
            String g1, String g2, double m1, double m2,
            double t, double df, double p, double alpha,
            boolean isSig, String effectLabel
    ) {
        if (isSig) {
            String higher = m1 > m2 ? g1 : g2;
            String lower = m1 > m2 ? g2 : g1;
            double diff = Math.abs(m1 - m2);
            return String.format(
                    "Statistically significant difference detected (t(%s) = %s, p = %s < %s). '%s' (M = %s) scored significantly higher than '%s' (M = %s) by %s points with a %s size.",
                    round(df, 1), round(t, 2), formatP(p), alpha, higher, round(Math.max(m1, m2), 2), lower, round(Math.min(m1, m2), 2), round(diff, 2), effectLabel.toLowerCase()
            );
        } else {
            return String.format(
                    "No statistically significant difference found between '%s' (M = %s) and '%s' (M = %s) at α = %s (t(%s) = %s, p = %s). Any observed difference is likely due to sample variation.",
                    g1, round(m1, 2), g2, round(m2, 2), alpha, round(df, 1), round(t, 2), formatP(p)
            );
        }
    }

    private String generateAnovaConclusion(
            String factor, int k, int totalN, double f,
            int dfB, int dfW, double p, double alpha,
            boolean isSig, double etaSq, String effectLabel
    ) {
        if (isSig) {
            return String.format(
                    "Statistically significant variance across %s groups detected (F(%d, %d) = %s, p = %s < %s). The factor accounts for %s%% of total score variance (η² = %s, %s). Customer preferences differ meaningfully across these segments.",
                    factor.toLowerCase().replace('_', ' '), dfB, dfW, round(f, 2), formatP(p), alpha, round(etaSq * 100.0, 1), round(etaSq, 3), effectLabel.toLowerCase()
            );
        } else {
            return String.format(
                    "No statistically significant difference found across %s groups at α = %s (F(%d, %d) = %s, p = %s). Mean scores are statistically homogenous across groups.",
                    factor.toLowerCase().replace('_', ' '), alpha, dfB, dfW, round(f, 2), formatP(p)
            );
        }
    }

    private String formatP(double p) {
        if (p < 0.001) return "< 0.001";
        return String.valueOf(round(p, 4));
    }

    private double round(double val, int decimals) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return 0.0;
        double scale = Math.pow(10.0, decimals);
        return Math.round(val * scale) / scale;
    }

    private Long asLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number num) return num.longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseId(String s) {
        if (s == null) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isBlank()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
