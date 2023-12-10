package com.example.autoscheduler;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/plans")
public class PlanDataController {

    @Autowired
    private PlanDataRepositoryStudent planDataRepositoryUni;
    @Autowired
    private PlanDataRepositoryUser planDataRepositoryUser;



    // Define the mapping of 'type' to its corresponding code
    private Map<String, Integer> typeCodeMap = new HashMap<>();

    public PlanDataController() {
        // 생성자에서 초기화
        typeCodeMap.put("공부", 1);
        typeCodeMap.put("저녁", 2);
        typeCodeMap.put("점심식사", 2);
        typeCodeMap.put("아침식사", 2);
        typeCodeMap.put("식사", 2);
        typeCodeMap.put("아침", 2);
        typeCodeMap.put("점심", 2);
        typeCodeMap.put("휴식", 3);
        typeCodeMap.put("게임", 3);
        typeCodeMap.put("여가", 4);
        typeCodeMap.put("여가시간", 4);
        typeCodeMap.put("자기계발", 4);
        typeCodeMap.put("수면", 0);
        typeCodeMap.put("취침", 0);
        typeCodeMap.put("숙면", 0);
        typeCodeMap.put("중요일정", 8);
        typeCodeMap.put("운동", 6);
    }

    @GetMapping("/auto/{user}/{setting}")
    public String getTimeCodes(@PathVariable String user, @PathVariable int setting) throws JsonProcessingException {
        // Find PlanDataUser by 'user_id'
        List<PlanDataUser> userPlans = planDataRepositoryUser.findByUserId(user);
        List<String> auto;
        List<String> noauto;



        List<PlanDataUser> noPlans = planDataRepositoryUser.findByUserIdAndPlanIs(user, "고정");
        LocalDate currentDate = LocalDate.now();

// Filter the list to get entries with start date on the same day as today
        List<PlanDataUser> todayPlanDataUsers = noPlans.stream()
                .filter(planDataUser -> planDataUser.getStart().toLocalDate().isEqual(currentDate))
                .collect(Collectors.toList());
        List<String> nono = todayPlanDataUsers.stream()
                .collect(Collectors.groupingBy(planDataUser -> planDataUser.getStartDate().toString())) // Group by 'user_id'
                .entrySet().stream()
                .map(entry -> {
                    List<PlanDataUser> userData = entry.getValue();

                    // Combine time codes for the same user
                    return combineTimeCodesUser(userData);
                })
                .collect(Collectors.toList());
        noauto = nono;







        if (userPlans.size() >= 10) {
            List<String> timeCodesList = userPlans.stream()
                    .collect(Collectors.groupingBy(planDataUser -> planDataUser.getStartDate().toString())) // Group by 'user_id'
                    .entrySet().stream()
                    .map(entry -> {
                        List<PlanDataUser> userData = entry.getValue();

                        // Combine time codes for the same user
                        return combineTimeCodesUser(userData);
                    })
                    .collect(Collectors.toList());
            auto = timeCodesList;
        }else{
            List<PlanData> uniStudents = planDataRepositoryUni.findByStudentIs("대학생");
            List<Integer> personValues = uniStudents.stream()
                    .map(PlanData::getPerson)
                    .collect(Collectors.toList());
            List<PlanData> matchingData = planDataRepositoryUni.findByPersonIn(personValues);

            List<String> timeCodeList = matchingData.stream()
                    .collect(Collectors.groupingBy(PlanData::getPerson)) // Group by 'person'
                    .entrySet().stream()
                    .map(entry -> {
                        int person = entry.getKey();
                        List<PlanData> personData = entry.getValue();

                        // Combine time codes for the same person
                        String combinedTimeCode = combineTimeCodes(personData);

                        return combinedTimeCode;
                    })
                    .collect(Collectors.toList());

            auto = timeCodeList;
        }



        // Extract 'planType', 'sex', 'start', 'end', and 'user_id' values and create a list of UserDto


        int generations = 100;
        int populationSize = 50;

        Chromosome bestChromosome = evolveAndFindBestChromosome(auto, generations, populationSize, setting);

        System.out.println("Best Chromosome: " + bestChromosome.genes);

        List<TimePeriod> timePeriods = parseTimeCode(bestChromosome.genes);

        for (TimePeriod timePeriod : timePeriods) {
            System.out.println(timePeriod);
        }

        String time = timePeriods.toString();



        ObjectMapper objectMapper = new ObjectMapper();

        // 문자열을 JSON 배열로 변환
        try {
            // 문자열을 JSON 배열로 파싱
            JsonNode jsonNode = objectMapper.readTree("[" + time + "]");

            // JSON 배열을 문자열로 변환
            String timeNew = objectMapper.writeValueAsString(jsonNode);

            // timeNew 출력
            System.out.println(timeNew);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }



        //return noauto.toString();
        //return timeNew;

        return timePeriods.toString();



    }


