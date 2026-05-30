package com.ghostcoach.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedbackDto {
    private Integer overallScore;
    private List<String> strengths;
    private List<String> areasToImprove;
    private String priorityFix;
    private String drillSuggestion;
    private String confidenceLevel;
}
