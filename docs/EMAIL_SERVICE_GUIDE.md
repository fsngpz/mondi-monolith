# Email Service Guide

This guide explains how to use the EmailService to send emails with Velocity templates in the Mondi application.

## Table of Contents
- [Overview](#overview)
- [Configuration](#configuration)
- [Sending Emails](#sending-emails)
- [Creating Templates](#creating-templates)
- [Best Practices](#best-practices)

## Overview

The `EmailService` provides a comprehensive email sending solution with support for:
- Simple text emails
- HTML emails
- Template-based emails using Velocity (VM) templates
- Emails with attachments

## Configuration

### SMTP Settings

Email configuration is defined in `application.properties`:

```properties
# SMTP Configuration
spring.mail.host=smtp-relay.brevo.com
spring.mail.username=your-username
spring.mail.password=your-password
spring.mail.properties.mail.transport.protocol=smtp
spring.mail.properties.mail.smtp.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

# Email Sender Information
mail.from.email=noreply@mondi.com
mail.from.name=Mondi
```

## Sending Emails

### 1. Simple Text Email

Send a plain text email:

```kotlin
@Service
class UserService(
    private val emailService: EmailService
) {
    fun sendWelcomeEmail(userEmail: String) {
        emailService.sendSimpleEmail(
            to = userEmail,
            subject = "Welcome to Mondi!",
            text = "Thank you for joining our platform."
        )
    }
}
```

### 2. HTML Email

Send an HTML formatted email:

```kotlin
fun sendHtmlNotification(userEmail: String) {
    val htmlContent = """
        <html>
            <body>
                <h1>Hello!</h1>
                <p>This is an <strong>HTML</strong> email.</p>
            </body>
        </html>
    """.trimIndent()

    emailService.sendHtmlEmail(
        to = userEmail,
        subject = "Notification",
        htmlContent = htmlContent
    )
}
```

### 3. Template-Based Email

Send an email using a Velocity template:

```kotlin
fun sendWelcomeEmailWithTemplate(user: User) {
    val context = mapOf(
        "name" to user.name,
        "verificationUrl" to "https://mondi.com/verify/${user.verificationToken}",
        "year" to Year.now().value.toString()
    )

    emailService.sendTemplateEmail(
        to = user.email,
        subject = "Welcome to Mondi!",
        templateName = "welcome",  // refers to welcome.vm
        context = context
    )
}
```

### 4. Email with Attachment

Send a template-based email with an attachment:

```kotlin
fun sendInvoiceEmail(user: User, pdfData: ByteArray) {
    val context = mapOf(
        "customerName" to user.name,
        "orderNumber" to "ORD-123456",
        "orderDate" to LocalDate.now().toString(),
        "year" to Year.now().value.toString()
    )

    emailService.sendTemplateEmailWithAttachment(
        to = user.email,
        subject = "Your Invoice",
        templateName = "order-confirmation",
        context = context,
        attachmentName = "invoice.pdf",
        attachmentData = pdfData
    )
}
```

## Creating Templates

### Template Location

All email templates should be placed in:
```
src/main/resources/templates/email/
```

### Template Naming

Template files use the `.vm` extension (Velocity Macro).
Example: `welcome.vm`, `password-reset.vm`

### Template Syntax

Velocity templates use Apache Velocity Template Language (VTL):

#### 1. Variables

```velocity
Hello, ${name}!
Your order number is: ${orderNumber}
```

#### 2. Conditionals

```velocity
#if($verificationUrl)
    <a href="${verificationUrl}">Verify Email</a>
#end

#if($user.isPremium)
    <p>Premium member benefits</p>
#else
    <p>Upgrade to premium</p>
#end
```

#### 3. Loops

```velocity
#foreach($item in $items)
    <tr>
        <td>${item.name}</td>
        <td>${item.price}</td>
    </tr>
#end
```

#### 4. Comments

```velocity
## This is a single-line comment

#*
  This is a
  multi-line comment
*#
```

### Example Template Structure

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>${subject}</title>
    <style>
        /* Your CSS styles here */
    </style>
</head>
<body>
    <div class="header">
        <h1>${title}</h1>
    </div>
    <div class="content">
        <p>Hello, ${name}!</p>
        <!-- Your content here -->
    </div>
    <div class="footer">
        <p>&copy; ${year} Mondi. All rights reserved.</p>
    </div>
</body>
</html>
```

## Available Templates

### 1. Welcome Email (`welcome.vm`)

**Context Variables:**
- `name` (String): User's name
- `verificationUrl` (String, optional): Email verification link
- `year` (String): Current year

**Example:**
```kotlin
val context = mapOf(
    "name" to "John Doe",
    "verificationUrl" to "https://mondi.com/verify/abc123",
    "year" to "2026"
)
emailService.sendTemplateEmail(user.email, "Welcome!", "welcome", context)
```

### 2. Password Reset (`password-reset.vm`)

**Context Variables:**
- `name` (String): User's name
- `resetUrl` (String, optional): Password reset link
- `resetCode` (String, optional): Reset code
- `expirationMinutes` (String): Link expiration time
- `year` (String): Current year

**Example:**
```kotlin
val context = mapOf(
    "name" to "Jane Doe",
    "resetUrl" to "https://mondi.com/reset/xyz789",
    "resetCode" to "123456",
    "expirationMinutes" to "30",
    "year" to "2026"
)
emailService.sendTemplateEmail(user.email, "Password Reset", "password-reset", context)
```

### 3. Order Confirmation (`order-confirmation.vm`)

**Context Variables:**
- `customerName` (String): Customer's name
- `orderNumber` (String): Order number
- `orderDate` (String): Order date
- `estimatedDelivery` (String): Estimated delivery date
- `items` (List<Map>): List of order items
- `totalAmount` (String): Total order amount
- `trackingUrl` (String, optional): Order tracking link
- `year` (String): Current year

**Example:**
```kotlin
val items = listOf(
    mapOf("name" to "Product A", "quantity" to 2, "price" to "29.99"),
    mapOf("name" to "Product B", "quantity" to 1, "price" to "49.99")
)

val context = mapOf(
    "customerName" to "Bob Smith",
    "orderNumber" to "ORD-123456",
    "orderDate" to "2026-01-31",
    "estimatedDelivery" to "2026-02-05",
    "items" to items,
    "totalAmount" to "109.97",
    "trackingUrl" to "https://mondi.com/track/ORD-123456",
    "year" to "2026"
)
emailService.sendTemplateEmail(customer.email, "Order Confirmed", "order-confirmation", context)
```

## Best Practices

### 1. Error Handling

Always wrap email sending in try-catch blocks:

```kotlin
try {
    emailService.sendTemplateEmail(email, subject, template, context)
    logger.info("Email sent successfully to $email")
} catch (e: EmailSendException) {
    logger.error("Failed to send email to $email", e)
    // Handle error appropriately (retry, alert, etc.)
}
```

### 2. Asynchronous Sending

For better performance, send emails asynchronously:

```kotlin
@Service
class NotificationService(
    private val emailService: EmailService
) {
    @Async
    fun sendWelcomeEmailAsync(user: User) {
        emailService.sendTemplateEmail(
            to = user.email,
            subject = "Welcome!",
            templateName = "welcome",
            context = mapOf("name" to user.name)
        )
    }
}
```

### 3. Template Testing

Test your templates before deploying:

```kotlin
@Test
fun `welcome template renders correctly`() {
    val context = mapOf(
        "name" to "Test User",
        "verificationUrl" to "https://test.com/verify",
        "year" to "2026"
    )

    // This will throw exception if template has errors
    emailService.sendTemplateEmail(
        "test@example.com",
        "Test",
        "welcome",
        context
    )
}
```

### 4. Email Preview

Create a preview endpoint for development:

```kotlin
@RestController
@RequestMapping("/api/dev/email-preview")
class EmailPreviewController(
    private val velocityEngine: VelocityEngine
) {
    @GetMapping("/{templateName}")
    fun preview(@PathVariable templateName: String): String {
        val context = VelocityContext()
        // Add sample data
        context.put("name", "Sample User")
        context.put("year", "2026")

        val writer = StringWriter()
        val template = velocityEngine.getTemplate("templates/email/$templateName.vm")
        template.merge(context, writer)

        return writer.toString()
    }
}
```

### 5. Localization

Support multiple languages:

```kotlin
fun sendLocalizedEmail(user: User, locale: Locale) {
    val templateName = when (locale.language) {
        "es" -> "welcome-es"
        "fr" -> "welcome-fr"
        else -> "welcome"
    }

    emailService.sendTemplateEmail(
        to = user.email,
        subject = getMessage("email.welcome.subject", locale),
        templateName = templateName,
        context = mapOf("name" to user.name)
    )
}
```

## Troubleshooting

### Common Issues

1. **Template Not Found**
   - Ensure template file exists in `src/main/resources/templates/email/`
   - Check file extension is `.vm`
   - Verify template name (without extension) matches parameter

2. **SMTP Connection Failed**
   - Verify SMTP credentials in `application.properties`
   - Check firewall settings
   - Ensure STARTTLS is supported by your email provider

3. **Variables Not Rendering**
   - Check variable names match exactly (case-sensitive)
   - Ensure variables are added to context map
   - Use `#if($variable)` to check if variable exists

4. **HTML Not Rendering**
   - Use `sendHtmlEmail()` or `sendTemplateEmail()` (not `sendSimpleEmail()`)
   - Ensure HTML is well-formed
   - Test in multiple email clients

## Additional Resources

- [Apache Velocity User Guide](https://velocity.apache.org/engine/2.4/user-guide.html)
- [Spring Boot Mail Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [Email HTML Best Practices](https://www.campaignmonitor.com/dev-resources/guides/coding-html-emails/)

---

**Last Updated:** 2026-01-31
**Author:** Ferdinand Sangap