    public static List<TimePeriod> parseTimeCode(String timeCode) {
        List<TimePeriod> timePeriods = new ArrayList<>();
        int currentType = -1; // 현재 처리 중인 데이터의 type
        int startSlot = -1; // 현재 처리 중인 시간 범위의 시작 슬롯

        for (int i = 0; i < timeCode.length(); i++) {
            int typeCode = Character.getNumericValue(timeCode.charAt(i));

            if (typeCode != 0) {
                // type이 0이 아닌 경우에만 처리
                if (typeCode != currentType) {
                    // 이전 데이터와 현재 데이터의 type이 다를 때
                    if (currentType != -1 && startSlot != -1) {
                        // 이전 데이터가 있는 경우에만 처리
                        LocalTime startTime = getSlotTime(startSlot);
                        LocalTime endTime = getSlotTime(i); // 여기서 변경: i로 수정
                        timePeriods.add(new TimePeriod(currentType, startTime, endTime));
                    }

                    // 새로운 데이터의 처리 시작
                    currentType = typeCode;
                    startSlot = i;
                }
            } else {
                // type이 0인 경우 처리 중단
                if (currentType != -1 && startSlot != -1) {
                    // 이전 데이터가 있는 경우에만 처리
                    LocalTime startTime = getSlotTime(startSlot);
                    LocalTime endTime = getSlotTime(i); // 여기서 변경: i로 수정
                    timePeriods.add(new TimePeriod(currentType, startTime, endTime));
                }

                currentType = -1;
                startSlot = -1;
            }
        }

        // 마지막 데이터 처리
        if (currentType != -1 && startSlot != -1) {
            LocalTime startTime = getSlotTime(startSlot);
            LocalTime endTime = getSlotTime(timeCode.length() - 1);
            timePeriods.add(new TimePeriod(currentType, startTime, endTime));
        }

        return timePeriods;
    }

    private static LocalTime getSlotTime(int slotIndex) {
        // 각 슬롯의 인덱스에 따른 LocalTime 계산
        int minutes = slotIndex * 30;
        return LocalTime.of((minutes / 60) % 24, minutes % 60);
    }




    static class TimePeriod {
        private final String type;
        private final LocalTime startTime;
        private final LocalTime endTime;

        public TimePeriod(int type, LocalTime startTime, LocalTime endTime) {
            this.type = mapType(type);
            this.startTime = startTime;
            this.endTime = endTime;
        }

        private String mapType(int type) {
            switch (type) {
                case 1:
                    return "공부";
                case 2:
                    return "식사";
                case 3:
                    return "휴식";
                case 4:
                    return "여가";
                case 5:
                    return "휴식";
                case 6:
                    return "운동";
                default:
                    return "알 수 없음";
            }
        }

        @Override
        public String toString() {
            return "{" +
                    "\"type\":\"" + type + '\"' +
                    ", \"startTime\":\"" + startTime + '\"' +
                    ", \"endTime\":\"" + endTime + '\"' +
                    '}';
        }
    }


