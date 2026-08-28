package ai.revexa.practice.service;

import ai.revexa.catalog.domain.Problem;
import ai.revexa.intelligence.pipeline.StageContext;
import org.springframework.stereotype.Component;

/** Builds the pipeline input from a stored problem plus whatever code the caller supplied. */
@Component
public class StageContextFactory {

    public StageContext.Builder from(Problem problem) {
        return StageContext.builder()
                .problemTitle(problem.getTitle())
                .problemStatement(problem.getStatement())
                .constraintsText(problem.getConstraintsText())
                .topics(problem.getTopics())
                .difficulty(problem.getDifficulty() == null ? "" : problem.getDifficulty().name());
    }

    public StageContext forCode(Problem problem, String code, String language) {
        return from(problem).code(code == null ? "" : code).language(language == null ? "" : language).build();
    }
}
