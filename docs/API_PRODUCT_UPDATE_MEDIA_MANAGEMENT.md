# Product Update with Media Management API

## Overview

This document describes the improved product update API that provides frontend-friendly media management. The new API
allows you to:

- Keep existing media by specifying their URLs
- Remove media by omitting their URLs from the request
- Upload new media files alongside existing media

## API Endpoints

### New Endpoint (Recommended)

**PATCH** `/v1/backoffice/products/{productId}`

This is the **recommended** endpoint for updating products with media management.

### Legacy Endpoint (Deprecated)

**PUT** `/v1/backoffice/products/{productId}`

This endpoint still exists for backwards compatibility but requires reuploading ALL media files. Use the PATCH endpoint
instead.

---

## Request Format

### Endpoint

```
PATCH /v1/backoffice/products/{productId}
Content-Type: multipart/form-data
Authorization: Bearer {jwt_token}
```

### Path Parameters

| Parameter | Type | Required | Description                                    |
|-----------|------|----------|------------------------------------------------|
| productId | Long | Yes      | The unique identifier of the product to update |

### Form Data Parameters

| Parameter             | Type               | Required | Description                              | Example                                                              |
|-----------------------|--------------------|----------|------------------------------------------|----------------------------------------------------------------------|
| name                  | String             | Yes      | Product name                             | "Diamond Wedding Ring"                                               |
| description           | String             | No       | Product description                      | "Beautiful 14k gold ring"                                            |
| price                 | BigDecimal         | Yes      | Product price                            | 1500.00                                                              |
| currency              | String             | Yes      | Currency code (USD, EUR, etc.)           | "USD"                                                                |
| specificationInHtml   | String             | No       | Product specifications in HTML format    | "&lt;p&gt;14k gold&lt;/p&gt;"                                        |
| discountPercentage    | BigDecimal         | Yes      | Discount percentage                      | 10.00                                                                |
| category              | String             | Yes      | Product category                         | "RING"                                                               |
| stock                 | Integer            | Yes      | Available stock                          | 50                                                                   |
| **existingMediaUrls** | List&lt;String&gt; | No       | Array of existing media URLs to **keep** | ["https://example.com/image1.jpg", "https://example.com/image2.jpg"] |
| **newMediaFiles**     | List&lt;File&gt;   | No       | Array of new image files to **upload**   | [file1.jpg, file2.jpg]                                               |

### Product Categories

Valid category values:

- `RING`
- `EARRING`
- `NECKLACE`
- `BRACELET`
- `PENDANT`
- `OTHER`

---

## Security Features

### HTML Sanitization

The `specificationInHtml` field is automatically sanitized using **OWASP Java HTML Sanitizer** to prevent XSS
(Cross-Site Scripting) attacks before being saved to the database. OWASP is a trusted, production-ready security library
maintained by the Open Web Application Security Project.

This means:

**Allowed Tags:**

- Text formatting: `<p>`, `<br>`, `<strong>`, `<b>`, `<em>`, `<i>`, `<u>`, `<s>`, `<mark>`, `<small>`, `<sub>`, `<sup>`
- Headings: `<h1>`, `<h2>`, `<h3>`, `<h4>`, `<h5>`, `<h6>`
- Lists: `<ul>`, `<ol>`, `<li>`
- Tables: `<table>`, `<thead>`, `<tbody>`, `<tfoot>`, `<tr>`, `<th>`, `<td>`, `<caption>`
- Other: `<blockquote>`, `<code>`, `<pre>`, `<hr>`, `<div>`, `<span>`

**Blocked Tags and Attributes:**

- Scripts: `<script>`, `<iframe>`, `<object>`, `<embed>`
- Styles: `<style>` tags (inline styles are limited)
- Event handlers: `onclick`, `onerror`, `onload`, etc.
- JavaScript URLs: `javascript:alert('XSS')`
- Data attributes: `data-*`

**Example:**

```html
<!-- Input -->
<p>This is <strong>bold</strong> text</p>
<script>alert('XSS')</script>

<!-- Output (sanitized) -->
<p>This is <strong>bold</strong> text</p>
```