    private double calculateSettingWeight(int setting, List<String> timeCodeList) {
        double baseWeight = 0.5; // 기본 가중치
        double matchingWeight = 0.1; // 똑같은 숫자가 들어있을 때 부여할 가중치

        for (String timeCode : timeCodeList) {
            for (int i = 0; i < timeCode.length(); i++) {
                int timeCodeDigit = Character.getNumericValue(timeCode.charAt(i));
                if (timeCodeDigit == setting) {
                    // 똑같은 숫자가 있을 때 가중치 적용
                    return baseWeight + matchingWeight;
                }
            }
        }

        // 똑같은 숫자가 없을 때 기본 가중치 반환
        return baseWeight;
    }
    @GetMapping("/university-students")
    public List<Integer> getUniversityStudents() {
        List<PlanData> uniStudents = planDataRepositoryUni.findByStudentIs("대학생");
        return uniStudents.stream()
                .map(PlanData::getPerson)
                .collect(Collectors.toList());
    }

    @GetMapping("/peee")
    public List<TimeCodeDto> getPersonsStartEndWithTypeCode() {
        List<PlanData> uniStudents = planDataRepositoryUni.findByStudentIs("대학생");
        List<Integer> personValues = uniStudents.stream()
                .map(PlanData::getPerson)
                .collect(Collectors.toList());

        // Find PlanData by 'person' in the given list
        List<PlanData> matchingData = planDataRepositoryUni.findByPersonIn(personValues);

        // Extract 'typeCode', 'sex', 'start', 'end', and 'person' values and create a list of TimeCodeDto
        List<TimeCodeDto> timeCodeList = matchingData.stream()
                .collect(Collectors.groupingBy(PlanData::getPerson)) // Group by 'person'
                .entrySet().stream()
                .map(entry -> {
                    int person = entry.getKey();
                    List<PlanData> personData = entry.getValue();

                    // Combine time codes for the same person
                    String combinedTimeCode = combineTimeCodes(personData);

                    // Create TimeCodeDto
                    return new TimeCodeDto(person, personData.get(0).getSex(), combinedTimeCode);
                })
                .collect(Collectors.toList());

        return timeCodeList;
    }

    @GetMapping("/g4")
    public String gStu() {


        List<PlanData> middleSchoolStudents = planDataRepositoryUni.findByStudentIs("중학생");
        List<PlanData> highSchoolStudents = planDataRepositoryUni.findByStudentEquals("고등학생");

        // Combine the two lists (You may need to handle duplicates based on your requirements)
        middleSchoolStudents.addAll(highSchoolStudents);

        List<Integer> personValues = middleSchoolStudents.stream()
                .map(PlanData::getPerson)
                .collect(Collectors.toList());


        List<PlanData> matchingData = planDataRepositoryUni.findByPersonIn(personValues);

        List<String> timeCodeList = matchingData.stream()
                .collect(Collectors.groupingBy(PlanData::getPerson)) // Group by 'person'
                .entrySet().stream()
                .map(entry -> {
                    int person = entry.getKey();
                    List<PlanData> personData = entry.getValue();

                    // Combine time codes for the same person
                    String combinedTimeCode = combineTimeCodes(personData);

                    return combinedTimeCode;
                })
                .collect(Collectors.toList());

        int generations = 100;
        int populationSize = 50;

        Chromosome bestChromosome = evolveAndFindBestChromosome(timeCodeList, generations, populationSize, 9);

        System.out.println("Best Chromosome: " + bestChromosome.genes);

        return bestChromosome.genes;
    }

    @GetMapping("/g2")
    public String gUni() {
        List<PlanData> uniStudents = planDataRepositoryUni.findByStudentIs("대학생");
        List<Integer> personValues = uniStudents.stream()
                .map(PlanData::getPerson)
                .collect(Collectors.toList());
        List<PlanData> matchingData = planDataRepositoryUni.findByPersonIn(personValues);

        List<String> timeCodeList = matchingData.stream()
                .collect(Collectors.groupingBy(PlanData::getPerson)) // Group by 'person'
                .entrySet().stream()
                .map(entry -> {
                    int person = entry.getKey();
                    List<PlanData> personData = entry.getValue();

                    // Combine time codes for the same person
                    String combinedTimeCode = combineTimeCodes(personData);

                    return combinedTimeCode;
                })
                .collect(Collectors.toList());

        int generations = 200;
        int populationSize = 50;

        Chromosome bestChromosome = evolveAndFindBestChromosome(timeCodeList, generations, populationSize, 9);

        System.out.println("Best Chromosome: " + bestChromosome.genes);

        return bestChromosome.genes;


    }

