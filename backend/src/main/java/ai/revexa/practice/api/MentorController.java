package ai.revexa.practice.api;

import ai.revexa.core.security.CurrentUser;
import ai.revexa.practice.dto.PracticeDtos;
import ai.revexa.practice.service.ChatService;
import ai.revexa.practice.service.HintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The mentoring surfaces: the progressive hint ladder and the context-aware chat. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Mentor")
public class MentorController {

    private final HintService hints;
    private final ChatService chat;

    public MentorController(HintService hints, ChatService chat) {
        this.hints = hints;
        this.chat = chat;
    }

    // ----------------------------------------------------------------- hints

    @PostMapping("/hints/sessions")
    @Operation(summary = "Open (or resume) the hint ladder for a problem")
    public PracticeDtos.HintSessionView startHints(
            @Valid @RequestBody PracticeDtos.StartHintSessionRequest request) {
        return hints.start(request, CurrentUser.requireId());
    }

    @GetMapping("/hints/sessions/{id}")
    @Operation(summary = "The current rung and every hint revealed so far")
    public PracticeDtos.HintSessionView getHints(@PathVariable UUID id) {
        return hints.get(id, CurrentUser.requireId());
    }

    @PostMapping("/hints/sessions/{id}/next")
    @Operation(summary = "Advance exactly one rung; the final rung needs revealSolution=true")
    public PracticeDtos.HintSessionView nextHint(
            @PathVariable UUID id, @Valid @RequestBody PracticeDtos.NextHintRequest request) {
        return hints.next(id, request, CurrentUser.requireId());
    }

    // ------------------------------------------------------------------ chat

    @PostMapping("/chat/threads")
    @Operation(summary = "Start a conversation anchored to a problem")
    public ResponseEntity<PracticeDtos.ChatThreadView> startThread(
            @Valid @RequestBody PracticeDtos.StartThreadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chat.start(request, CurrentUser.requireId()));
    }

    @GetMapping("/chat/threads")
    @Operation(summary = "Your conversations")
    public List<PracticeDtos.ChatThreadView> threads(@RequestParam(required = false) UUID problemId) {
        return chat.list(CurrentUser.requireId(), problemId);
    }

    @GetMapping("/chat/threads/{id}")
    @Operation(summary = "One conversation with its full message history")
    public PracticeDtos.ChatThreadView thread(@PathVariable UUID id) {
        return chat.get(id, CurrentUser.requireId());
    }

    @PostMapping("/chat/threads/{id}/messages")
    @Operation(summary = "Send a message; spoilers stay closed unless this turn asks for them")
    public PracticeDtos.ChatTurn send(
            @PathVariable UUID id, @Valid @RequestBody PracticeDtos.SendMessageRequest request) {
        return chat.send(id, request, CurrentUser.requireId());
    }
}