This sanitization happens automatically - you don't need to do anything on the frontend. Just send the HTML as-is, and
the backend will ensure it's safe.

---

## How Media Management Works

### Scenario 1: Keep All Existing Media (No Changes)

If you want to update product details without changing media:

**Request:**

```
PATCH /v1/backoffice/products/123
Content-Type: multipart/form-data

name=Updated Product Name
price=2000.00
currency=USD
category=RING
stock=30
discountPercentage=15.00
existingMediaUrls=https://cdn.example.com/product/image1.jpg
existingMediaUrls=https://cdn.example.com/product/image2.jpg
```

**Result:** Both existing images are kept, no new uploads.

---

### Scenario 2: Remove Some Media

If you want to remove the second image and keep only the first:

**Request:**

```
PATCH /v1/backoffice/products/123
Content-Type: multipart/form-data

name=Updated Product Name
price=2000.00
currency=USD
category=RING
stock=30
discountPercentage=15.00
existingMediaUrls=https://cdn.example.com/product/image1.jpg
```

**Result:**

- First image is kept
- Second image is **deleted** from storage and database

---

### Scenario 3: Add New Media

If you want to keep existing media and add new ones:

**Request:**

```
PATCH /v1/backoffice/products/123
Content-Type: multipart/form-data

name=Updated Product Name
price=2000.00
currency=USD
category=RING
stock=30
discountPercentage=15.00
existingMediaUrls=https://cdn.example.com/product/image1.jpg
newMediaFiles=<file data for new-image1.jpg>
newMediaFiles=<file data for new-image2.jpg>
```

**Result:**

- Existing image1.jpg is kept (order: 0)
- new-image1.jpg is uploaded (order: 1)
- new-image2.jpg is uploaded (order: 2)

---

### Scenario 4: Replace All Media

If you want to remove all existing media and upload completely new ones:

**Request:**

```
PATCH /v1/backoffice/products/123
Content-Type: multipart/form-data

name=Updated Product Name
price=2000.00
currency=USD
category=RING
stock=30
discountPercentage=15.00
newMediaFiles=<file data for brand-new1.jpg>
newMediaFiles=<file data for brand-new2.jpg>
```

**Result:**

- ALL existing media is **deleted**
- brand-new1.jpg is uploaded (order: 0)
- brand-new2.jpg is uploaded (order: 1)

---

## Media Order

Media files are ordered as follows:

1. Existing media (kept in the order they appear in `existingMediaUrls`)
2. New media (appended after existing media in upload order)

**Example:**

```
existingMediaUrls: [imageB.jpg, imageA.jpg]
newMediaFiles: [imageC.jpg, imageD.jpg]

Final order:
0. imageB.jpg (existing, kept)
1. imageA.jpg (existing, kept)
2. imageC.jpg (newly uploaded)
3. imageD.jpg (newly uploaded)
```

---

## Response Format

### Success Response (200 OK)

```json
{
  "id": 123,
  "name": "Updated Product Name",
  "description": "Product description",
  "price": 2000.00,
  "currency": "USD",
  "specificationInHtml": "<p>14k gold</p>",
  "discountPercentage": 15.00,
  "mediaUrls": [
    "https://cdn.example.com/product/image1.jpg",
    "https://cdn.example.com/product/new-image1.jpg",
    "https://cdn.example.com/product/new-image2.jpg"
  ],
  "category": "RING",
  "stock": 30,
  "sku": "RING_26_001",
  "status": "ACTIVE"
}
```

### Error Responses

#### 400 Bad Request

```json
{
  "type": "IllegalArgumentException",
  "message": "the field 'name' cannot be null"
}
```

#### 401 Unauthorized

```json
{
  "type": "UnauthorizedException",
  "message": "Invalid or expired token"
}
```

#### 403 Forbidden

```json
{
  "type": "AccessDeniedException",
  "message": "Access denied"
}
```

#### 404 Not Found

```json
{
  "type": "NoSuchElementException",
  "message": "no product data was found with id '123'"
}
```