    @GetMapping("/g2/{id}")
    public String gUniSt() {
        List<PlanData> uniStudents = planDataRepositoryUni.findByStudentIs("대학생");
        List<Integer> personValues = uniStudents.stream()
                .map(PlanData::getPerson)
                .collect(Collectors.toList());
        List<PlanData> matchingData = planDataRepositoryUni.findByPersonIn(personValues);

        List<String> timeCodeList = matchingData.stream()
                .collect(Collectors.groupingBy(PlanData::getPerson)) // Group by 'person'
                .entrySet().stream()
                .map(entry -> {
                    int person = entry.getKey();
                    List<PlanData> personData = entry.getValue();

                    // Combine time codes for the same person
                    String combinedTimeCode = combineTimeCodes(personData);

                    return combinedTimeCode;
                })
                .collect(Collectors.toList());

        int generations = 300;
        int populationSize = 30;

        Chromosome bestChromosome = evolveAndFindBestChromosome(timeCodeList, generations, populationSize, 9);

        System.out.println("Best Chromosome: " + bestChromosome.genes);

        return bestChromosome.genes;


    }

    static Chromosome evolveAndFindBestChromosome(List<String> timeCodeList, int generations, int populationSize, int setting) {
        List<Chromosome> population = initializePopulation(populationSize, timeCodeList.get(0).length(), setting);

        Chromosome bestChromosome = null;
        for (int generation = 0; generation < generations; generation++) {
            evaluatePopulation(population, timeCodeList, setting);
            population = evolvePopulation(population);

            // 현재 세대에서 가장 좋은 개체를 기록
            Chromosome currentBest = getBestChromosome(population, timeCodeList, setting);
            if (bestChromosome == null || currentBest.fitness > bestChromosome.fitness) {
                bestChromosome = new Chromosome(currentBest.genes);
            }
        }

        return bestChromosome;
    }

    static List<Chromosome> initializePopulation(int populationSize, int geneLength, int setting) {
        List<Chromosome> population = new ArrayList<>();
        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < populationSize; i++) {
            StringBuilder genes = new StringBuilder();
            for (int j = 0; j < geneLength; j++) {
                if (j >= 0 && j <= 13) {
                    genes.append("0"); // 1번째부터 14번째까지는 항상 5로 고정
                } else {
                    // 14번째 이후의 gene 중 일부를 setting 값에 따라 고정
                    if (random.nextDouble() < calculateSettingProbability(setting)) {
                        genes.append(setting);
                    } else {
                        genes.append(random.nextInt(7)); // 0~6의 숫자를 랜덤으로 생성
                    }
                }
            }
            genes.setCharAt(14, '0'); // 15번째를 0으로 고정
            population.add(new Chromosome(genes.toString()));
        }

