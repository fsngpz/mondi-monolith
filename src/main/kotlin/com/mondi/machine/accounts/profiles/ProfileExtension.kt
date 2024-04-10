package com.mondi.machine.accounts.profiles

/**
 * an extension function to convert the [Profile] to [ProfileResponse] instance.
 *
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
  return ProfileResponse(id, this.name, this.address, this.profilePictureUrl)
}