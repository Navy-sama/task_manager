package io.github.navysama.taskmanager.task.internal.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TaskSpecificationsTest {

    @Test
    void containsPattern_shouldLowercaseTrimAndWrapWithWildcards() {
        assertThat(TaskSpecifications.containsPattern("  Buy Milk ")).isEqualTo("%buy milk%");
    }

    @Test
    void containsPattern_shouldEscapeLikeWildcardsAndTheEscapeCharacter() {
        assertThat(TaskSpecifications.containsPattern("50%_off!")).isEqualTo("%50!%!_off!!%");
    }
}
