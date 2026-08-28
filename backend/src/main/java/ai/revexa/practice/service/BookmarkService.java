package ai.revexa.practice.service;

import ai.revexa.catalog.api.ProblemService;
import ai.revexa.core.error.ApiException;
import ai.revexa.practice.domain.Bookmark;
import ai.revexa.practice.domain.BookmarkRepository;
import ai.revexa.practice.dto.PracticeDtos;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookmarkService {

    private static final Set<String> KINDS =
            Set.of("REVIEW", "HINT", "CHAT", "COMPARISON", "INSIGHT", "COMPLEXITY");

    private final BookmarkRepository repository;
    private final ProblemService problems;

    public BookmarkService(BookmarkRepository repository, ProblemService problems) {
        this.repository = repository;
        this.problems = problems;
    }

    @Transactional
    public PracticeDtos.BookmarkView create(PracticeDtos.CreateBookmarkRequest request, UUID userId) {
        String kind = request.kind().toUpperCase(Locale.ROOT);
        if (!KINDS.contains(kind)) {
            throw ApiException.badRequest("Unknown bookmark kind. Expected one of: " + String.join(", ", KINDS));
        }
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(userId);
        bookmark.setKind(kind);
        bookmark.setProblemId(request.problemId());
        bookmark.setReferenceId(request.referenceId());
        bookmark.setTitle(request.title());
        bookmark.setContent(request.content());
        bookmark.setNote(request.note());
        return toView(repository.save(bookmark));
    }

    @Transactional(readOnly = true)
    public List<PracticeDtos.BookmarkView> list(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toView).toList();
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        Bookmark bookmark =
                repository.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Bookmark"));
        repository.delete(bookmark);
    }

    private PracticeDtos.BookmarkView toView(Bookmark bookmark) {
        return new PracticeDtos.BookmarkView(
                bookmark.getId(),
                bookmark.getKind(),
                bookmark.getProblemId(),
                bookmark.getProblemId() == null ? null : problems.titleOf(bookmark.getProblemId()),
                bookmark.getReferenceId(),
                bookmark.getTitle(),
                bookmark.getContent(),
                bookmark.getNote(),
                bookmark.getCreatedAt());
    }
}
