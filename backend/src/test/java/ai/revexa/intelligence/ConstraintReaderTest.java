package ai.revexa.intelligence;

import static org.assertj.core.api.Assertions.assertThat;

import ai.revexa.intelligence.heuristic.Complexity;
import ai.revexa.intelligence.heuristic.ConstraintReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConstraintReaderTest {

    private final ConstraintReader reader = new ConstraintReader();

    @Test
    @DisplayName("prefers the bound on input size over the bound on values")
    void prefersSizeBound() {
        ConstraintReader.Constraints constraints =
                reader.read(
                        """
                        Constraints:
                        2 <= nums.length <= 10^4
                        -10^9 <= nums[i] <= 10^9
                        -10^9 <= target <= 10^9
                        """,
                        null);

        assertThat(constraints.maxN()).isEqualTo(10_000L);
        assertThat(constraints.budget()).isEqualTo(Complexity.LINEARITHMIC);
    }

    @Test
    @DisplayName("reads a multiplied bound such as 5 * 10^4")
    void readsMultipliedBound() {
        ConstraintReader.Constraints constraints =
                reader.read("Constraints:\n0 <= s.length <= 5 * 10^4\n", null);
        assertThat(constraints.maxN()).isEqualTo(50_000L);
    }

    @Test
    @DisplayName("a tiny bound legitimises exponential work")
    void tinyBoundAllowsExponential() {
        assertThat(reader.read("Constraints:\n1 <= n <= 20\n", null).budget()).isEqualTo(Complexity.EXPONENTIAL);
    }

    @Test
    @DisplayName("says so plainly when no bound is stated")
    void handlesMissingConstraints() {
        ConstraintReader.Constraints constraints = reader.read("Return the sum of the array.", null);
        assertThat(constraints.maxN()).isZero();
        assertThat(constraints.budget()).isEmpty();
        assertThat(constraints.note()).contains("No explicit bound");
    }

    @Test
    @DisplayName("keeps the minus sign on a negative lower bound")
    void doesNotStripNegativeSigns() {
        ConstraintReader.Constraints constraints =
                reader.read(
                        """
                        Constraints:
                        2 <= nums.length <= 10^4
                        -10^9 <= nums[i] <= 10^9
                        """,
                        null);

        assertThat(constraints.lines()).contains("-10^9 <= nums[i] <= 10^9");
    }
}
