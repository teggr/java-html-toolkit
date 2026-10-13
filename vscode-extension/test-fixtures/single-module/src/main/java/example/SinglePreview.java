package example;

import io.github.teggr.html.preview.Preview;

public class SinglePreview {
    @Preview("Single module")
    public String render() {
        return "<main>single module</main>";
    }
}
