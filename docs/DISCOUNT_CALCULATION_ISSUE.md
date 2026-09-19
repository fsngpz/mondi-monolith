# Discount Calculation Issue & Resolution Plan

## Executive Summary

The current implementation experiences precision loss when converting between discount prices and percentages due to rounding operations. This document analyzes the root cause, evaluates multiple solutions, and recommends an optimal approach.

---

## Current Implementation

### Data Flow

1. **Frontend → Backend (Create/Update)**
   - Frontend sends: `discountPrice` (e.g., 99.99)
   - Backend receives: `discountPrice` and `price` (e.g., 100.00)
   - Backend calculates: `discountPercentage` using formula
   - Backend stores: Only `discountPercentage` in database

2. **Backend → Frontend (GET API)**
   - Backend reads: `price` and `discountPercentage` from database
   - Backend calculates: `discountPrice` using formula
   - Backend returns: Both `discountPrice` and `discountPercentage`

### Current Storage Schema

**Database (`products` table):**
```kotlin
price: BigDecimal                // e.g., 123.45
discountPercentage: BigDecimal   // e.g., 19.00 (stored with 2 decimal precision)
```

### Current Calculation Logic

**Location:** `src/main/kotlin/com/mondi/machine/products/ProductExtension.kt`

**Saving (Frontend → Database):**
```kotlin
// Line 53-66: calculateDiscountPercentage()
fun BigDecimal.calculateDiscountPercentage(
    originalPrice: BigDecimal,
    scale: Int = 2
): BigDecimal {
    val discount = originalPrice - this  // this = discountPrice
    // Percentage = (Discount / Original) * 100
    return (discount * BigDecimal(100))
        .divide(originalPrice, scale, RoundingMode.HALF_UP)
}
```

**Retrieving (Database → Frontend):**
```kotlin
// Line 102-106: getDiscountPrice()
fun getDiscountPrice(originalPrice: BigDecimal, discountPercentage: BigDecimal): BigDecimal {
    val discountAmount = (originalPrice * discountPercentage)
        .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    return originalPrice - discountAmount
}
```

---

## The Problem: Precision Loss

### Root Cause

The issue occurs due to **double rounding** - once when converting discountPrice → percentage, and again when converting percentage → discountPrice.

### Problematic Example

```
Original Price: $123.45
Frontend sends discountPrice: $100.00

Step 1 (Saving):
- Discount amount: $123.45 - $100.00 = $23.45
- Calculate percentage: (23.45 / 123.45) × 100 = 18.997568...%
- Rounded to 2 decimals: 19.00%
- Database stores: 19.00%

Step 2 (Retrieving):
- Read from DB: 19.00%
- Calculate discount amount: ($123.45 × 19.00) / 100 = $23.4555
- Rounded to 2 decimals: $23.46
- Calculate discountPrice: $123.45 - $23.46 = $99.99
- API returns: $99.99 ❌ (Expected: $100.00)

Discrepancy: $0.01
```

### When It Happens

The precision loss occurs when:
1. The calculated percentage has more than 2 decimal places
2. The recalculation introduces rounding in the opposite direction
3. Specific price combinations that don't divide evenly

### Impact Assessment

**Critical Scenarios:**
- Financial accuracy issues (price mismatch)
- Customer trust concerns (displayed price ≠ saved price)
- Accounting discrepancies in reports
- Frontend validation failures

**Scale Impact:**
- Small datasets: Negligible performance impact
- Large datasets (1000+ products): Calculation overhead in GET APIs

---

## Solution Analysis

### Option 1: Store Discount Price (Instead of Percentage)

**Implementation:**
```kotlin
// Database schema
price: BigDecimal
discountPrice: BigDecimal  // NEW: Store actual discount price

// On GET API
discountPercentage = discountPrice.calculateDiscountPercentage(price)
```

**Pros:**
- ✅ Preserves exact discount price sent by frontend
- ✅ No precision loss on the primary value (discountPrice)
- ✅ Simple database migration
- ✅ Frontend always gets the exact price they set

**Cons:**
- ❌ Percentage calculated on every GET request
- ❌ Performance impact on large datasets (N calculations per request)
- ❌ Percentage may vary slightly across requests if price changes
- ❌ Cannot easily apply percentage-based discounts without recalculation

**Performance Analysis:**
```
For 1000 products:
- 1000 × calculateDiscountPercentage() calls
- Each: 1 subtraction + 1 multiplication + 1 division
- Estimated overhead: ~10-20ms per 1000 products (negligible)
```

