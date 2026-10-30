package com.edupulse.service;

import com.edupulse.dto.TimetableSlotDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TimetableService {

    public static final Map<String, String> SUBJECT_NAMES = Map.of(
            "CSE 203", "DISCRETE MATHEMATICS",
            "CSE 204", "DESIGN AND ANALYSIS OF ALGORITHMS",
            "CSE 207", "DIGITAL ELECTRONICS",
            "CSE 213", "ADVANCED JAVA PROGRAMMING",
            "FIC 120", "FOUNDATIONS OF ELECTRICAL AND ELECTRONICS ENGINEERING",
            "MCE 272", "INTRODUCTION TO ROCKET AND JET TECHNOLOGY",
            "SEC 203", "APTITUDE FOR ENGINEERS - III"
    );

    public static final Map<String, String> SUBJECT_FACULTY = Map.of(
            "CSE 203", "Dr. Jayasree Subramanian ( 19091 )",
            "CSE 204", "Dr. Roopa Tirumalasetti ( 24082 ), Mr. Sugyan Kumar Mishra ( 24032 )",
            "CSE 207", "Dr. Pudi Dhilleswararao ( 25369 ), Dr. Rishikanta Mayengbam ( 26372 )",
            "CSE 213", "Dr. Aditya Kumar Sahu ( 25346 ), Mr. Balvinder Singh Bondili ( 24101 )",
            "FIC 120", "Dr. Bibaswan Bose ( 26369 )",
            "MCE 272", "Dr. Vipin Kumar ( 26083 )",
            "SEC 203", "Mr. Skilling Course Six Two ( tmp062 )"
    );

    public static final Map<String, String> SUBJECT_ROOMS = Map.of(
            "CSE 203", "(S 404)",
            "CSE 204", "(C 305), (S 404)",
            "CSE 207", "(C 706), (S 404), (X 201)",
            "CSE 213", "(C 305), (S 404)",
            "FIC 120", "(S 404), (S 411)",
            "MCE 272", "(C 609), (C 902)",
            "SEC 203", "(S 404)"
    );

    public static final Map<String, String> SUBJECT_LTP = Map.of(
            "CSE 203", "3-0-0-3",
            "CSE 204", "3-0-1-4",
            "CSE 207", "3-0-1-4",
            "CSE 213", "3-0-1-4",
            "FIC 120", "2-0-1-3",
            "MCE 272", "3-0-0-3",
            "SEC 203", "1-0-1-2"
    );

    public List<TimetableSlotDto> getCommonTimetable() {
        List<TimetableSlotDto> slots = new ArrayList<>();

        // Monday
        slots.add(createSlot("Monday", 1, "09:00 To 09:50", "CSE 204", "(C 305)"));
        slots.add(createSlot("Monday", 2, "10:00 To 10:50", "CSE 204", "(C 305)"));
        slots.add(createSlot("Monday", 5, "01:00 To 01:50", "CSE 213", "(S 404)"));
        slots.add(createSlot("Monday", 6, "02:00 To 02:50", "SEC 203", "(S 404)"));
        slots.add(createSlot("Monday", 7, "03:00 To 03:50", "SEC 203", "(S 404)"));
        slots.add(createSlot("Monday", 8, "04:00 To 05:30", "MCE 272", "(C 902)"));

        // Tuesday
        slots.add(createSlot("Tuesday", 1, "09:00 To 09:50", "FIC 120", "(S 411)"));
        slots.add(createSlot("Tuesday", 2, "10:00 To 10:50", "FIC 120", "(S 411)"));
        slots.add(createSlot("Tuesday", 5, "01:00 To 01:50", "FIC 120", "(S 404)"));
        slots.add(createSlot("Tuesday", 6, "02:00 To 02:50", "FIC 120", "(S 404)"));
        slots.add(createSlot("Tuesday", 8, "04:00 To 05:30", "MCE 272", "(C 609)"));

        // Wednesday
        slots.add(createSlot("Wednesday", 2, "10:00 To 10:50", "CSE 204", "(S 404)"));
        slots.add(createSlot("Wednesday", 5, "01:00 To 01:50", "CSE 207", "(C 706)"));
        slots.add(createSlot("Wednesday", 6, "02:00 To 02:50", "CSE 207", "(C 706)"));

        // Thursday
        slots.add(createSlot("Thursday", 1, "09:00 To 09:50", "CSE 213", "(S 404)"));
        slots.add(createSlot("Thursday", 2, "10:00 To 10:50", "CSE 213", "(S 404)"));
        slots.add(createSlot("Thursday", 5, "01:00 To 01:50", "CSE 207", "(X 201)"));
        slots.add(createSlot("Thursday", 6, "02:00 To 02:50", "CSE 207", "(X 201)"));
        slots.add(createSlot("Thursday", 7, "03:00 To 03:50", "CSE 203", "(S 404)"));
        slots.add(createSlot("Thursday", 8, "04:00 To 05:30", "SEC 203", "(S 404)"));

        // Friday
        slots.add(createSlot("Friday", 1, "09:00 To 09:50", "CSE 213", "(C 305)"));
        slots.add(createSlot("Friday", 2, "10:00 To 10:50", "CSE 213", "(C 305)"));
        slots.add(createSlot("Friday", 3, "11:00 To 11:50", "CSE 207", "(S 404)"));
        slots.add(createSlot("Friday", 5, "01:00 To 01:50", "CSE 204", "(S 404)"));
        slots.add(createSlot("Friday", 6, "02:00 To 02:50", "CSE 204", "(S 404)"));
        slots.add(createSlot("Friday", 7, "03:00 To 03:50", "CSE 203", "(S 404)"));
        slots.add(createSlot("Friday", 8, "04:00 To 05:30", "CSE 203", "(S 404)"));

        return slots;
    }

    private TimetableSlotDto createSlot(String day, int slotNum, String timeRange, String code, String room) {
        return TimetableSlotDto.builder()
                .day(day)
                .slotNumber(slotNum)
                .timeRange(timeRange)
                .courseCode(code)
                .courseName(SUBJECT_NAMES.getOrDefault(code, code))
                .classroom(room)
                .facultyName(SUBJECT_FACULTY.getOrDefault(code, "Faculty"))
                .ltp(SUBJECT_LTP.getOrDefault(code, "3-0-0-3"))
                .build();
    }
}