---

## Frontend Integration Examples

### React/TypeScript Example

```typescript
interface UpdateProductRequest {
    name: string;
    description?: string;
    price: number;
    currency: string;
    specificationInHtml?: string;
    discountPercentage: number;
    category: string;
    stock: number;
    existingMediaUrls: string[];
    newMediaFiles: File[];
}

async function updateProduct(
    productId: number,
    data: UpdateProductRequest,
    token: string
): Promise<ProductResponse> {
    const formData = new FormData();

    // Add basic fields
    formData.append('name', data.name);
    if (data.description) formData.append('description', data.description);
    formData.append('price', data.price.toString());
    formData.append('currency', data.currency);
    if (data.specificationInHtml) {
        formData.append('specificationInHtml', data.specificationInHtml);
    }
    formData.append('discountPercentage', data.discountPercentage.toString());
    formData.append('category', data.category);
    formData.append('stock', data.stock.toString());

    // Add existing media URLs
    data.existingMediaUrls.forEach(url => {
        formData.append('existingMediaUrls', url);
    });

    // Add new media files
    data.newMediaFiles.forEach(file => {
        formData.append('newMediaFiles', file);
    });

    const response = await fetch(
        `https://api.example.com/v1/backoffice/products/${productId}`,
        {
            method: 'PATCH',
            headers: {
                'Authorization': `Bearer ${token}`,
            },
            body: formData,
        }
    );

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
}
```

### Usage Example

```typescript
// User wants to remove the 2nd image and add a new one
const currentProduct = {
    id: 123,
    mediaUrls: [
        'https://cdn.example.com/product/image1.jpg',
        'https://cdn.example.com/product/image2.jpg',
        'https://cdn.example.com/product/image3.jpg'
    ],
    // ... other fields
};

// User selects to keep image1 and image3, and uploads a new file
const newFile = new File([blob], 'new-image.jpg', {type: 'image/jpeg'});

await updateProduct(123, {
    name: 'Updated Product',
    price: 2000.00,
    currency: 'USD',
    category: 'RING',
    stock: 30,
    discountPercentage: 15.00,
    existingMediaUrls: [
        'https://cdn.example.com/product/image1.jpg',
        'https://cdn.example.com/product/image3.jpg'
    ],
    newMediaFiles: [newFile]
}, userToken);

// Result:
// - image1.jpg is kept (order: 0)
// - image2.jpg is DELETED
// - image3.jpg is kept (order: 1)
// - new-image.jpg is uploaded (order: 2)
```

### Vue 3 Composition API Example

```vue

<script setup lang="ts">
  import {ref} from 'vue';

  const product = ref({
    id: 123,
    name: 'Diamond Ring',
    mediaUrls: [
      'https://cdn.example.com/product/image1.jpg',
      'https://cdn.example.com/product/image2.jpg'
    ],
    // ... other fields
  });

  const selectedMedia = ref<string[]>([...product.value.mediaUrls]);
  const newFiles = ref<File[]>([]);

  function onMediaSelect(url: string) {
    const index = selectedMedia.value.indexOf(url);
    if (index > -1) {
      selectedMedia.value.splice(index, 1); // Remove (user wants to delete this)
    } else {
      selectedMedia.value.push(url); // Add back
    }
  }

  function onNewFilesSelect(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files) {
      newFiles.value = Array.from(target.files);
    }
  }

  async function saveProduct() {
    const formData = new FormData();
    formData.append('name', product.value.name);
    formData.append('price', product.value.price.toString());
    formData.append('currency', product.value.currency);
    formData.append('category', product.value.category);
    formData.append('stock', product.value.stock.toString());
    formData.append('discountPercentage', product.value.discountPercentage.toString());

    // Add selected existing media
    selectedMedia.value.forEach(url => {
      formData.append('existingMediaUrls', url);
    });

    // Add new files
    newFiles.value.forEach(file => {
      formData.append('newMediaFiles', file);
    });

    const response = await fetch(
        `/v1/backoffice/products/${product.value.id}`,
        {
          method: 'PATCH',
          headers: {
            'Authorization': `Bearer ${token}`,
          },
          body: formData,
        }
    );

    const updatedProduct = await response.json();
    product.value = updatedProduct;
  }
