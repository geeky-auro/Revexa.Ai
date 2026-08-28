package ai.revexa.intelligence;

import static org.assertj.core.api.Assertions.assertThat;

import ai.revexa.intelligence.heuristic.Complexity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ComplexityTest {

    @Test
    @DisplayName("normalises the many spellings of the same bound")
    void normalises() {
        assertThat(Complexity.normalize("O(N)")).isEqualTo(Complexity.LINEAR);
        assertThat(Complexity.normalize("o(n log n)")).isEqualTo(Complexity.LINEARITHMIC);
        assertThat(Complexity.normalize("O(n*log n)")).isEqualTo(Complexity.LINEARITHMIC);
        assertThat(Complexity.normalize("O(n^2)")).isEqualTo(Complexity.QUADRATIC);
        assertThat(Complexity.normalize("O(2^n)")).isEqualTo(Complexity.EXPONENTIAL);
    }

    @Test
    @DisplayName("orders bounds so 'is this better?' is a comparison")
    void orders() {
        assertThat(Complexity.isBetter(Complexity.LINEAR, Complexity.QUADRATIC)).isTrue();
        assertThat(Complexity.isBetter(Complexity.LOG, Complexity.LINEAR)).isTrue();
        assertThat(Complexity.isWorse(Complexity.EXPONENTIAL, Complexity.CUBIC)).isTrue();
        assertThat(Complexity.isBetter(Complexity.LINEAR, "O(N)")).isFalse();
    }

    @Test
    @DisplayName("maps loop nesting to a bound")
    void mapsLoopDepth() {
        assertThat(Complexity.forLoopDepth(0)).isEqualTo(Complexity.CONSTANT);
        assertThat(Complexity.forLoopDepth(1)).isEqualTo(Complexity.LINEAR);
        assertThat(Complexity.forLoopDepth(2)).isEqualTo(Complexity.QUADRATIC);
        assertThat(Complexity.forLoopDepth(3)).isEqualTo(Complexity.CUBIC);
    }
}
