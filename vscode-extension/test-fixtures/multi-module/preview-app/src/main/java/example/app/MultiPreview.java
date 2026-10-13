package example.app;

import example.support.Markup;
import io.github.teggr.html.preview.Preview;

public class MultiPreview {
    @Preview("Multi module")
    public String render() {
        return Markup.heading("multi module");
    }
}