        return population;
    }

    static double calculateSettingProbability(int setting) {
        // setting 값에 따라 고정될 확률을 계산하여 반환
        // 예를 들어, setting이 1일 때는 0.1, setting이 2일 때는 0.2의 확률로 고정
        return 0.1 * setting;
    }

    static void evaluatePopulation(List<Chromosome> population, List<String> timeCodeList, int setting) {
        for (Chromosome chromosome : population) {
            double fitness = calculateFitness(chromosome.genes, timeCodeList, setting);
            chromosome.fitness = fitness;
        }
    }

    static double calculateFitness(String genes, List<String> timeCodeList, int setting) {
        double totalFitness = 0.0;

        for (String timeCode : timeCodeList) {
            int matchCount = 0;
            int settingCount = 0;

            for (int i = 0; i < genes.length(); i++) {
                if (genes.charAt(i) == timeCode.charAt(i)) {
                    matchCount++;
                }

                // 추가: {setting} 값에 따라 가중치를 조절
                if (Character.getNumericValue(timeCode.charAt(i)) == setting) {
                    settingCount++;
                }
            }

            // 기존 코드에서 유사도를 계산한 부분
            double matchFitness = (double) matchCount / genes.length();
            totalFitness += matchFitness;

            // 추가: 연속된 수에 대한 가중치 부여
            int consecutiveCount = countConsecutive(genes);
            double consecutiveWeight = 0.2; // 연속된 수에 대한 기본 가중치

            // 연속된 수의 개수에 따라 가중치를 더 높임
            double weightedConsecutiveWeight = consecutiveWeight * (1 + consecutiveCount);
            totalFitness += weightedConsecutiveWeight;

            // 추가: 다른 유사도 측정 방식에 대한 가중치 부여
            double customSimilarityWeight = calculateCustomSimilarity(genes, timeCode);
            totalFitness += customSimilarityWeight;

            // 추가: {setting} 값에 따라 가중치를 조절
            double settingWeight = calculateSettingWeight(setting, settingCount);
            totalFitness += settingWeight;
        }

        return totalFitness / timeCodeList.size();
    }



    static double calculateSettingWeight(int setting, int settingCount) {
        // {setting} 값에 따라 적절한 가중치를 계산하여 반환
        // 예를 들어, setting이 1일 때는 0.1 * settingCount, setting이 2일 때는 0.2 * settingCount를 반환하도록 설정
        if(setting == 6)
            setting = 100;
        return 1 * setting * settingCount;
    }

    static boolean containsSettingValue(String timeCode, int setting) {
        // timeCode에 {setting} 값이 포함되어 있는지 확인
        return timeCode.chars().anyMatch(ch -> Character.getNumericValue(ch) == setting);
    }

    // 사용자 정의 유사도 측정 메서드 (예시)
    static double calculateCustomSimilarity(String genes, String timeCode) {
        // 여기에서 원하는 유사도 측정 방식을 구현하고 가중치를 반환
        // 예를 들어, 유사한 패턴이나 특정 조건에 따라 가중치를 부여할 수 있습니다.
        // 간단한 예시로 문자열 길이에 대한 가중치를 부여하는 방법을 보여줍니다.
        double lengthDifference = Math.abs(genes.length() - timeCode.length());
        double lengthWeight = 0.1; // 길이 차이에 대한 기본 가중치
        return lengthWeight * lengthDifference;
    }

    static int countConsecutive(String genes) {
        int consecutiveCount = 0;
        int maxConsecutive = 4; // 최대 연속된 수의 개수

        for (int i = 0; i < genes.length() - 1; i++) {
            if (Math.abs(genes.charAt(i + 1) - genes.charAt(i)) <= 1) {
                consecutiveCount++;
                if (consecutiveCount >= maxConsecutive) {
                    return maxConsecutive; // 최대 개수를 초과하면 더 이상 세지 않음
                }
            } else {
                consecutiveCount = 0; // 연속이 끊기면 초기화
            }
        }

        return consecutiveCount;
    }

    static List<Chromosome> evolvePopulation(List<Chromosome> population) {
        Collections.sort(population, Comparator.comparingDouble(chromosome -> -chromosome.fitness));

        List<Chromosome> newPopulation = new ArrayList<>();
        int eliteCount = population.size() / 5; // 상위 10%는 그대로 전이

        for (int i = 0; i < eliteCount; i++) {
            newPopulation.add(population.get(i));
        }

        Random random = new Random();
        for (int i = eliteCount; i < population.size(); i++) {
            Chromosome parent1 = selectParent(population, random);
            Chromosome parent2 = selectParent(population, random);
            Chromosome child = crossover(parent1, parent2);
            mutate(child);
            newPopulation.add(child);
        }

        return newPopulation;
    }

    static Chromosome selectParent(List<Chromosome> population, Random random) {
        int index = random.nextInt(population.size());
        return population.get(index);
    }

    static Chromosome crossover(Chromosome parent1, Chromosome parent2) {
        int splitPoint = parent1.genes.length() / 2;
        String childGenes = parent1.genes.substring(0, splitPoint) + parent2.genes.substring(splitPoint);
        return new Chromosome(childGenes);
    }

    static void mutate(Chromosome chromosome) {
        Random random = new Random();

        while (true) {
            int mutationPoint = random.nextInt(chromosome.genes.length() - 2); // 인덱스 범위를 초과하지 않도록 설정
            int mutationValue = random.nextInt(7);

            // 연속된 숫자가 5개 이상인 경우 다시 랜덤한 값을 선택
            if (!hasConsecutiveGreaterThan5(chromosome.genes, mutationPoint, mutationValue)) {
                if (mutationPoint + 2 < chromosome.genes.length()) {
                    chromosome.genes = chromosome.genes.substring(0, mutationPoint)
                            + mutationValue + mutationValue + mutationValue
                            + chromosome.genes.substring(mutationPoint + 3);
                } else {
                    chromosome.genes = chromosome.genes.substring(0, mutationPoint)
                            + mutationValue + mutationValue + mutationValue;
                }
                break;
            }
        }
    }

    static boolean hasConsecutiveGreaterThan5(String genes, int mutationPoint, int mutationValue) {
        int consecutiveCount = 0;

        for (int i = mutationPoint; i < genes.length(); i++) {
            if (genes.charAt(i) == Character.forDigit(mutationValue, 10)) {
                consecutiveCount++;
                if (consecutiveCount >= 5) {
                    return true; // 연속된 숫자가 5개 이상이면 true 반환
                }
            } else {
                consecutiveCount = 0; // 연속이 끊기면 초기화
            }
        }

        return false; // 연속된 숫자가 5개 이상이 아니면 false 반환
    }

    static Chromosome getBestChromosome(List<Chromosome> population, List<String> timeCodeList, int setting) {
        evaluatePopulation(population, timeCodeList, setting);
        return Collections.max(population, Comparator.comparingDouble(chromosome -> chromosome.fitness));
    }

    static class Chromosome {
        String genes;
        double fitness;

        Chromosome(String genes) {
            this.genes = genes;
        }
    }



    //////////////////////////////////////////////////////////
    @GetMapping("/genetic-algorithm")
    public List<String> runGeneticAlgorithm() {
        List<TimeCodeDto> timeCodeList = getPersonsStartEndWithTypeCode();

        // Extract time codes from the TimeCodeDto list
        List<String> timeCodes = timeCodeList.stream()
                .map(TimeCodeDto::toString)
                .collect(Collectors.toList());

        // Run genetic algorithm for a random number of generations (200~300)
        int generations = ThreadLocalRandom.current().nextInt(100, 201);
        for (int generation = 1; generation <= generations; generation++) {
            List<String> newGeneration = geneticAlgorithm(timeCodes);
            System.out.println("Generation " + generation + ": " + newGeneration);
            timeCodes = newGeneration;
        }

        // Evaluate fitness scores for the final generation
        Map<String, Integer> fitnessScores = evaluateFitness(timeCodes);

        // Select the top 7 time codes based on fitness scores
        List<String> topTimeCodes = fitnessScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(7)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return timeCodes;
    }

    private List<String> geneticAlgorithm(List<String> timeCodes) {
        // Existing genetic algorithm logic...
        // For the sake of example, simply return a shuffled list
        List<String> shuffledCodes = new ArrayList<>(timeCodes);
        Collections.shuffle(shuffledCodes);
        applyMutation(shuffledCodes); // Apply mutation to the new generation
        return shuffledCodes;
    }

    private Map<String, Integer> evaluateFitness(List<String> timeCodes) {
        // Actual implementation of fitness evaluation with weights

        Map<String, Integer> fitnessScores = new HashMap<>();

        for (String code : timeCodes) {
            int fitnessScore = 0;

            // Calculate fitness score with weights
            for (int i = 0; i < code.length(); i++) {
                char value = code.charAt(i);
                int weight = 1;

                if (value == '1') {
                    weight = (int) (1.5 * weight); // Increase weight for '1'
                } else if (value == '6') {
                    weight = 3 * weight; // Increase weight for '6'
                }

                fitnessScore += weight;
            }

            fitnessScores.put(code, fitnessScore);
        }

        return fitnessScores;
    }

    private List<String> selectParents(List<String> timeCodes, Map<String, Integer> fitnessScores) {
        // Placeholder implementation of parent selection
        // You need to implement the actual parent selection logic

        // For demonstration purposes, select parents randomly
        Collections.shuffle(timeCodes);
        return timeCodes.subList(0, Math.min(5, timeCodes.size())); // Select top 5 by default
    }

    private List<String> crossover(List<String> parents) {
        // Placeholder implementation of crossover
        // You need to implement the actual crossover logic

        // For demonstration purposes, simply copy parents
        return new ArrayList<>(parents);
    }

    private void applyMutation(List<String> offspring) {
        Random random = new Random();

        for (int i = 0; i < offspring.size(); i++) {
            String code = offspring.get(i);
            char[] codeArray = code.toCharArray();

            // Fix the count of '5' to 16
            long countOfFive = code.chars().filter(c -> c == '5').count();
            if (countOfFive != 16) {
                int position = random.nextInt(48);
                codeArray[position] = '5';
            }

            // Ensure that numbers are not consecutive, and limit consecutive occurrences of 2, 3, 4, 5, 6 to 4
            for (int j = 0; j < codeArray.length - 1; j++) {
                if (Character.isDigit(codeArray[j]) && Character.isDigit(codeArray[j + 1])) {
                    codeArray[j + 1] = '0';  // Set the next digit to '0'
                }

                // Limit consecutive occurrences of 2, 3, 4, 5, 6 to 4
                if (Character.isDigit(codeArray[j]) && Character.isDigit(codeArray[j + 1]) &&
                        Character.getNumericValue(codeArray[j]) >= 2 && Character.getNumericValue(codeArray[j]) <= 6) {
                    codeArray[j + 1] = '0';  // Set the next digit to '0'
                }
            }

            offspring.set(i, new String(codeArray));
        }
    }

    // Method to combine time codes for the same person
    private String combineTimeCodes(List<PlanData> personData) {
        // Initialize a char array for the combined time code with 48 '0' characters
        char[] combinedTimeCode = new char[48];
        for (int i = 0; i < combinedTimeCode.length; i++) {
            combinedTimeCode[i] = '0';
        }

        // Iterate through each PlanData and update the combined time code
        for (PlanData planData : personData) {
            String timeCode = createTypeCode(planData);
            for (int i = 0; i < combinedTimeCode.length; i++) {
                // Combine time codes by taking the maximum value at each position
                combinedTimeCode[i] = (char) Math.max(combinedTimeCode[i], timeCode.charAt(i));
            }
        }

        // Convert the char array to a String
        return new String(combinedTimeCode);
    }

    private String combineTimeCodesUser(List<PlanDataUser> personDataUser) {
        // Initialize a char array for the combined time code with 48 '0' characters
        char[] combinedTimeCode = new char[48];
        for (int i = 0; i < combinedTimeCode.length; i++) {
            combinedTimeCode[i] = '0';
        }

        // Iterate through each PlanData and update the combined time code
        for (PlanDataUser planData : personDataUser) {
            String timeCode = createTypeCodeUser(planData);
            for (int i = 0; i < combinedTimeCode.length; i++) {
                // Combine time codes by taking the maximum value at each position
                combinedTimeCode[i] = (char) Math.max(combinedTimeCode[i], timeCode.charAt(i));
            }
        }

        // Convert the char array to a String
        return new String(combinedTimeCode);
    }



    @GetMapping("/school-students")
    public List<PlanData> getSchoolStudents() {
        List<PlanData> middleSchoolStudents = planDataRepositoryUni.findByStudentIs("중학생");
        List<PlanData> highSchoolStudents = planDataRepositoryUni.findByStudentEquals("고등학생");

        // Combine the two lists (You may need to handle duplicates based on your requirements)
        middleSchoolStudents.addAll(highSchoolStudents);

        List<Integer> personValues = middleSchoolStudents.stream()
                .map(PlanData::getPerson)
                .collect(Collectors.toList());

        List<PlanData> uniStudents = planDataRepositoryUni.findByStudentIs("대학생");
        List<Integer> pe = uniStudents.stream()
                .map(PlanData::getPerson)
                .collect(Collectors.toList());
        List<PlanData> matchingData = planDataRepositoryUni.findByPersonIn(pe);

        return uniStudents;
    }

    // Method to create the time code for a given PlanData
    private String createTypeCode(PlanData planData) {
        // Extract the 'start' and 'end' times from the PlanData
        LocalDateTime startTime = planData.getStart();
        LocalDateTime endTime = planData.getEnd();

        // Initialize a char array for the time code with 48 '0' characters
        char[] timeCode = new char[48];
        for (int i = 0; i < timeCode.length; i++) {
            timeCode[i] = '0';
        }

        // Set the type code in the time code array based on the 'start' and 'end' times
        int typeCode = typeCodeMap.getOrDefault(planData.getType(), 0);

        // Check if 'start' is greater than 'end', indicating a span across midnight
        if (startTime.isAfter(endTime)) {
            // Calculate the time code for the first part of the span
            int startSlot = getSlotIndex(startTime);
            int endSlot = 47; // Index of the last slot

            for (int i = startSlot; i <= endSlot; i++) {
                timeCode[i] = Character.forDigit(typeCode, 10);
            }

            // Calculate the time code for the second part of the span
            startSlot = 0; // Index of the first slot
            endSlot = getSlotIndex(endTime);

            for (int i = startSlot; i <= endSlot; i++) {
                timeCode[i] = Character.forDigit(typeCode, 10);
            }
        } else {
            // Calculate the time code for the regular case
            int startSlot = getSlotIndex(startTime);
            int endSlot = getSlotIndex(endTime);

            for (int i = startSlot; i <= endSlot; i++) {
                timeCode[i] = Character.forDigit(typeCode, 10);
            }
        }




        // Convert the char array to a String
        return new String(timeCode);
    }

    private String createTypeCodeUser(PlanDataUser planData) {
        // Extract the 'start' and 'end' times from the PlanData
        LocalDateTime startTime = planData.getStart();
        LocalDateTime endTime = planData.getEnd();

        // Initialize a char array for the time code with 48 '0' characters
        char[] timeCode = new char[48];
        for (int i = 0; i < timeCode.length; i++) {
            timeCode[i] = '0';
        }

        // Set the type code in the time code array based on the 'start' and 'end' times
        int typeCode = typeCodeMap.getOrDefault(planData.getPlantype(), 0);

        // Check if 'start' is greater than 'end', indicating a span across midnight
        if (startTime.isAfter(endTime)) {
            // Calculate the time code for the first part of the span
            int startSlot = getSlotIndex(startTime);
            int endSlot = 47; // Index of the last slot

            for (int i = startSlot; i <= endSlot; i++) {
                timeCode[i] = Character.forDigit(typeCode, 10);
            }

            // Calculate the time code for the second part of the span
            startSlot = 0; // Index of the first slot
            endSlot = getSlotIndex(endTime);

            for (int i = startSlot; i <= endSlot; i++) {
                timeCode[i] = Character.forDigit(typeCode, 10);
            }
        } else {
            // Calculate the time code for the regular case
            int startSlot = getSlotIndex(startTime);
            int endSlot = getSlotIndex(endTime);

            for (int i = startSlot; i <= endSlot; i++) {
                timeCode[i] = Character.forDigit(typeCode, 10);
            }
        }




        // Convert the char array to a String
        return new String(timeCode);
    }

    // Method to calculate the time slot index (30-minute slots) for a given time
    private int getSlotIndex(LocalDateTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        return (hour * 2) + (minute / 30);
    }


}