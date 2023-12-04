package com.example.autoscheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plans")
public class PlanDataController {

    @Autowired
    private PlanDataRepositoryStudent planDataRepositoryUni;

    // Define the mapping of 'type' to its corresponding code
    private Map<String, Integer> typeCodeMap = new HashMap<>();

    public PlanDataController() {
        // 생성자에서 초기화
        typeCodeMap.put("공부", 1);
        typeCodeMap.put("저녁", 2);
        typeCodeMap.put("점심식사", 2);
        typeCodeMap.put("아침식사", 2);
        typeCodeMap.put("아침", 2);
        typeCodeMap.put("점심", 2);
        typeCodeMap.put("휴식", 3);
        typeCodeMap.put("게임", 3);
        typeCodeMap.put("여가", 4);
        typeCodeMap.put("수면", 5);
        typeCodeMap.put("취침", 5);
        typeCodeMap.put("숙면", 5);
        typeCodeMap.put("운동", 6);
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

        return topTimeCodes;
    }

    private List<String> geneticAlgorithm(List<String> timeCodes) {
        // Existing genetic algorithm logic...
        // For the sake of example, simply return a shuffled list
        List<String> shuffledCodes = new ArrayList<>(timeCodes);
        Collections.shuffle(shuffledCodes);
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
        // Placeholder implementation of mutation
        // You need to implement the actual mutation logic

        // For demonstration purposes, randomly select a position and change its value
        Random random = new Random();
        int position = random.nextInt(48);
        char newValue = Character.forDigit(random.nextInt(6), 10); // Assume values from 0 to 5
        offspring.forEach(code -> {
            char[] codeArray = code.toCharArray();
            codeArray[position] = newValue;
            code = new String(codeArray);
        });
    }
    // ////////////Existing methods...

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

    @GetMapping("/school-students")
    public List<Integer> getSchoolStudents() {
        List<PlanData> middleSchoolStudents = planDataRepositoryUni.findByStudentIs("중학생");
        List<PlanData> highSchoolStudents = planDataRepositoryUni.findByStudentEquals("고등학생");

        // Combine the two lists (You may need to handle duplicates based on your requirements)
        middleSchoolStudents.addAll(highSchoolStudents);

        List<Integer> personValues = middleSchoolStudents.stream()
                .map(PlanData::getPerson)
                .collect(Collectors.toList());

        return personValues;
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

    // Method to calculate the time slot index (30-minute slots) for a given time
    private int getSlotIndex(LocalDateTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        return (hour * 2) + (minute / 30);
    }


}