package ai.revexa.progress.api;

import ai.revexa.core.security.CurrentUser;
import ai.revexa.identity.api.AuthService;
import ai.revexa.progress.dto.ProgressDtos;
import ai.revexa.progress.service.ProgressService;
import ai.revexa.progress.service.RecommendationService;
import ai.revexa.progress.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/progress")
@Tag(name = "Progress")
public class ProgressController {

    private final ProgressService progress;
    private final RecommendationService recommendations;
    private final ReportService reports;
    private final AuthService auth;

    public ProgressController(
            ProgressService progress,
            RecommendationService recommendations,
            ReportService reports,
            AuthService auth) {
        this.progress = progress;
        this.recommendations = recommendations;
        this.reports = reports;
        this.auth = auth;
    }

    @GetMapping("/summary")
    @Operation(summary = "Dashboard metrics: mastery, mistakes, trend, complexity mix, hint dependency")
    public ProgressDtos.ProgressSummary summary() {
        return progress.summary(CurrentUser.requireId());
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Personalised next actions grounded in your review history")
    public ProgressDtos.RecommendationBundle recommendations() {
        return recommendations.recommend(CurrentUser.requireId());
    }

    @GetMapping("/report")
    @Operation(summary = "A full learning report, including a markdown rendering")
    public ProgressDtos.LearningReport report() {
        return reports.generate(auth.require(CurrentUser.requireId()));
    }

    @GetMapping(value = "/report/export", produces = "text/markdown")
    @Operation(summary = "Download the learning report as a markdown file")
    public ResponseEntity<String> exportReport() {
        ProgressDtos.LearningReport report = reports.generate(auth.require(CurrentUser.requireId()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"revexa-learning-report.md\"")
                .contentType(MediaType.valueOf("text/markdown"))
                .body(report.markdown());
    }
}
