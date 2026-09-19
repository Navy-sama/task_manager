package io.github.navysama.taskmanager.shared.error;

import org.springframework.http.ProblemDetail;

/** Factory for RFC 9457 Problem Details carrying the application's {@code code} extension. */
public final class Problems {

    public static final String CODE_PROPERTY = "code";
    public static final String ERRORS_PROPERTY = "errors";

    private Problems() {}

    public static ProblemDetail of(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setProperty(CODE_PROPERTY, code.name());
        return problem;
    }

    static boolean hasCode(ProblemDetail problem) {
        return problem.getProperties() != null && problem.getProperties().containsKey(CODE_PROPERTY);
    }
}
