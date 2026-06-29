package dev.rebelcraft.html.preview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreviewRunnerTest {

    // ---------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------

    /** A method that returns a plain HTML string. */
    public static class StringPreview {
        @Preview("Hello World")
        public String hello() {
            return "<h1>Hello, World!</h1>";
        }
    }

    /** Mimics a non-String preview payload object. */
    public static class FakeDomContent {
        public String render() {
            return "<p>rendered</p>";
        }

        @Override
        public String toString() {
            return render();
        }
    }

    public static class DomContentPreview {
        @Preview
        public FakeDomContent page() {
            return new FakeDomContent();
        }
    }

    public static class PlainObjectPreview {
        @Preview
        public Object plain() {
            return new Object() {
                @Override
                public String toString() {
                    return "plain-object";
                }
            };
        }
    }

    /** A method that returns {@code null}. */
    public static class NullPreview {
        @Preview
        public String empty() {
            return null;
        }
    }

    /** A method that is NOT annotated with {@code @Preview}. */
    public static class NoAnnotation {
        public String hello() {
            return "<h1>Hello</h1>";
        }
    }

    static class HiddenPreview {
        @Preview
        private String hidden() {
            return "<p>hidden</p>";
        }
    }

    static class HiddenDomContentPreview {
        @Preview
        private FakeDomContent extracted() {
            return new FakeDomContent();
        }
    }

    static class AccessFailingDomContentPreview {
        @Preview
        private FakeDomContent accessFails() {
            return new FakeDomContent();
        }
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    void runsStringReturningMethod() throws Exception {
        String output = PreviewRunner.run(new String[]{
                StringPreview.class.getName(), "hello"
        });
        assertEquals("<h1>Hello, World!</h1>", output);
    }

    @Test
    void normalizesParenthesizedMethodNames() throws Exception {
        String output = PreviewRunner.run(new String[]{
                StringPreview.class.getName(), "hello()"
        });
        assertEquals("<h1>Hello, World!</h1>", output);
    }

    @Test
    void runsPrivateMethodOnPackagePrivateClass() throws Exception {
        String output = PreviewRunner.run(new String[]{
                HiddenPreview.class.getName(), "hidden"
        });
        assertEquals("<p>hidden</p>", output);
    }

    @Test
    void runsDomContentReturningMethod() throws Exception {
        String output = PreviewRunner.run(new String[]{
                DomContentPreview.class.getName(), "page"
        });
        assertEquals("high-priority-generator", output);
    }

    @Test
    void runsPackagePrivateDomContentMethodViaObjectFallback() throws Exception {
        String output = PreviewRunner.run(new String[]{
                HiddenDomContentPreview.class.getName(), "extracted"
        });
        assertEquals("<p>rendered</p>", output);
    }

    @Test
    void fallsBackWhenHigherPriorityGeneratorHasAccessError() throws Exception {
        String output = PreviewRunner.run(new String[]{
                AccessFailingDomContentPreview.class.getName(), "accessFails"
        });
        assertEquals("<p>rendered</p>", output);
    }

    @Test
    void runsPlainObjectMethodViaObjectFallback() throws Exception {
        String output = PreviewRunner.run(new String[]{
                PlainObjectPreview.class.getName(), "plain"
        });
        assertEquals("plain-object", output);
    }

    @Test
    void returnsEmptyStringForNullResult() throws Exception {
        String output = PreviewRunner.run(new String[]{
                NullPreview.class.getName(), "empty"
        });
        assertEquals("", output);
    }

    @Test
    void throwsWhenAnnotationMissing() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PreviewRunner.run(new String[]{
                        NoAnnotation.class.getName(), "hello"
                }));
        assertTrue(ex.getMessage().contains("@Preview"));
    }

    @Test
    void throwsWhenTooFewArguments() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PreviewRunner.run(new String[]{}));
        assertTrue(ex.getMessage().contains("Usage"));
    }
}