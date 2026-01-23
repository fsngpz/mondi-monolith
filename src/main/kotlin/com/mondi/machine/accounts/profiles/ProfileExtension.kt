package com.mondi.machine.accounts.profiles

/**
 * an extension function to convert the [Profile] to [ProfileResponse] instance.
 *
 * @return the [ProfileResponse] instance.
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
fun Profile.toResponse(): ProfileResponse {
  val id = this.id
  // -- validate the field id --
  requireNotNull(id) {
    "value for 'id' is null"
  }
  // -- return the instance of ProfileResponse --
  return ProfileResponse(
    id = id,
    name = this.name,
    address = this.address,
    profilePictureUrl = this.profilePictureUrl,
    mobile = this.user.mobile,
    membershipSince = this.user.membershipSince
  )
}

/**
 * an extension function to convert the [Profile] to [ProfileRequest] instance.
 *
 * @return the [ProfileRequest] instance.
 */
fun Profile.toRequest(): ProfileRequest {
  return ProfileRequest(
    name = this.name,
    address = this.address,
    profilePictureKey = this.profilePictureUrl,
    mobile = this.user.mobile,
    membershipSince = this.user.membershipSince
  )
}