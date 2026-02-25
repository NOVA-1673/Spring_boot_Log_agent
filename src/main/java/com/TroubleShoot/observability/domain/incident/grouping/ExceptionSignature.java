package com.troubleshoot.observability.domain.incident.grouping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExceptionSignature {

    private static final int DEFAULT_TOP_FRAMES = 5;
    private static final boolean DEFAULT_INCLUDE_LINE_NUMBER = false;
    private static final boolean DEFAULT_FILTER_JDK = true;
    private static final boolean DEFAULT_FILTER_SPRING = false;

    private static final Pattern STACKTRACE_LINE =
            Pattern.compile("^\\s*at\\s+(.+)\\((.+)\\)$");

    private final String exceptionClassName;
    private final List<String> frames;
    private final String signatureString;
    private final String signatureHash;

    private ExceptionSignature(String exceptionClassName, List<String> frames) {
        this.exceptionClassName = exceptionClassName;
        this.frames = Collections.unmodifiableList(frames);
        this.signatureString = exceptionClassName + "\n" + String.join("\n", frames);
        this.signatureHash = sha256Hex(this.signatureString);
    }

    public static ExceptionSignature fromThrowable(Throwable throwable) {
        return builder().fromThrowable(throwable);
    }

    public static ExceptionSignature fromStacktrace(String stacktrace) {
        return builder().fromStacktrace(stacktrace);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getExceptionClassName() {
        return exceptionClassName;
    }

    public List<String> getFrames() {
        return frames;
    }

    public String getSignatureString() {
        return signatureString;
    }

    public String getSignatureHash() {
        return signatureHash;
    }

    public static final class Builder {
        private int topFrames = DEFAULT_TOP_FRAMES;
        private boolean includeLineNumber = DEFAULT_INCLUDE_LINE_NUMBER;
        private boolean filterJdk = DEFAULT_FILTER_JDK;
        private boolean filterSpring = DEFAULT_FILTER_SPRING;

        public Builder topFrames(int topFrames) {
            if (topFrames <= 0) {
                throw new IllegalArgumentException("topFrames must be positive");
            }
            this.topFrames = topFrames;
            return this;
        }

        public Builder includeLineNumber(boolean includeLineNumber) {
            this.includeLineNumber = includeLineNumber;
            return this;
        }

        public Builder filterJdk(boolean filterJdk) {
            this.filterJdk = filterJdk;
            return this;
        }

        public Builder filterSpring(boolean filterSpring) {
            this.filterSpring = filterSpring;
            return this;
        }

        public ExceptionSignature fromThrowable(Throwable throwable) {
            Objects.requireNonNull(throwable, "throwable");
            Throwable root = rootCauseOf(throwable);
            String exceptionClassName = root.getClass().getName();
            StackTraceElement[] stackTrace = root.getStackTrace();
            List<String> frames = normalizeFrames(toFrames(stackTrace));
            return new ExceptionSignature(exceptionClassName, frames);
        }

        public ExceptionSignature fromStacktrace(String stacktrace) {
            Objects.requireNonNull(stacktrace, "stacktrace");
            List<String> lines = splitLines(stacktrace);
            String exceptionClassName = extractExceptionClassName(lines);
            List<Frame> frames = parseFrames(lines);
            List<String> normalized = normalizeFrames(frames);
            return new ExceptionSignature(exceptionClassName, normalized);
        }

        private List<String> normalizeFrames(List<Frame> frames) {
            List<String> normalized = new ArrayList<>();
            for (Frame frame : frames) {
                if (shouldFilter(frame.className)) {
                    continue;
                }
                normalized.add(formatFrame(frame));
                if (normalized.size() >= topFrames) {
                    break;
                }
            }
            return normalized;
        }

        private boolean shouldFilter(String className) {
            if (className == null) {
                return false;
            }
            if (filterJdk && (className.startsWith("java.")
                    || className.startsWith("jdk.")
                    || className.startsWith("sun."))) {
                return true;
            }
            return filterSpring && className.startsWith("org.springframework.");
        }

        private String formatFrame(Frame frame) {
            String fileName = frame.fileName != null ? frame.fileName : "Unknown Source";
            if (includeLineNumber && frame.lineNumber >= 0) {
                return frame.className + "#" + frame.methodName + "(" + fileName + ":" + frame.lineNumber + ")";
            }
            if (includeLineNumber) {
                return frame.className + "#" + frame.methodName + "(" + fileName + ":?)";
            }
            return frame.className + "#" + frame.methodName + "(" + fileName + ")";
        }
    }

    private static List<Frame> toFrames(StackTraceElement[] elements) {
        List<Frame> frames = new ArrayList<>();
        for (StackTraceElement element : elements) {
            frames.add(new Frame(
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber()));
        }
        return frames;
    }

    private static List<String> splitLines(String stacktrace) {
        String[] raw = stacktrace.split("\\r?\\n");
        List<String> lines = new ArrayList<>(raw.length);
        for (String line : raw) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String extractExceptionClassName(List<String> lines) {
        if (lines.isEmpty()) {
            return "UnknownException";
        }
        String first = lines.get(0).trim();
        if (first.startsWith("Exception in thread")) {
            int lastSpace = first.lastIndexOf(' ');
            if (lastSpace > 0 && lastSpace < first.length() - 1) {
                return first.substring(lastSpace + 1).trim();
            }
        }
        int colon = first.indexOf(':');
        if (colon >= 0) {
            return first.substring(0, colon).trim();
        }
        return first;
    }

    private static List<Frame> parseFrames(List<String> lines) {
        List<Frame> frames = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = STACKTRACE_LINE.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String methodFull = matcher.group(1).trim();
            String filePart = matcher.group(2).trim();
            int lastDot = methodFull.lastIndexOf('.');
            String className;
            String methodName;
            if (lastDot > 0) {
                className = methodFull.substring(0, lastDot);
                methodName = methodFull.substring(lastDot + 1);
            } else {
                className = methodFull;
                methodName = "unknown";
            }

            String fileName = filePart;
            int lineNumber = -1;
            int colon = filePart.lastIndexOf(':');
            if (colon > 0) {
                fileName = filePart.substring(0, colon);
                String lineText = filePart.substring(colon + 1);
                try {
                    lineNumber = Integer.parseInt(lineText);
                } catch (NumberFormatException ignored) {
                    lineNumber = -1;
                }
            }
            frames.add(new Frame(className, methodName, fileName, lineNumber));
        }
        return frames;
    }

    private static Throwable rootCauseOf(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static final class Frame {
        private final String className;
        private final String methodName;
        private final String fileName;
        private final int lineNumber;

        private Frame(String className, String methodName, String fileName, int lineNumber) {
            this.className = className;
            this.methodName = methodName;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
        }
    }
}
