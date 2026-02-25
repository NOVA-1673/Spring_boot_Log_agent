package com.TroubleShoot.observability.domain.incident.grouping;

import com.troubleshoot.observability.domain.incident.grouping.ExceptionSignature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionSignatureTest {

    @Test
    void sameThrowableProducesSameHash() {
        Throwable throwable = createThrowable();

        ExceptionSignature first = ExceptionSignature.fromThrowable(throwable);
        ExceptionSignature second = ExceptionSignature.fromThrowable(throwable);

        assertThat(first.getSignatureHash()).isEqualTo(second.getSignatureHash());
    }

    @Test
    void togglingIncludeLineNumberChangesHash() {
        Throwable throwable = createThrowable();

        ExceptionSignature withLine = ExceptionSignature.builder()
                .includeLineNumber(true)
                .fromThrowable(throwable);
        ExceptionSignature withoutLine = ExceptionSignature.builder()
                .includeLineNumber(false)
                .fromThrowable(throwable);

        assertThat(withLine.getSignatureHash()).isNotEqualTo(withoutLine.getSignatureHash());
    }

    @Test
    void filteringRemovesJavaFrames() {
        RuntimeException throwable = new RuntimeException("boom");
        throwable.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("java.lang.String", "valueOf", "String.java", 12),
                new StackTraceElement("com.acme.Foo", "bar", "Foo.java", 42)
        });

        ExceptionSignature signature = ExceptionSignature.builder()
                .includeLineNumber(false)
                .filterJdk(true)
                .fromThrowable(throwable);

        assertThat(signature.getFrames()).hasSize(1);
        assertThat(signature.getFrames().get(0)).isEqualTo("com.acme.Foo#bar(Foo.java)");
    }

    @Test
    void usesRootCauseExceptionClassForThrowable() {
        IllegalArgumentException root = new IllegalArgumentException("root");
        RuntimeException wrapper = new RuntimeException("wrapper", root);

        ExceptionSignature signature = ExceptionSignature.fromThrowable(wrapper);

        assertThat(signature.getExceptionClassName()).isEqualTo(IllegalArgumentException.class.getName());
    }

    @Test
    void fromRawStacktraceProducesDeterministicHash() {
        String stacktrace = String.join("\n",
                "java.lang.IllegalArgumentException: bad input",
                "    at com.acme.Foo.bar(Foo.java:10)",
                "    at java.util.Objects.requireNonNull(Objects.java:246)"
        );

        ExceptionSignature first = ExceptionSignature.fromStacktrace(stacktrace);
        ExceptionSignature second = ExceptionSignature.fromStacktrace(stacktrace);

        assertThat(first.getSignatureHash()).isEqualTo(second.getSignatureHash());
    }

    private static Throwable createThrowable() {
        try {
            throw new IllegalStateException("boom");
        } catch (Exception ex) {
            return ex;
        }
    }
}
