package com.mondi.machine.utils

import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory
import org.owasp.html.Sanitizers
import org.springframework.stereotype.Component

/**
 * HTML sanitizer to prevent XSS attacks using OWASP Java HTML Sanitizer.
 * Uses OWASP's PolicyFactory to clean HTML content and allow only safe tags and attributes.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-24
 */
@Component
class HtmlSanitizer {

    private val policy: PolicyFactory = createPolicy()
    private val basicPolicy: PolicyFactory = createBasicPolicy()

    /**
     * Sanitizes HTML content by removing potentially dangerous tags and attributes.
     * Only allows safe formatting tags suitable for product specifications.
     *
     * @param html the raw HTML content to sanitize
     * @return the sanitized HTML content, or null if input is null
     */
    fun sanitize(html: String?): String? {
        if (html.isNullOrBlank()) {
            return html
        }

        return policy.sanitize(html)
    }

    /**
     * Creates a comprehensive policy for product specifications.
     * This policy allows rich formatting suitable for e-commerce product descriptions.
     *
     * Allowed tags:
     * - Text formatting: p, br, strong, b, em, i, u, s, mark, small, sub, sup
     * - Headings: h1, h2, h3, h4, h5, h6
     * - Lists: ul, ol, li
     * - Tables: table, thead, tbody, tfoot, tr, th, td, caption
     * - Other: blockquote, code, pre, hr, div, span
     *
     * Allowed attributes:
     * - class: on all tags (for styling)
     * - style: limited to safe CSS properties
     * - colspan, rowspan: on table cells
     * - start: on ordered lists
     */
    private fun createPolicy(): PolicyFactory {
        // Build a custom policy with all needed elements and attributes
        return HtmlPolicyBuilder()
            // Text formatting
            .allowElements("p", "br", "strong", "b", "em", "i", "u", "s", "mark", "small", "sub", "sup")
            // Headings
            .allowElements("h1", "h2", "h3", "h4", "h5", "h6")
            // Lists
            .allowElements("ul", "ol", "li")
            .allowAttributes("start").onElements("ol")
            // Tables with colspan and rowspan
            .allowElements("table", "thead", "tbody", "tfoot", "tr", "th", "td", "caption")
            .allowAttributes("colspan", "rowspan").onElements("td", "th")
            // Other semantic elements
            .allowElements("blockquote", "code", "pre", "hr", "div", "span")
            // Allow class attribute globally for styling
            .allowAttributes("class").globally()
            // Allow safe style attributes
            .allowStyling()
            .toFactory()
    }

    /**
     * Sanitizes HTML with a more restrictive policy.
     * Only allows basic text formatting tags.
     *
     * @param html the raw HTML content to sanitize
     * @return the sanitized HTML content with only basic formatting
     */
    fun sanitizeBasic(html: String?): String? {
        if (html.isNullOrBlank()) {
            return html
        }

        return basicPolicy.sanitize(html)
    }

    /**
     * Creates a basic policy with minimal formatting.
     * Uses OWASP's predefined FORMATTING and BLOCKS policies plus headings.
     */
    private fun createBasicPolicy(): PolicyFactory {
        return Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(
                HtmlPolicyBuilder()
                    .allowElements("h1", "h2", "h3", "h4", "h5", "h6")
                    .toFactory()
            )
    }

    /**
     * Strips all HTML tags and returns only plain text.
     *
     * @param html the HTML content
     * @return plain text without any HTML tags
     */
    fun stripHtml(html: String?): String? {
        if (html.isNullOrBlank()) {
            return html
        }

        // Use an empty policy to strip all HTML (no tags allowed)
        val emptyPolicy = HtmlPolicyBuilder().toFactory()
        return emptyPolicy.sanitize(html)
    }
}
