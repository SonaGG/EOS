/*
 * Copyright 2026 Sona Softworks LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gg.sona.eos

import gg.sona.eos.internal.Native
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.ValueLayout

/**
 * All possible result codes returned by EOS API calls.
 *
 * The original C names are preserved in the KDoc for each entry. See the
 * upstream `eos_result.h` for the full description of each code.
 */
@Suppress("EnumEntryName", "EnumNaming")
public enum class EosResult(public val value: Int) {
    /** `EOS_Success`: The operation succeeded. */
    Success(0),
    /** `EOS_NoConnection`: There is no network connection. */
    NoConnection(1),
    /** `EOS_InvalidCredentials`: Invalid credentials were supplied. */
    InvalidCredentials(2),
    /** `EOS_InvalidUser`: The local user is invalid. */
    InvalidUser(3),
    /** `EOS_InvalidAuth`: The local user is not authenticated. */
    InvalidAuth(4),
    /** `EOS_AccessDenied`: Access was denied. */
    AccessDenied(5),
    /** `EOS_MissingPermissions`: A permission required to perform the operation is missing. */
    MissingPermissions(6),
    /** `EOS_Token_Not_Account`: The token does not represent an account. */
    TokenNotAccount(7),
    /** `EOS_TooManyRequests`: The call was throttled. */
    TooManyRequests(8),
    /** `EOS_AlreadyPending`: An async request with the same parameters is already in flight. */
    AlreadyPending(9),
    /** `EOS_InvalidParameters`: One or more parameters are invalid. */
    InvalidParameters(10),
    /** `EOS_InvalidRequest`: The request is invalid. */
    InvalidRequest(11),
    /** `EOS_UnrecognizedResponse`: The backend returned a response that could not be parsed. */
    UnrecognizedResponse(12),
    /** `EOS_IncompatibleVersion`: The SDK version is incompatible with the backend. */
    IncompatibleVersion(13),
    /** `EOS_NotConfigured`: The SDK is not configured. */
    NotConfigured(14),
    /** `EOS_AlreadyConfigured`: The SDK is already configured. */
    AlreadyConfigured(15),
    /** `EOS_NotImplemented`: The operation is not implemented on this platform. */
    NotImplemented(16),
    /** `EOS_Canceled`: The operation was canceled. */
    Canceled(17),
    /** `EOS_NotFound`: The requested resource was not found. */
    NotFound(18),
    /** `EOS_OperationWillRetry`: An async operation will be retried. */
    OperationWillRetry(19),
    /** `EOS_NoChange`: The request had no effect. */
    NoChange(20),
    /** `EOS_VersionMismatch`: A version mismatch occurred. */
    VersionMismatch(21),
    /** `EOS_LimitExceeded`: A client-side limit was exceeded. */
    LimitExceeded(22),
    /** `EOS_Disabled`: The feature or account is disabled. */
    Disabled(23),
    /** `EOS_DuplicateNotAllowed`: A duplicate entry is not allowed. */
    DuplicateNotAllowed(24),
    /** `EOS_MissingParameters_DEPRECATED`: Missing parameters. Deprecated and unused. */
    MissingParametersDeprecated(25),
    /** `EOS_InvalidSandboxId`: The sandbox id is invalid. */
    InvalidSandboxId(26),
    /** `EOS_TimedOut`: The operation timed out. */
    TimedOut(27),
    /** `EOS_PartialResult`: Only a partial result was returned. */
    PartialResult(28),
    /** `EOS_Missing_Role`: A required role is missing. */
    MissingRole(29),
    /** `EOS_Missing_Feature`: A required feature is missing. */
    MissingFeature(30),
    /** `EOS_Invalid_Sandbox`: The sandbox is invalid. */
    InvalidSandbox(31),
    /** `EOS_Invalid_Deployment`: The deployment is invalid. */
    InvalidDeployment(32),
    /** `EOS_Invalid_Product`: The product is invalid. */
    InvalidProduct(33),
    /** `EOS_Invalid_ProductUserID`: The product user id is invalid. */
    InvalidProductUserId(34),
    /** `EOS_ServiceFailure`: The backend service failed. */
    ServiceFailure(35),
    /** `EOS_CacheDirectoryMissing`: The cache directory is not set. */
    CacheDirectoryMissing(36),
    /** `EOS_CacheDirectoryInvalid`: The cache directory is not accessible. */
    CacheDirectoryInvalid(37),
    /** `EOS_InvalidState`: The resource is in an invalid state. */
    InvalidState(38),
    /** `EOS_RequestInProgress`: A request is already in progress. */
    RequestInProgress(39),
    /** `EOS_ApplicationSuspended`: The application is suspended. */
    ApplicationSuspended(40),
    /** `EOS_NetworkDisconnected`: The network is disconnected. */
    NetworkDisconnected(41),
    /** `EOS_InsufficientOutputBuffer`: The output buffer is too small. */
    InsufficientOutputBuffer(42),
    /** `EOS_ClientPolicyMissingAction`: The action is not allowed by client policy. */
    ClientPolicyMissingAction(43),
    /** `EOS_Auth_AccountLocked`: The account is locked. */
    AuthAccountLocked(1001),
    /** `EOS_Auth_AccountLockedForUpdate`: The account is locked for update. */
    AuthAccountLockedForUpdate(1002),
    /** `EOS_Auth_InvalidRefreshToken`: The refresh token is invalid. */
    AuthInvalidRefreshToken(1003),
    /** `EOS_Auth_InvalidToken`: The access token is invalid. */
    AuthInvalidToken(1004),
    /** `EOS_Auth_AuthenticationFailure`: Authentication failed. */
    AuthAuthenticationFailure(1005),
    /** `EOS_Auth_InvalidPlatformToken`: The platform token is invalid. */
    AuthInvalidPlatformToken(1006),
    /** `EOS_Auth_WrongAccount`: Auth parameters do not match the account. */
    AuthWrongAccount(1007),
    /** `EOS_Auth_WrongClient`: Auth parameters do not match the client. */
    AuthWrongClient(1008),
    /** `EOS_Auth_FullAccountRequired`: A full account is required. */
    AuthFullAccountRequired(1009),
    /** `EOS_Auth_HeadlessAccountRequired`: A headless account is required. */
    AuthHeadlessAccountRequired(1010),
    /** `EOS_Auth_PasswordResetRequired`: A password reset is required. */
    AuthPasswordResetRequired(1011),
    /** `EOS_Auth_PasswordCannotBeReused`: The password cannot be reused. */
    AuthPasswordCannotBeReused(1012),
    /** `EOS_Auth_Expired`: The authorization code has expired. */
    AuthExpired(1013),
    /** `EOS_Auth_ScopeConsentRequired`: User consent is required. */
    AuthScopeConsentRequired(1014),
    /** `EOS_Auth_ApplicationNotFound`: The application is not found. */
    AuthApplicationNotFound(1015),
    /** `EOS_Auth_ScopeNotFound`: The requested scope is not found. */
    AuthScopeNotFound(1016),
    /** `EOS_Auth_AccountFeatureRestricted`: The account is restricted. */
    AuthAccountFeatureRestricted(1017),
    /** `EOS_Auth_AccountPortalLoadError`: The Account Portal failed to load. */
    AuthAccountPortalLoadError(1018),
    /** `EOS_Auth_CorrectiveActionRequired`: Corrective action is required. */
    AuthCorrectiveActionRequired(1019),
    /** `EOS_Auth_PinGrantCode`: A pin grant code was issued. */
    AuthPinGrantCode(1020),
    /** `EOS_Auth_PinGrantExpired`: The pin grant code has expired. */
    AuthPinGrantExpired(1021),
    /** `EOS_Auth_PinGrantPending`: The pin grant code is pending. */
    AuthPinGrantPending(1022),
    /** `EOS_Auth_ExternalAuthNotLinked`: External auth is not linked. */
    AuthExternalAuthNotLinked(1030),
    /** `EOS_Auth_ExternalAuthRevoked`: External auth was revoked. */
    AuthExternalAuthRevoked(1032),
    /** `EOS_Auth_ExternalAuthInvalid`: External auth token is invalid. */
    AuthExternalAuthInvalid(1033),
    /** `EOS_Auth_ExternalAuthRestricted`: External auth is restricted. */
    AuthExternalAuthRestricted(1034),
    /** `EOS_Auth_ExternalAuthCannotLogin`: External auth cannot be used to login. */
    AuthExternalAuthCannotLogin(1035),
    /** `EOS_Auth_ExternalAuthExpired`: External auth has expired. */
    AuthExternalAuthExpired(1036),
    /** `EOS_Auth_ExternalAuthIsLastLoginType`: External auth is the last login type. */
    AuthExternalAuthIsLastLoginType(1037),
    /** `EOS_Auth_ExchangeCodeNotFound`: The exchange code was not found. */
    AuthExchangeCodeNotFound(1040),
    /** `EOS_Auth_OriginatingExchangeCodeSessionExpired`: The originating exchange code session has expired. */
    AuthOriginatingExchangeCodeSessionExpired(1041),
    /** `EOS_Auth_AccountNotActive`: The account is not active. */
    AuthAccountNotActive(1050),
    /** `EOS_Auth_MFARequired`: MFA is required. */
    AuthMfaRequired(1060),
    /** `EOS_Auth_ParentalControls`: Parental controls are in place. */
    AuthParentalControls(1070),
    /** `EOS_Auth_NoRealId`: Korea real id is missing. */
    AuthNoRealId(1080),
    /** `EOS_Auth_UserInterfaceRequired`: User interaction is required. */
    AuthUserInterfaceRequired(1090),
    /** `EOS_Friends_InviteAwaitingAcceptance`: An invite is awaiting acceptance. */
    FriendsInviteAwaitingAcceptance(2000),
    /** `EOS_Friends_NoInvitation`: There is no pending invitation. */
    FriendsNoInvitation(2001),
    /** `EOS_Friends_AlreadyFriends`: Users are already friends. */
    FriendsAlreadyFriends(2003),
    /** `EOS_Friends_NotFriends`: Users are not friends. */
    FriendsNotFriends(2004),
    /** `EOS_Friends_TargetUserTooManyInvites`: The target user has too many invites. */
    FriendsTargetUserTooManyInvites(2005),
    /** `EOS_Friends_LocalUserTooManyInvites`: The local user has too many invites. */
    FriendsLocalUserTooManyInvites(2006),
    /** `EOS_Friends_TargetUserFriendLimitExceeded`: The target user has reached the friend limit. */
    FriendsTargetUserFriendLimitExceeded(2007),
    /** `EOS_Friends_LocalUserFriendLimitExceeded`: The local user has reached the friend limit. */
    FriendsLocalUserFriendLimitExceeded(2008),
    /** `EOS_Presence_DataInvalid`: Presence data is invalid. */
    PresenceDataInvalid(3000),
    /** `EOS_Presence_DataLengthInvalid`: Presence data length is invalid. */
    PresenceDataLengthInvalid(3001),
    /** `EOS_Presence_DataKeyInvalid`: Presence data key is invalid. */
    PresenceDataKeyInvalid(3002),
    /** `EOS_Presence_DataKeyLengthInvalid`: Presence data key length is invalid. */
    PresenceDataKeyLengthInvalid(3003),
    /** `EOS_Presence_DataValueInvalid`: Presence data value is invalid. */
    PresenceDataValueInvalid(3004),
    /** `EOS_Presence_DataValueLengthInvalid`: Presence data value length is invalid. */
    PresenceDataValueLengthInvalid(3005),
    /** `EOS_Presence_RichTextInvalid`: Rich text is invalid. */
    PresenceRichTextInvalid(3006),
    /** `EOS_Presence_RichTextLengthInvalid`: Rich text is too long. */
    PresenceRichTextLengthInvalid(3007),
    /** `EOS_Presence_StatusInvalid`: Status is invalid. */
    PresenceStatusInvalid(3008),
    /** `EOS_Presence_RichTextNotSupported`: Rich text is not supported by the template. */
    PresenceRichTextNotSupported(3009),
    /** `EOS_Presence_TemplateNotSupported`: Template is required for rich text. */
    PresenceTemplateNotSupported(3010),
    /** `EOS_Presence_TemplateIdInvalid`: Template id is invalid. */
    PresenceTemplateIdInvalid(3011),
    /** `EOS_Presence_TemplateTypeInvalid`: Template type is invalid. */
    PresenceTemplateTypeInvalid(3012),
    /** `EOS_Presence_TemplateKeyInvalid`: Template key is invalid. */
    PresenceTemplateKeyInvalid(3013),
    /** `EOS_Presence_TemplateValueInvalid`: Template value is invalid. */
    PresenceTemplateValueInvalid(3014),
    /** `EOS_Presence_TemplateNotFound`: Template was not found. */
    PresenceTemplateNotFound(3015),
    /** `EOS_Presence_TemplateInvalidVariableInput`: Invalid variable input for template. */
    PresenceTemplateInvalidVariableInput(3016),
    /** `EOS_Presence_TemplateLocalizationServerError`: Localization server error. */
    PresenceTemplateLocalizationServerError(3017),
    /** `EOS_Presence_TemplateUnknownError`: Unknown template error. */
    PresenceTemplateUnknownError(3018),
    /** `EOS_Ecom_EntitlementStale`: The entitlement is stale. */
    EcomEntitlementStale(4000),
    /** `EOS_Ecom_CatalogOfferStale`: The catalog offer is stale. */
    EcomCatalogOfferStale(4001),
    /** `EOS_Ecom_CatalogItemStale`: The catalog item is stale. */
    EcomCatalogItemStale(4002),
    /** `EOS_Ecom_CatalogOfferPriceInvalid`: The catalog offer price is invalid. */
    EcomCatalogOfferPriceInvalid(4003),
    /** `EOS_Ecom_CheckoutLoadError`: The checkout page failed to load. */
    EcomCheckoutLoadError(4004),
    /** `EOS_Ecom_PurchaseProcessing`: The purchase is processing. */
    EcomPurchaseProcessing(4005),
    /** `EOS_Ecom_CatalogOfferInvalid`: The catalog offer is invalid. */
    EcomCatalogOfferInvalid(4006),
    /** `EOS_Sessions_SessionInProgress`: A session is already in progress. */
    SessionsSessionInProgress(5000),
    /** `EOS_Sessions_TooManyPlayers`: Too many players. */
    SessionsTooManyPlayers(5001),
    /** `EOS_Sessions_NoPermission`: No permission for the session. */
    SessionsNoPermission(5002),
    /** `EOS_Sessions_SessionAlreadyExists`: The session already exists. */
    SessionsSessionAlreadyExists(5003),
    /** `EOS_Sessions_InvalidLock`: The session lock is invalid. */
    SessionsInvalidLock(5004),
    /** `EOS_Sessions_InvalidSession`: The session is invalid. */
    SessionsInvalidSession(5005),
    /** `EOS_Sessions_SandboxNotAllowed`: The sandbox is not allowed. */
    SessionsSandboxNotAllowed(5006),
    /** `EOS_Sessions_InviteFailed`: Sending the invite failed. */
    SessionsInviteFailed(5007),
    /** `EOS_Sessions_InviteNotFound`: The invite was not found. */
    SessionsInviteNotFound(5008),
    /** `EOS_Sessions_UpsertNotAllowed`: The session cannot be modified. */
    SessionsUpsertNotAllowed(5009),
    /** `EOS_Sessions_AggregationFailed`: Aggregation failed. */
    SessionsAggregationFailed(5010),
    /** `EOS_Sessions_HostAtCapacity`: The host is at capacity. */
    SessionsHostAtCapacity(5011),
    /** `EOS_Sessions_SandboxAtCapacity`: The sandbox is at capacity. */
    SessionsSandboxAtCapacity(5012),
    /** `EOS_Sessions_SessionNotAnonymous`: The session is not anonymous. */
    SessionsSessionNotAnonymous(5013),
    /** `EOS_Sessions_OutOfSync`: The session is out of sync. */
    SessionsOutOfSync(5014),
    /** `EOS_Sessions_TooManyInvites`: Too many invites. */
    SessionsTooManyInvites(5015),
    /** `EOS_Sessions_PresenceSessionExists`: A presence session already exists. */
    SessionsPresenceSessionExists(5016),
    /** `EOS_Sessions_DeploymentAtCapacity`: The deployment is at capacity. */
    SessionsDeploymentAtCapacity(5017),
    /** `EOS_Sessions_NotAllowed`: The operation is not allowed. */
    SessionsNotAllowed(5018),
    /** `EOS_Sessions_PlayerSanctioned`: The player is sanctioned. */
    SessionsPlayerSanctioned(5019),
    /** `EOS_PlayerDataStorage_FilenameInvalid`: The filename is invalid. */
    PlayerDataStorageFilenameInvalid(6000),
    /** `EOS_PlayerDataStorage_FilenameLengthInvalid`: The filename is too long. */
    PlayerDataStorageFilenameLengthInvalid(6001),
    /** `EOS_PlayerDataStorage_FilenameInvalidChars`: The filename has invalid characters. */
    PlayerDataStorageFilenameInvalidChars(6002),
    /** `EOS_PlayerDataStorage_FileSizeTooLarge`: The file size is too large. */
    PlayerDataStorageFileSizeTooLarge(6003),
    /** `EOS_PlayerDataStorage_FileSizeInvalid`: The file size is invalid. */
    PlayerDataStorageFileSizeInvalid(6004),
    /** `EOS_PlayerDataStorage_FileHandleInvalid`: The file handle is invalid. */
    PlayerDataStorageFileHandleInvalid(6005),
    /** `EOS_PlayerDataStorage_DataInvalid`: The data is invalid. */
    PlayerDataStorageDataInvalid(6006),
    /** `EOS_PlayerDataStorage_DataLengthInvalid`: The data length is invalid. */
    PlayerDataStorageDataLengthInvalid(6007),
    /** `EOS_PlayerDataStorage_StartIndexInvalid`: The start index is invalid. */
    PlayerDataStorageStartIndexInvalid(6008),
    /** `EOS_PlayerDataStorage_RequestInProgress`: A request is in progress. */
    PlayerDataStorageRequestInProgress(6009),
    /** `EOS_PlayerDataStorage_UserThrottled`: The user is throttled. */
    PlayerDataStorageUserThrottled(6010),
    /** `EOS_PlayerDataStorage_EncryptionKeyNotSet`: The encryption key is not set. */
    PlayerDataStorageEncryptionKeyNotSet(6011),
    /** `EOS_PlayerDataStorage_UserErrorFromDataCallback`: A data callback returned an error. */
    PlayerDataStorageUserErrorFromDataCallback(6012),
    /** `EOS_PlayerDataStorage_FileHeaderHasNewerVersion`: The file has a newer header version. */
    PlayerDataStorageFileHeaderHasNewerVersion(6013),
    /** `EOS_PlayerDataStorage_FileCorrupted`: The file is corrupted. */
    PlayerDataStorageFileCorrupted(6014),
    /** `EOS_Connect_ExternalTokenValidationFailed`: External token validation failed. */
    ConnectExternalTokenValidationFailed(7000),
    /** `EOS_Connect_UserAlreadyExists`: The user already exists. */
    ConnectUserAlreadyExists(7001),
    /** `EOS_Connect_AuthExpired`: Auth has expired. */
    ConnectAuthExpired(7002),
    /** `EOS_Connect_InvalidToken`: The token is invalid. */
    ConnectInvalidToken(7003),
    /** `EOS_Connect_UnsupportedTokenType`: The token type is unsupported. */
    ConnectUnsupportedTokenType(7004),
    /** `EOS_Connect_LinkAccountFailed`: Linking the account failed. */
    ConnectLinkAccountFailed(7005),
    /** `EOS_Connect_ExternalServiceUnavailable`: The external service is unavailable. */
    ConnectExternalServiceUnavailable(7006),
    /** `EOS_Connect_ExternalServiceConfigurationFailure`: The external service is mis-configured. */
    ConnectExternalServiceConfigurationFailure(7007),
    /** `EOS_UI_SocialOverlayLoadError`: The social overlay failed to load. */
    UiSocialOverlayLoadError(8000),
    /** `EOS_UI_InconsistentVirtualMemoryFunctions`: Custom memory functions are inconsistent. */
    UiInconsistentVirtualMemoryFunctions(8001),
    /** `EOS_Lobby_NotOwner`: Not the lobby owner. */
    LobbyNotOwner(9000),
    /** `EOS_Lobby_InvalidLock`: The lobby lock is invalid. */
    LobbyInvalidLock(9001),
    /** `EOS_Lobby_LobbyAlreadyExists`: The lobby already exists. */
    LobbyLobbyAlreadyExists(9002),
    /** `EOS_Lobby_SessionInProgress`: A session is in progress. */
    LobbySessionInProgress(9003),
    /** `EOS_Lobby_TooManyPlayers`: Too many players. */
    LobbyTooManyPlayers(9004),
    /** `EOS_Lobby_NoPermission`: No permission for the lobby. */
    LobbyNoPermission(9005),
    /** `EOS_Lobby_InvalidSession`: The session is invalid. */
    LobbyInvalidSession(9006),
    /** `EOS_Lobby_SandboxNotAllowed`: The sandbox is not allowed. */
    LobbySandboxNotAllowed(9007),
    /** `EOS_Lobby_InviteFailed`: The invite failed. */
    LobbyInviteFailed(9008),
    /** `EOS_Lobby_InviteNotFound`: The invite was not found. */
    LobbyInviteNotFound(9009),
    /** `EOS_Lobby_UpsertNotAllowed`: The lobby cannot be modified. */
    LobbyUpsertNotAllowed(9010),
    /** `EOS_Lobby_AggregationFailed`: Aggregation failed. */
    LobbyAggregationFailed(9011),
    /** `EOS_Lobby_HostAtCapacity`: The host is at capacity. */
    LobbyHostAtCapacity(9012),
    /** `EOS_Lobby_SandboxAtCapacity`: The sandbox is at capacity. */
    LobbySandboxAtCapacity(9013),
    /** `EOS_Lobby_TooManyInvites`: Too many invites. */
    LobbyTooManyInvites(9014),
    /** `EOS_Lobby_DeploymentAtCapacity`: The deployment is at capacity. */
    LobbyDeploymentAtCapacity(9015),
    /** `EOS_Lobby_NotAllowed`: The operation is not allowed. */
    LobbyNotAllowed(9016),
    /** `EOS_Lobby_MemberUpdateOnly`: Only member data was updated. */
    LobbyMemberUpdateOnly(9017),
    /** `EOS_Lobby_PresenceLobbyExists`: A presence lobby already exists. */
    LobbyPresenceLobbyExists(9018),
    /** `EOS_Lobby_VoiceNotEnabled`: The lobby does not have voice enabled. */
    LobbyVoiceNotEnabled(9019),
    /** `EOS_Lobby_PlatformNotAllowed`: The client platform is not allowed. */
    LobbyPlatformNotAllowed(9020),
    /** `EOS_TitleStorage_UserErrorFromDataCallback`: A data callback returned an error. */
    TitleStorageUserErrorFromDataCallback(10000),
    /** `EOS_TitleStorage_EncryptionKeyNotSet`: The encryption key is not set. */
    TitleStorageEncryptionKeyNotSet(10001),
    /** `EOS_TitleStorage_FileCorrupted`: The file is corrupted. */
    TitleStorageFileCorrupted(10002),
    /** `EOS_TitleStorage_FileHeaderHasNewerVersion`: The file has a newer header version. */
    TitleStorageFileHeaderHasNewerVersion(10003),
    /** `EOS_Mods_ModSdkProcessIsAlreadyRunning`: The ModSdk process is already running. */
    ModsModSdkProcessIsAlreadyRunning(11000),
    /** `EOS_Mods_ModSdkCommandIsEmpty`: The ModSdk command is empty. */
    ModsModSdkCommandIsEmpty(11001),
    /** `EOS_Mods_ModSdkProcessCreationFailed`: The ModSdk process could not be created. */
    ModsModSdkProcessCreationFailed(11002),
    /** `EOS_Mods_CriticalError`: A critical ModSdk error occurred. */
    ModsCriticalError(11003),
    /** `EOS_Mods_ToolInternalError`: A ModSdk internal error occurred. */
    ModsToolInternalError(11004),
    /** `EOS_Mods_IPCFailure`: A ModSdk IPC failure occurred. */
    ModsIpcFailure(11005),
    /** `EOS_Mods_InvalidIPCResponse`: An invalid ModSdk IPC response was received. */
    ModsInvalidIpcResponse(11006),
    /** `EOS_Mods_URILaunchFailure`: A ModSdk URI launch failure occurred. */
    ModsUriLaunchFailure(11007),
    /** `EOS_Mods_ModIsNotInstalled`: The mod is not installed. */
    ModsModIsNotInstalled(11008),
    /** `EOS_Mods_UserDoesNotOwnTheGame`: The user does not own the game. */
    ModsUserDoesNotOwnTheGame(11009),
    /** `EOS_Mods_OfferRequestByIdInvalidResult`: Invalid offer request result. */
    ModsOfferRequestByIdInvalidResult(11010),
    /** `EOS_Mods_CouldNotFindOffer`: The mod offer was not found. */
    ModsCouldNotFindOffer(11011),
    /** `EOS_Mods_OfferRequestByIdFailure`: The offer request failed. */
    ModsOfferRequestByIdFailure(11012),
    /** `EOS_Mods_PurchaseFailure`: The mod purchase failed. */
    ModsPurchaseFailure(11013),
    /** `EOS_Mods_InvalidGameInstallInfo`: The game install info is invalid. */
    ModsInvalidGameInstallInfo(11014),
    /** `EOS_Mods_CannotGetManifestLocation`: The mod manifest location is unavailable. */
    ModsCannotGetManifestLocation(11015),
    /** `EOS_Mods_UnsupportedOS`: The mod does not support the current OS. */
    ModsUnsupportedOs(11016),
    /** `EOS_AntiCheat_ClientProtectionNotAvailable`: The anti-cheat client is not available. */
    AntiCheatClientProtectionNotAvailable(12000),
    /** `EOS_AntiCheat_InvalidMode`: The anti-cheat mode is invalid. */
    AntiCheatInvalidMode(12001),
    /** `EOS_AntiCheat_ClientProductIdMismatch`: The anti-cheat product id does not match. */
    AntiCheatClientProductIdMismatch(12002),
    /** `EOS_AntiCheat_ClientSandboxIdMismatch`: The anti-cheat sandbox id does not match. */
    AntiCheatClientSandboxIdMismatch(12003),
    /** `EOS_AntiCheat_ProtectMessageSessionKeyRequired`: A session key is required. */
    AntiCheatProtectMessageSessionKeyRequired(12004),
    /** `EOS_AntiCheat_ProtectMessageValidationFailed`: Message validation failed. */
    AntiCheatProtectMessageValidationFailed(12005),
    /** `EOS_AntiCheat_ProtectMessageInitializationFailed`: Message encryption init failed. */
    AntiCheatProtectMessageInitializationFailed(12006),
    /** `EOS_AntiCheat_PeerAlreadyRegistered`: The peer is already registered. */
    AntiCheatPeerAlreadyRegistered(12007),
    /** `EOS_AntiCheat_PeerNotFound`: The peer was not found. */
    AntiCheatPeerNotFound(12008),
    /** `EOS_AntiCheat_PeerNotProtected`: The peer is not protected. */
    AntiCheatPeerNotProtected(12009),
    /** `EOS_AntiCheat_ClientDeploymentIdMismatch`: The anti-cheat deployment id does not match. */
    AntiCheatClientDeploymentIdMismatch(12010),
    /** `EOS_AntiCheat_DeviceIdAuthIsNotSupported`: Device id auth is not supported. */
    AntiCheatDeviceIdAuthIsNotSupported(12011),
    /** `EOS_RTC_TooManyParticipants`: The RTC room is at capacity. */
    RtcTooManyParticipants(13000),
    /** `EOS_RTC_RoomAlreadyExists`: The RTC room already exists. */
    RtcRoomAlreadyExists(13001),
    /** `EOS_RTC_UserKicked`: The user was kicked. */
    RtcUserKicked(13002),
    /** `EOS_RTC_UserBanned`: The user is banned. */
    RtcUserBanned(13003),
    /** `EOS_RTC_RoomWasLeft`: The room was left. */
    RtcRoomWasLeft(13004),
    /** `EOS_RTC_ReconnectionTimegateExpired`: The reconnection timegate has expired. */
    RtcReconnectionTimegateExpired(13005),
    /** `EOS_RTC_ShutdownInvoked`: The SDK was shut down. */
    RtcShutdownInvoked(13006),
    /** `EOS_RTC_UserIsInBlocklist`: The user is in the block list. */
    RtcUserIsInBlocklist(13007),
    /** `EOS_RTC_AllocationFailed`: Resource allocation failed. */
    RtcAllocationFailed(13009),
    /** `EOS_RTC_VoiceModerationModeMismatch`: Voice moderation mode mismatch. */
    RtcVoiceModerationModeMismatch(13010),
    /** `EOS_RTC_EmptyRecord`: The record buffer is empty. */
    RtcEmptyRecord(13011),
    /** `EOS_RTC_RoomOptionsMismatch`: Room options mismatch. */
    RtcRoomOptionsMismatch(13012),
    /** `EOS_ProgressionSnapshot_SnapshotIdUnavailable`: The snapshot id is unavailable. */
    ProgressionSnapshotIdUnavailable(14000),
    /** `EOS_KWS_ParentEmailMissing`: The parent email is missing. */
    KwsParentEmailMissing(15000),
    /** `EOS_KWS_UserGraduated`: The user has graduated. */
    KwsUserGraduated(15001),
    /** `EOS_Android_JavaVMNotStored`: The Java VM is not stored. */
    AndroidJavaVmNotStored(17000),
    /** `EOS_Android_ReservedMustReferenceLocalVM`: Android Reserved must reference the local VM. */
    AndroidReservedMustReferenceLocalVm(17001),
    /** `EOS_Android_ReservedMustBeNull`: Android Reserved must be null. */
    AndroidReservedMustBeNull(17002),
    /** `EOS_Permission_RequiredPatchAvailable`: A patch is required. */
    PermissionRequiredPatchAvailable(18000),
    /** `EOS_Permission_RequiredSystemUpdate`: A system update is required. */
    PermissionRequiredSystemUpdate(18001),
    /** `EOS_Permission_AgeRestrictionFailure`: Age restriction failure. */
    PermissionAgeRestrictionFailure(18002),
    /** `EOS_Permission_AccountTypeFailure`: Account type failure. */
    PermissionAccountTypeFailure(18003),
    /** `EOS_Permission_ChatRestriction`: Chat is restricted. */
    PermissionChatRestriction(18004),
    /** `EOS_Permission_UGCRestriction`: UGC is restricted. */
    PermissionUgcRestriction(18005),
    /** `EOS_Permission_OnlinePlayRestricted`: Online play is restricted. */
    PermissionOnlinePlayRestricted(18006),
    /** `EOS_DesktopCrossplay_ApplicationNotBootstrapped`: The application was not bootstrapped. */
    DesktopCrossplayApplicationNotBootstrapped(19000),
    /** `EOS_DesktopCrossplay_ServiceNotInstalled`: The service is not installed. */
    DesktopCrossplayServiceNotInstalled(19001),
    /** `EOS_DesktopCrossplay_ServiceStartFailed`: The service failed to start. */
    DesktopCrossplayServiceStartFailed(19002),
    /** `EOS_DesktopCrossplay_ServiceNotRunning`: The service is not running. */
    DesktopCrossplayServiceNotRunning(19003),
    /** `EOS_CustomInvites_InviteFailed`: The custom invite failed. */
    CustomInvitesInviteFailed(20000),
    /** `EOS_UserInfo_BestDisplayNameIndeterminate`: The best display name is indeterminate. */
    UserInfoBestDisplayNameIndeterminate(22000),
    /** `EOS_ConsoleInit_OnNetworkRequestedDeprecatedCallbackNotSet`: The deprecated network callback is not set. */
    ConsoleInitOnNetworkRequestedDeprecatedCallbackNotSet(23000),
    /** `EOS_ConsoleInit_CacheStorage_SizeKBNotMultipleOf16`: Cache storage size is not a multiple of 16. */
    ConsoleInitCacheStorageSizeKbNotMultipleOf16(23001),
    /** `EOS_ConsoleInit_CacheStorage_SizeKBBelowMinimumSize`: Cache storage size is below the minimum. */
    ConsoleInitCacheStorageSizeKbBelowMinimumSize(23002),
    /** `EOS_ConsoleInit_CacheStorage_SizeKBExceedsMaximumSize`: Cache storage size exceeds the maximum. */
    ConsoleInitCacheStorageSizeKbExceedsMaximumSize(23003),
    /** `EOS_ConsoleInit_CacheStorage_IndexOutOfRangeRange`: Cache storage index is out of range. */
    ConsoleInitCacheStorageIndexOutOfRange(23004),
    /** `EOS_UnexpectedError`: An unexpected error occurred. `0x7FFFFFFF`. */
    UnexpectedError(0x7FFFFFFF.toInt()),
    ;

    public companion object {
        private val byValue: Map<Int, EosResult> = entries.associateBy { it.value }
        private val nameLookup = HashMap<Long, EosResult>()

        init {
            // Cache the result-name string with a C downcall so toString() can be
            // implemented cheaply.
            runCatching {
                val handle = Native.downcall(
                    "EOS_EResult_ToString",
                    java.lang.foreign.FunctionDescriptor.of(java.lang.foreign.ValueLayout.ADDRESS, java.lang.foreign.ValueLayout.JAVA_INT)
                )
                for (r in entries) {
                    val seg = handle.invokeExact(r.value) as java.lang.foreign.MemorySegment
                    nameLookup[seg.address()] = r
                }
            }
        }

        /** Resolve a result code value to the corresponding [EosResult] entry. */
        public fun fromValue(value: Int): EosResult = byValue[value] ?: UnexpectedError

        /** Returns true if the result is [Success]. */
        public fun EosResult.isSuccess(): Boolean = this == Success

        /** Returns true if the result is anything other than [Success]. */
        public fun EosResult.isFailure(): Boolean = this != Success

        /** Returns true if the result indicates an in-progress operation that will be retried. */
        public fun EosResult.willRetry(): Boolean = this == OperationWillRetry
    }
}