**Verdict:** ⚠️ Acceptable for small to medium datasets

---

### Option 2: Store Both Discount Price AND Percentage

**Implementation:**
```kotlin
// Database schema
price: BigDecimal
discountPrice: BigDecimal      // Store actual discount price
discountPercentage: BigDecimal // Store calculated percentage

// On GET API (no calculation needed)
return ProductResponse(
    price = product.price,
    discountPrice = product.discountPrice,
    discountPercentage = product.discountPercentage
)
```

**Pros:**
- ✅ Zero precision loss
- ✅ Zero calculation overhead on GET APIs
- ✅ Both values immediately available
- ✅ Best for read-heavy applications
- ✅ Can validate consistency between values

**Cons:**
- ❌ Data redundancy (storing 2 representations of same data)
- ❌ Risk of inconsistency if one field updated without the other
- ❌ Slightly larger database footprint
- ❌ Requires careful update logic to maintain consistency

**Mitigation for Cons:**
```kotlin
// Use database triggers or application-level validation
class Product {
    var price: BigDecimal
    var discountPrice: BigDecimal
        set(value) {
            field = value
            // Auto-update percentage when discountPrice changes
            discountPercentage = value.calculateDiscountPercentage(price)
        }
    var discountPercentage: BigDecimal
        private set  // Make setter private to prevent inconsistency
}
```

**Verdict:** ✅ **RECOMMENDED** for most use cases

---

### Option 3: Store Discount Amount (Instead of Price or Percentage)

**Implementation:**
```kotlin
// Database schema
price: BigDecimal
discountAmount: BigDecimal  // Store dollar amount of discount

// On GET API
discountPrice = price - discountAmount
discountPercentage = discountAmount.calculatePercentageFromAmount(price)
```

**Pros:**
- ✅ Preserves exact discount amount
- ✅ Easy to apply same discount amount across products

**Cons:**
- ❌ Requires TWO calculations on GET API (price and percentage)
- ❌ Worse performance than Option 1
- ❌ Less intuitive than storing discount price
- ❌ Frontend needs to send discountAmount instead of discountPrice

**Verdict:** ❌ Not recommended

---

### Option 4: Increase Percentage Precision

**Implementation:**
```kotlin
// Use higher precision for percentage
fun BigDecimal.calculateDiscountPercentage(
    originalPrice: BigDecimal,
    scale: Int = 6  // Increased from 2 to 6
): BigDecimal {
    return (discount * BigDecimal(100))
        .divide(originalPrice, scale, RoundingMode.HALF_UP)
}

// Database stores: 18.997568% (instead of 19.00%)
```

**Testing the Example:**
```
Original Price: $123.45
Frontend sends: $100.00

Saving:
- Percentage: (23.45 / 123.45) × 100 = 18.997568%
- Database stores: 18.997568%

Retrieving:
- Discount: ($123.45 × 18.997568) / 100 = $23.449995...
- Rounded: $23.45
- discountPrice: $123.45 - $23.45 = $100.00 ✅
```

**Pros:**
- ✅ Significantly reduces precision loss
- ✅ Minimal code changes
- ✅ No additional database columns
- ✅ No performance impact

**Cons:**
- ⚠️ Still has theoretical precision loss (just reduced)
- ⚠️ May show unintuitive percentages (18.997568% vs 19%)
- ⚠️ Doesn't guarantee 100% accuracy in all cases

**When It Still Fails:**
```
Price: $0.03
discountPrice: $0.01
Percentage: (0.02 / 0.03) × 100 = 66.666666...%
Even at 6 decimals: 66.666667%
Recalculated: $0.03 × 66.666667 / 100 = $0.020000... ≈ $0.02
discountPrice: $0.03 - $0.02 = $0.01 ✅

Price: $0.07
discountPrice: $0.04
Percentage: (0.03 / 0.07) × 100 = 42.857142857...%
At 6 decimals: 42.857143%
Recalculated: $0.07 × 42.857143 / 100 = $0.03000... ≈ $0.03
discountPrice: $0.07 - $0.03 = $0.04 ✅
```

**Verdict:** ✅ Good middle-ground solution

---

### Option 5: Store Percentage with Exact Price Fallback

