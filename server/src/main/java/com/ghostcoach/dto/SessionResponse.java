package com.ghostcoach.dto;

import com.ghostcoach.model.CoachingSession;
import com.ghostcoach.model.SportType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponse {
    private Long id;
    private String imagePath;
    private Integer overallScore;
    private String strengths;
    private String areasToImprove;
    private String priorityFix;
    private String drillSuggestion;
    private String confidenceLevel;
    private LocalDateTime createdAt;
    private SportType sport;
    private FeedbackDto feedback;

    public static SessionResponse from(CoachingSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .imagePath(session.getImagePath())
                .overallScore(session.getOverallScore())
                .strengths(session.getStrengths())
                .areasToImprove(session.getAreasToImprove())
                .priorityFix(session.getPriorityFix())
                .drillSuggestion(session.getDrillSuggestion())
                .confidenceLevel(session.getConfidenceLevel())
                .createdAt(session.getCreatedAt())
                .sport(session.getSport())
                .build();
    }

    public static SessionResponse fromWithFeedback(CoachingSession session, FeedbackDto feedback) {
        SessionResponse res = from(session);
        res.setFeedback(feedback);
        return res;
    }
}
