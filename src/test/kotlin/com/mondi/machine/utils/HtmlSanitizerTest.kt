package com.mondi.machine.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The test class for [HtmlSanitizer].
 *
 * @author Ferdinand Sangap
 * @since 2026-01-24
 */
internal class HtmlSanitizerTest {

    private val htmlSanitizer = HtmlSanitizer()

    @Test
    fun `sanitize allows safe formatting tags`() {
        val html = "<p>This is <strong>bold</strong> and <em>italic</em> text.</p>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).isEqualTo("<p>This is <strong>bold</strong> and <em>italic</em> text.</p>")
    }

    @Test
    fun `sanitize removes script tags`() {
        val html = "<p>Safe content</p><script>alert('XSS')</script>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).isEqualTo("<p>Safe content</p>")
        assertThat(result).doesNotContain("script")
        assertThat(result).doesNotContain("alert")
    }

    @Test
    fun `sanitize removes onclick attributes`() {
        val html = "<p onclick=\"alert('XSS')\">Click me</p>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).isEqualTo("<p>Click me</p>")
        assertThat(result).doesNotContain("onclick")
    }

    @Test
    fun `sanitize removes javascript URLs`() {
        val html = "<a href=\"javascript:alert('XSS')\">Click me</a>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).doesNotContain("javascript:")
        assertThat(result).doesNotContain("alert")
    }

    @Test
    fun `sanitize allows headings`() {
        val html = "<h1>Title</h1><h2>Subtitle</h2><h3>Section</h3>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).contains("<h1>Title</h1>")
        assertThat(result).contains("<h2>Subtitle</h2>")
        assertThat(result).contains("<h3>Section</h3>")
    }

    @Test
    fun `sanitize allows lists`() {
        val html = "<ul><li>Item 1</li><li>Item 2</li></ul>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).contains("<ul>")
        assertThat(result).contains("<li>Item 1</li>")
        assertThat(result).contains("<li>Item 2</li>")
        assertThat(result).contains("</ul>")
    }

    @Test
    fun `sanitize allows ordered lists`() {
        val html = "<ol><li>First</li><li>Second</li></ol>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).contains("<ol>")
        assertThat(result).contains("<li>First</li>")
        assertThat(result).contains("</ol>")
    }