**Implementation:**
```kotlin
// Database schema
price: BigDecimal
discountPercentage: BigDecimal
discountPriceOverride: BigDecimal?  // Nullable, only set when precision matters

// On GET API
fun getDiscountPrice(): BigDecimal {
    return discountPriceOverride ?: calculateDiscountPrice(price, discountPercentage)
}

// On Save
fun save(inputPrice: BigDecimal, inputDiscountPrice: BigDecimal) {
    val calculatedPercentage = inputDiscountPrice.calculateDiscountPercentage(inputPrice)
    val recalculatedPrice = getDiscountPrice(inputPrice, calculatedPercentage)

    // Only store override if there's a discrepancy
    if (recalculatedPrice != inputDiscountPrice) {
        discountPriceOverride = inputDiscountPrice
    } else {
        discountPriceOverride = null
    }
}
```

**Pros:**
- ✅ Guarantees exact price when needed
- ✅ Optimizes storage (override only when necessary)
- ✅ Maintains percentage-based discounts
- ✅ Zero calculation overhead when override exists

**Cons:**
- ⚠️ More complex logic
- ⚠️ Additional nullable field
- ⚠️ Requires careful handling in update scenarios

**Verdict:** ✅ Advanced solution for complex requirements

---

## Recommended Solution

### Primary Recommendation: **Option 2** (Store Both)

**Rationale:**
1. **Accuracy**: 100% precision preservation
2. **Performance**: Zero calculation overhead on reads
3. **Simplicity**: Straightforward implementation
4. **Maintainability**: Clear data model, easy to debug

### Alternative Recommendation: **Option 4** (Higher Precision)

**Use When:**
- Storage optimization is critical
- Willing to accept minor precision loss in edge cases
- Want minimal code changes

---

## Implementation Plan

### Phase 1: Database Migration

**Migration Script:**
```sql
-- Add new column
ALTER TABLE products
ADD COLUMN discount_price DECIMAL(19, 2);

-- Populate existing data
UPDATE products
SET discount_price = price - (price * discount_percentage / 100)
WHERE discount_percentage > 0;

-- Add constraint to ensure discount_price <= price
ALTER TABLE products
ADD CONSTRAINT check_discount_price_valid
CHECK (discount_price IS NULL OR discount_price <= price);
```

### Phase 2: Update Entity

**File:** `src/main/kotlin/com/mondi/machine/products/Product.kt`

```kotlin
@Entity
@Table(name = "products")
class Product(
    var name: String,
    var description: String?,
    var currency: String,
    var specificationInHtml: String?,
    var price: BigDecimal,
    var discountPrice: BigDecimal = BigDecimal.ZERO,      // NEW
    var discountPercentage: BigDecimal = BigDecimal.ZERO,
    // ... rest of fields
)
```

### Phase 3: Update Response Mapping

**File:** `src/main/kotlin/com/mondi/machine/products/ProductExtension.kt`

```kotlin
fun Product.toResponse(): ProductResponse {
    return ProductResponse(
        id = requireNotNull(this.id),
        name = this.name,
        description = this.description,
        price = this.price,
        discountPrice = this.discountPrice,  // Direct mapping, no calculation
        currency = this.currency,
        discountPercentage = this.discountPercentage,  // Direct mapping
        // ... rest of fields
    )
}
```

### Phase 4: Update Save Logic

**File:** `src/main/kotlin/com/mondi/machine/backoffices/products/BackofficeProductService.kt`

```kotlin
suspend fun updateWithMediaManagement(
    id: Long,
    request: BackofficeProductUpdateRequest
): BackofficeProductResponse {
    val price = request.price
    val inputDiscountPrice = request.discountPrice ?: BigDecimal.ZERO
    val inputPercentage = request.discountPercentage

    // Calculate final values
    val (finalDiscountPrice, finalPercentage) = when {
        // Priority 1: Use discount price if provided
        inputDiscountPrice.signum() > 0 -> {
            val percentage = inputDiscountPrice.calculateDiscountPercentage(price)
            Pair(inputDiscountPrice, percentage)
        }
        // Priority 2: Use percentage if provided
        inputPercentage.signum() > 0 -> {
            val discountPrice = getDiscountPrice(price, inputPercentage)
            Pair(discountPrice, inputPercentage)
        }
        // No discount
        else -> Pair(BigDecimal.ZERO, BigDecimal.ZERO)
    }

    return productService.updateWithMediaManagement(
        id = id,
        // ... other fields
        price = price,
        discountPrice = finalDiscountPrice,      // NEW
        discountPercentage = finalPercentage,
        // ... rest of fields
    ).toResponse()
}
```

