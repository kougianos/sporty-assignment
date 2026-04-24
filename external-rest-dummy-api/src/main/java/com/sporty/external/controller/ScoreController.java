package com.sporty.external.controller;

import com.sporty.external.dto.ScoreResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private static final String[] SAMPLE_SCORES = {"0:0", "1:0", "0:1", "1:1", "2:1", "1:2", "2:0", "0:2", "3:1", "2:2"};

    @GetMapping("/{eventId}")
    public ScoreResponse getScore(@PathVariable String eventId) {
        var score = SAMPLE_SCORES[ThreadLocalRandom.current().nextInt(SAMPLE_SCORES.length)];
        log.info("Returning score {} for event {}", score, eventId);
        return new ScoreResponse(eventId, score);
    }
}