    @Test
    fun `sanitize allows tables`() {
        val html = """
            <table>
                <thead><tr><th>Header</th></tr></thead>
                <tbody><tr><td>Data</td></tr></tbody>
            </table>
        """.trimIndent()
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).contains("<table>")
        assertThat(result).contains("<thead>")
        assertThat(result).contains("<th>Header</th>")
        assertThat(result).contains("<td>Data</td>")
    }

    @Test
    fun `sanitize allows table colspan and rowspan`() {
        val html = "<table><tr><td colspan=\"2\" rowspan=\"2\">Cell</td></tr></table>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).contains("colspan=\"2\"")
        assertThat(result).contains("rowspan=\"2\"")
    }

    @Test
    fun `sanitize removes iframe tags`() {
        val html = "<p>Content</p><iframe src=\"evil.com\"></iframe>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).isEqualTo("<p>Content</p>")
        assertThat(result).doesNotContain("iframe")
    }

    @Test
    fun `sanitize removes object and embed tags`() {
        val html = "<p>Safe</p><object data=\"evil.swf\"></object><embed src=\"bad.swf\">"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).isEqualTo("<p>Safe</p>")
        assertThat(result).doesNotContain("object")
        assertThat(result).doesNotContain("embed")
    }

    @Test
    fun `sanitize removes style tags`() {
        val html = "<style>body { background: red; }</style><p>Content</p>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).isEqualTo("<p>Content</p>")
        assertThat(result).doesNotContain("style>")
    }

    @Test
    fun `sanitize handles null input`() {
        val result = htmlSanitizer.sanitize(null)

        assertThat(result).isNull()
    }

    @Test
    fun `sanitize handles empty string`() {
        val result = htmlSanitizer.sanitize("")

        assertThat(result).isEmpty()
    }

    @Test
    fun `sanitize handles blank string`() {
        val result = htmlSanitizer.sanitize("   ")

        assertThat(result).isEqualTo("   ")
    }

    @Test
    fun `sanitize allows code and pre tags`() {
        val html = "<pre><code>function test() { return true; }</code></pre>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).contains("<pre>")
        assertThat(result).contains("<code>")
        assertThat(result).contains("function test()")
    }

    @Test
    fun `sanitize removes data attributes`() {
        val html = "<p data-id=\"123\" data-custom=\"value\">Content</p>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).isEqualTo("<p>Content</p>")
        assertThat(result).doesNotContain("data-")
    }

    @Test
    fun `sanitize preserves nested tags`() {
        val html = "<p>This is <strong>bold with <em>italic</em> inside</strong></p>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).contains("<strong>")
        assertThat(result).contains("<em>")
        assertThat(result).contains("</em>")
        assertThat(result).contains("</strong>")
    }

    @Test
    fun `sanitize removes onerror attributes on img tags`() {
        val html = "<img src=\"image.jpg\" onerror=\"alert('XSS')\">"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).doesNotContain("onerror")
        assertThat(result).doesNotContain("alert")
    }

    @Test
    fun `sanitizeBasic allows only basic formatting`() {
        val html = "<h1>Title</h1><p>Text with <strong>bold</strong></p><script>alert('bad')</script>"
        val result = htmlSanitizer.sanitizeBasic(html)

        assertThat(result).contains("<h1>Title</h1>")
        assertThat(result).contains("<p>")
        assertThat(result).contains("<strong>bold</strong>")
        assertThat(result).doesNotContain("script")
    }

    @Test
    fun `stripHtml removes all HTML tags`() {
        val html = "<p>This is <strong>bold</strong> text with <a href=\"test\">link</a>.</p>"
        val result = htmlSanitizer.stripHtml(html)

        assertThat(result).isEqualTo("This is bold text with link.")
        assertThat(result).doesNotContain("<")
        assertThat(result).doesNotContain(">")
    }

    @Test
    fun `stripHtml handles null input`() {
        val result = htmlSanitizer.stripHtml(null)

        assertThat(result).isNull()
    }

    @Test
    fun `sanitize protects against XSS in attributes`() {
        val html = "<p title=\"\" onload=\"alert('XSS')\">Content</p>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).isEqualTo("<p>Content</p>")
        assertThat(result).doesNotContain("onload")
    }

    @Test
    fun `sanitize handles malformed HTML gracefully`() {
        val html = "<p>Unclosed paragraph<div>Nested div</p></div>"
        val result = htmlSanitizer.sanitize(html)

        // jsoup will fix malformed HTML
        assertThat(result).isNotNull
        assertThat(result).contains("Unclosed paragraph")
        assertThat(result).contains("Nested div")
    }

    @Test
    fun `sanitize allows blockquote`() {
        val html = "<blockquote>This is a quote</blockquote>"
        val result = htmlSanitizer.sanitize(html)

        assertThat(result).contains("<blockquote>")
        assertThat(result).contains("This is a quote")
        assertThat(result).contains("</blockquote>")
    }

    @Test
    fun `sanitize allows hr tag`() {
        val html = "<p>First section</p><hr><p>Second section</p>"
        val result = htmlSanitizer.sanitize(html)

        // OWASP outputs XHTML-style self-closing tags
        assertThat(result).containsAnyOf("<hr>", "<hr />")
    }

    @Test
    fun `sanitize complex product specification`() {
        val html = """
            <h2>Product Specifications</h2>
            <ul>
                <li><strong>Material:</strong> 14k Gold</li>
                <li><strong>Weight:</strong> 5.2g</li>
                <li><strong>Size:</strong> Adjustable</li>
            </ul>
            <table>
                <thead><tr><th>Property</th><th>Value</th></tr></thead>
                <tbody>
                    <tr><td>Purity</td><td>14k</td></tr>
                    <tr><td>Color</td><td>Yellow Gold</td></tr>
                </tbody>
            </table>
            <p><em>Note:</em> Comes with certificate of authenticity</p>
        """.trimIndent()

        val result = htmlSanitizer.sanitize(html)

        assertThat(result).contains("<h2>Product Specifications</h2>")
        assertThat(result).contains("<ul>")
        assertThat(result).contains("<strong>Material:</strong>")
        assertThat(result).contains("<table>")
        assertThat(result).contains("<em>Note:</em>")
        assertThat(result).doesNotContain("script")
    }
}