### Phase 5: Update Service Layer

**File:** `src/main/kotlin/com/mondi/machine/products/ProductService.kt`

Update the `updateWithMediaManagement` method signature to include `discountPrice`:

```kotlin
suspend fun updateWithMediaManagement(
    id: Long,
    name: String,
    description: String?,
    price: BigDecimal,
    discountPrice: BigDecimal,  // NEW parameter
    currency: String,
    // ... rest of parameters
)
```

---

## Testing Strategy

### Unit Tests

**Test Cases:**
```kotlin
@Test
fun `should preserve exact discount price after save and retrieve`() {
    // Given
    val originalPrice = BigDecimal("123.45")
    val discountPrice = BigDecimal("100.00")

    // When
    val product = createProduct(price = originalPrice, discountPrice = discountPrice)
    val retrieved = productRepository.findById(product.id!!)

    // Then
    assertThat(retrieved.discountPrice).isEqualTo(discountPrice)
    assertThat(retrieved.discountPercentage).isEqualTo(BigDecimal("19.00"))
}

@Test
fun `should handle edge cases with precision`() {
    val testCases = listOf(
        Triple(BigDecimal("99.99"), BigDecimal("66.66"), BigDecimal("33.33")),
        Triple(BigDecimal("123.45"), BigDecimal("100.00"), BigDecimal("19.00")),
        Triple(BigDecimal("0.03"), BigDecimal("0.01"), BigDecimal("66.67"))
    )

    testCases.forEach { (price, discountPrice, expectedPercentage) ->
        val product = createProduct(price = price, discountPrice = discountPrice)
        val retrieved = productRepository.findById(product.id!!)

        assertThat(retrieved.discountPrice).isEqualTo(discountPrice)
        assertThat(retrieved.discountPercentage).isCloseTo(expectedPercentage, within(BigDecimal("0.01")))
    }
}
```

### Integration Tests

```kotlin
@Test
fun `GET API should return exact discount price from database`() {
    // Given
    val product = createProduct(
        price = BigDecimal("123.45"),
        discountPrice = BigDecimal("100.00")
    )

    // When
    val response = mockMvc.get("/api/products/${product.id}")
        .andExpect { status { isOk() } }
        .andReturn()
        .response
        .contentAsString
        .let { objectMapper.readValue(it, ProductResponse::class.java) }

    // Then
    assertThat(response.discountPrice).isEqualTo(BigDecimal("100.00"))
    assertThat(response.discountPercentage).isEqualTo(BigDecimal("19.00"))
}
```

---

## Performance Considerations

### Current vs. Recommended Approach

| Metric | Current (Calculation) | Recommended (Stored) |
|--------|----------------------|---------------------|
| GET API (1 product) | ~0.01ms overhead | 0ms overhead |
| GET API (1000 products) | ~10-20ms overhead | 0ms overhead |
| Database size | Baseline | +8 bytes per product |
| Update complexity | Low | Medium |

### Scalability Analysis

**For 10,000 products:**
- Additional storage: ~80 KB (negligible)
- Read performance improvement: ~100-200ms per request
- Write overhead: Minimal (one extra field)

**Conclusion:** Read optimization >> Storage cost

---

## Migration Risk Assessment

### Risks

1. **Data Inconsistency During Migration**
   - **Mitigation**: Run migration in transaction, validate all products

2. **Backward Compatibility**
   - **Mitigation**: Keep both fields, deprecate calculation method gradually

3. **Frontend Breaking Changes**
   - **Mitigation**: API contract remains the same (both fields already in response)

### Rollback Plan

```sql
-- If needed to rollback
ALTER TABLE products
DROP COLUMN discount_price;

-- Revert application code to use calculation method
```

---

## Conclusion

The precision loss in discount calculations stems from double rounding during percentage ↔ price conversions. Storing both `discountPrice` and `discountPercentage` provides the optimal balance of accuracy, performance, and maintainability.

### Next Steps

1. ✅ Review and approve this document
2. ⬜ Create database migration script
3. ⬜ Update entity and service layers
4. ⬜ Write comprehensive tests
5. ⬜ Deploy to staging environment
6. ⬜ Validate with real data
7. ⬜ Deploy to production

---

**Document Version:** 1.0
**Last Updated:** 2026-01-26
**Author:** Ferdinand Sangap
**Status:** Pending Review
