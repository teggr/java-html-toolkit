package example.support;

public final class Markup {
    private Markup() {
    }

    public static String heading(String text) {
        return "<h1>" + text + "</h1>";
    }
}
