export function injectCssIntoHtml(html: string, cssContent: string): string {
    if (!cssContent) {
        return html;
    }

    const isFullDocument = /<html[>\s]/i.test(html) || /<head[>\s]/i.test(html);
    if (!isFullDocument) {
        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    ${cssContent}
</head>
<body>
    ${html}
</body>
</html>`;
    }

    const headMatch = html.match(/<head[^>]*>/i);
    if (headMatch?.index !== undefined) {
        const headEndIndex = headMatch.index + headMatch[0].length;
        return html.slice(0, headEndIndex) + '\n    ' + cssContent + html.slice(headEndIndex);
    }

    const htmlMatch = html.match(/<html[^>]*>/i);
    if (htmlMatch?.index !== undefined) {
        const htmlEndIndex = htmlMatch.index + htmlMatch[0].length;
        return html.slice(0, htmlEndIndex) + `\n<head>\n    ${cssContent}\n</head>` + html.slice(htmlEndIndex);
    }

    return html;
}