</script>

<template>
  <div>
    <!-- Existing media selection -->
    <div v-for="url in product.mediaUrls" :key="url">
      <img :src="url"/>
      <input
          type="checkbox"
          :checked="selectedMedia.includes(url)"
          @change="onMediaSelect(url)"
      />
      <label>Keep this image</label>
    </div>

    <!-- New file upload -->
    <input
        type="file"
        multiple
        accept="image/*"
        @change="onNewFilesSelect"
    />

    <button @click="saveProduct">Save Product</button>
  </div>
</template>
```

---

## Best Practices

### 1. Always Send Existing Media URLs

When updating a product, always include the `existingMediaUrls` parameter with the URLs of media you want to **keep**.
Any media URL not included will be **deleted**.

❌ **Wrong:** Don't send empty `existingMediaUrls` if you want to keep media

```
existingMediaUrls=  (empty)
```

This will **delete all existing media**!

✅ **Correct:** Always include URLs of media to keep

```
existingMediaUrls=https://cdn.example.com/image1.jpg
existingMediaUrls=https://cdn.example.com/image2.jpg
```

### 2. Order Matters

The order of URLs in `existingMediaUrls` determines the display order. Put the most important image first.

### 3. Error Handling

Always handle potential errors:

- Network failures
- File size limits (check with backend team)
- Unsupported file formats
- Authorization failures

### 4. File Validation

Validate files on the frontend before upload:

```typescript
function validateImage(file: File): boolean {
    const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    const maxSize = 5 * 1024 * 1024; // 5MB

    if (!validTypes.includes(file.type)) {
        alert('Only JPEG, PNG, and WebP images are allowed');
        return false;
    }

    if (file.size > maxSize) {
        alert('File size must be less than 5MB');
        return false;
    }

    return true;
}
```

### 5. Loading States

Show loading indicators during upload:

```typescript
const [isUpdating, setIsUpdating] = useState(false);

async function updateProduct() {
    setIsUpdating(true);
    try {
        await updateProductAPI(...);
    } finally {
        setIsUpdating(false);
    }
}
```

---

## Migration from PUT to PATCH

If you're currently using the PUT endpoint, here's how to migrate:

### Old Way (PUT - Requires Reuploading All Media)

```typescript
// User must reupload ALL images even if they haven't changed
const formData = new FormData();
formData.append('name', 'Updated Product');
formData.append('mediaFiles', existingImage1File); // Must reupload!
formData.append('mediaFiles', existingImage2File); // Must reupload!
formData.append('mediaFiles', newImage3File);

fetch(`/v1/backoffice/products/123`, {
    method: 'PUT',
    body: formData
});
```

### New Way (PATCH - Keep Existing by URL)

```typescript
// Just reference existing images by URL, no reuploading!
const formData = new FormData();
formData.append('name', 'Updated Product');
formData.append('existingMediaUrls', 'https://cdn.example.com/image1.jpg'); // Just URL!
formData.append('existingMediaUrls', 'https://cdn.example.com/image2.jpg'); // Just URL!
formData.append('newMediaFiles', newImage3File); // Only upload new

fetch(`/v1/backoffice/products/123`, {
    method: 'PATCH',
    body: formData
});
```

---

## Summary

The new PATCH endpoint provides a frontend-friendly way to update products with fine-grained media management:

✅ **Benefits:**

- No need to reupload existing images
- Granular control over which media to keep/remove
- Efficient bandwidth usage
- Better user experience
- Maintains media order

🎯 **Key Points:**

- Use `existingMediaUrls` to specify which media to **keep**
- Use `newMediaFiles` to upload new media
- Media not in `existingMediaUrls` will be **deleted**
- Order is preserved based on array order
- All other product fields work the same as before

📝 **Recommendation:**
Migrate all frontend code to use the PATCH endpoint for better performance and user experience.
