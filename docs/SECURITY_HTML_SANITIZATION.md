# HTML Sanitization Security

## Overview

The Mondi application implements HTML sanitization to prevent XSS (Cross-Site Scripting) attacks on user-supplied HTML
content, specifically in product specifications. This document describes the sanitization implementation and security
measures.

## Implementation

### Technology

The application uses [OWASP Java HTML Sanitizer](https://github.com/OWASP/java-html-sanitizer) version 20240325.1, a
production-ready Java library designed specifically for sanitizing user-provided HTML and preventing XSS attacks. This
library is maintained by OWASP (Open Web Application Security Project) and is battle-tested in production environments.

### Component Location

**File:** `src/main/kotlin/com/mondi/machine/utils/HtmlSanitizer.kt`

The `HtmlSanitizer` is a Spring `@Component` that uses OWASP's `PolicyFactory` and `HtmlPolicyBuilder` to define
sanitization policies. It provides three sanitization methods:

1. `sanitize(html: String?)` - Full sanitization with product-friendly tags using a comprehensive policy
2. `sanitizeBasic(html: String?)` - Basic formatting only using a restrictive policy
3. `stripHtml(html: String?)` - Remove all HTML tags using an empty policy

## Policy Configuration

### Overview

OWASP Java HTML Sanitizer uses a policy-based approach where you define what HTML elements and attributes are allowed.
The `HtmlPolicyBuilder` creates these policies, and malicious content is automatically stripped during sanitization.

### Allowed Tags

The sanitizer's comprehensive policy allows the following HTML tags suitable for product specifications:

#### Text Formatting

- `<p>`, `<br>` - Paragraphs and line breaks
- `<strong>`, `<b>` - Bold text
- `<em>`, `<i>` - Italic text
- `<u>` - Underlined text
- `<s>` - Strikethrough text
- `<mark>` - Highlighted text
- `<small>` - Small text
- `<sub>`, `<sup>` - Subscript and superscript

#### Headings

- `<h1>`, `<h2>`, `<h3>`, `<h4>`, `<h5>`, `<h6>` - All heading levels

#### Lists

- `<ul>` - Unordered lists
- `<ol>` - Ordered lists
- `<li>` - List items
- `<ol start="5">` - Ordered lists with custom starting number

#### Tables

- `<table>` - Table container
- `<thead>`, `<tbody>`, `<tfoot>` - Table sections
- `<tr>` - Table rows
- `<th>`, `<td>` - Table headers and cells
- `<caption>` - Table caption
- `<td colspan="2" rowspan="3">` - Cell spanning

#### Other Semantic Tags

- `<blockquote>` - Block quotes
- `<code>`, `<pre>` - Code blocks
- `<hr>` - Horizontal rule
- `<div>`, `<span>` - Generic containers

### Allowed Attributes

- `class` - CSS class names (on all tags)
- `style` - Limited inline styles (on all tags)
- `colspan`, `rowspan` - Table cell spanning
- `start` - Ordered list starting number

### Blocked Elements

The following are automatically removed by the sanitizer:

#### Dangerous Tags

- `<script>` - JavaScript code
- `<iframe>` - Embedded frames
- `<object>`, `<embed>` - Embedded objects
- `<style>` - CSS styles
- `<link>` - External resources
- `<meta>` - Metadata

#### Event Handlers

All JavaScript event handlers are stripped:

- `onclick`, `ondblclick`
- `onload`, `onerror`
- `onmouseover`, `onmouseout`
- `onfocus`, `onblur`
- And all other `on*` attributes

#### Dangerous Attributes

- `data-*` - Data attributes
- `javascript:` URLs
- `vbscript:` URLs

## Usage in Application

### Where Sanitization is Applied

HTML sanitization is applied in `ProductService` before saving to the database:

1. **Product Creation** (`create` method)
2. **Product Update** (`update` method)
3. **Product Update with Media Management** (`updateWithMediaManagement` method)

**File:** `src/main/kotlin/com/mondi/machine/products/ProductService.kt`

```kotlin
@Service
class ProductService(
    private val htmlSanitizer: HtmlSanitizer
) {
    suspend fun create(request: BackofficeProductRequest): Product {
        // HTML is automatically sanitized using OWASP policies
        val sanitizedSpecification = htmlSanitizer.sanitize(request.specificationInHtml)
        val product = Product(
            specificationInHtml = sanitizedSpecification,
            // ... other fields
        )
        // ...
    }
}
```

### API Endpoints Affected

All endpoints that accept product creation or updates:

- `POST /v1/backoffice/products` - Create product
- `PUT /v1/backoffice/products/{id}` - Update product (legacy)
- `PATCH /v1/backoffice/products/{id}` - Update product with media management

## Security Guarantees

### What is Prevented

The HTML sanitizer prevents:

1. **XSS Attacks** - Malicious JavaScript cannot be injected
2. **Event Handler Exploitation** - No event-based attacks possible
3. **Frame Injection** - Cannot embed external iframes
4. **CSS Injection** - Style tags and most CSS properties are blocked
5. **Protocol Handlers** - JavaScript and VBScript URLs are blocked

### Example Attack Prevention

#### Script Tag Removal

```html
<!-- Input -->
<p>Safe content</p>
<script>
  alert('XSS')
</script>

<!-- Output -->
<p>Safe content</p>
```

#### Event Handler Removal

```html
<!-- Input -->
<p onclick="alert('XSS')">Click me</p>

<!-- Output -->
<p>Click me</p>
```

#### JavaScript URL Removal

```html
<!-- Input -->
<a href="javascript:alert('XSS')">Click</a>

<!-- Output -->
Click
```

#### Iframe Removal

```html
<!-- Input -->
<p>Content</p>
<iframe src="evil.com"></iframe>

<!-- Output -->
<p>Content</p>
```

## Testing

### Test Coverage

Comprehensive test coverage ensures security:

**File:** `src/test/kotlin/com/mondi/machine/utils/HtmlSanitizerTest.kt`

**Test Categories:**

1. **Safe Tag Preservation** - Ensures allowed tags are kept
2. **Dangerous Tag Removal** - Ensures malicious tags are stripped
3. **Event Handler Removal** - Ensures JavaScript events are blocked
4. **Nested Tag Handling** - Ensures complex HTML structures work
5. **Edge Cases** - Null, empty, blank strings
6. **Real-World Scenarios** - Complex product specifications

**Total Tests:** 35 test cases covering various attack vectors and legitimate use cases

### Running Tests

```bash
# Run HTML sanitizer tests only
./gradlew test --tests "com.mondi.machine.utils.HtmlSanitizerTest"

# Run all tests including security tests
./gradlew clean build
```

## Best Practices

### For Frontend Developers

1. **Don't Pre-Sanitize** - Send HTML as-is; backend handles sanitization
2. **Use Allowed Tags** - Stick to the allowed tag list for best results
3. **Test Edge Cases** - Test with various HTML structures during development
4. **Inform Users** - If providing a rich text editor, document which tags are allowed

### For Backend Developers

1. **Always Sanitize** - Never skip sanitization for any user-supplied HTML
2. **Sanitize Early** - Sanitize at the service layer before database save
3. **Test Thoroughly** - Add tests for any new fields that accept HTML
4. **Review Safelist** - Periodically review and update the safelist as needed

## Security Considerations

### Defense in Depth

HTML sanitization is one layer of security. Additional measures should include:

1. **Content Security Policy (CSP)** - HTTP headers to prevent XSS
2. **Input Validation** - Validate data types and formats
3. **Output Encoding** - Encode data when rendering to HTML
4. **Authentication & Authorization** - Ensure only authorized users can create/update products

### Known Limitations

1. **CSS Attacks** - While style tags are blocked, inline styles with safe properties are allowed via OWASP's styling
   policy. The library prevents dangerous CSS but allows common styling.
2. **HTML Formatting** - OWASP outputs XHTML-compliant HTML (e.g., `<hr />` instead of `<hr>`). This is more strict but
   safer.
3. **Performance** - HTML sanitization has minimal overhead but is not zero-cost. OWASP is optimized for production use.

## Maintenance

### Updating the Policy

To add or remove allowed tags or attributes:

1. Modify `HtmlSanitizer.createPolicy()` method using `HtmlPolicyBuilder`
2. Add corresponding tests in `HtmlSanitizerTest`
3. Update this documentation
4. Review security implications with the team

Example of adding a new element:

```kotlin
HtmlPolicyBuilder()
    .allowElements("newElement")
    .allowAttributes("newAttribute").onElements("newElement")
    .toFactory()
```

### Updating OWASP Java HTML Sanitizer

When updating the OWASP dependency:

1. Check [OWASP releases](https://github.com/OWASP/java-html-sanitizer/releases) for security updates
2. Run full test suite to ensure compatibility
3. Update `owaspSanitizerVersion` in `build.gradle.kts`
4. Review any breaking changes in the release notes

## References

- [OWASP Java HTML Sanitizer GitHub](https://github.com/OWASP/java-html-sanitizer)
- [OWASP Java HTML Sanitizer Documentation](https://javadoc.io/doc/com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer/latest/index.html)
- [OWASP XSS Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
- [OWASP XSS Filter Evasion Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/XSS_Filter_Evasion_Cheat_Sheet.html)
- [OWASP Top 10 - Injection](https://owasp.org/www-project-top-ten/)

## Questions and Support

For security concerns or questions about HTML sanitization, please contact the security team or create an issue in the
project repository.
