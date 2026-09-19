package io.github.navysama.taskmanager;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Fails the build when a module reaches into another module's internals or declares an unallowed dependency. */
class ModularityTest {

    @Test
    void modules_shouldRespectDeclaredBoundaries() {
        ApplicationModules.of(TaskManagerApplication.class).verify();
    }
}
