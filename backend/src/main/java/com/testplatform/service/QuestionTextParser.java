package com.testplatform.service;

import com.testplatform.dto.QuestionDto;
import com.testplatform.exception.ApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses questions from a simple text format:
 *
 * Q: What is 2+2?
 * A) 3
 * B) 4
 * C) 5
 * D) 6
 * ANSWER: B
 *
 * Blocks are separated by one or more blank lines.
 */
public class QuestionTextParser {

    public static List<QuestionDto> parse(String content) {
        List<QuestionDto> questions = new ArrayList<>();
        if (content == null || content.isBlank()) {
            throw ApiException.badRequest("Uploaded file is empty");
        }

        String[] blocks = content.replace("\r\n", "\n").split("\n\\s*\n");
        int blockNum = 0;

        for (String block : blocks) {
            if (block.isBlank()) continue;
            blockNum++;
            String[] lines = block.strip().split("\n");

            QuestionDto q = new QuestionDto();
            for (String rawLine : lines) {
                String line = rawLine.strip();
                if (line.isEmpty()) continue;

                if (line.regionMatches(true, 0, "Q:", 0, 2)) {
                    q.setText(line.substring(2).strip());
                } else if (line.regionMatches(true, 0, "A)", 0, 2)) {
                    q.setOptionA(line.substring(2).strip());
                } else if (line.regionMatches(true, 0, "B)", 0, 2)) {
                    q.setOptionB(line.substring(2).strip());
                } else if (line.regionMatches(true, 0, "C)", 0, 2)) {
                    q.setOptionC(line.substring(2).strip());
                } else if (line.regionMatches(true, 0, "D)", 0, 2)) {
                    q.setOptionD(line.substring(2).strip());
                } else if (line.regionMatches(true, 0, "ANSWER:", 0, 7)) {
                    q.setCorrectOption(line.substring(7).strip().toUpperCase());
                }
            }

            validate(q, blockNum);
            questions.add(q);
        }

        if (questions.isEmpty()) {
            throw ApiException.badRequest("No valid questions found in the uploaded file");
        }
        return questions;
    }

    private static void validate(QuestionDto q, int blockNum) {
        if (isBlank(q.getText())) throw ApiException.badRequest("Question " + blockNum + " is missing text (Q:)");
        if (isBlank(q.getOptionA()) || isBlank(q.getOptionB()) || isBlank(q.getOptionC()) || isBlank(q.getOptionD())) {
            throw ApiException.badRequest("Question " + blockNum + " must have options A), B), C) and D)");
        }
        if (isBlank(q.getCorrectOption()) || !List.of("A", "B", "C", "D").contains(q.getCorrectOption())) {
            throw ApiException.badRequest("Question " + blockNum + " must have ANSWER: A/B/C/D");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
