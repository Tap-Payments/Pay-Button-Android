# Porting Pay-Button-iOS changes to Android

The two sdks render the same web sdk and answer the same callbacks, so they are kept in the
same shape on purpose. A change made in `Pay-Button-iOS` should land in one obvious file
here. This is that map.

## File map

| Pay-Button-iOS | Pay-Button-Android |
| --- | --- |
| `Logic/Shared/Public/PayButtonDelegate.swift` | `PayButtonDataConfiguration.kt` → `PayButtonStatusDelegate` |
| `Logic/Shared/Public/PayButtonView.swift` (statics) | `PayButtonView.kt` |
| `Logic/Shared/Private/enums/CallBackSchemeEnum.swift` | `enums/CallBackSchemeEnum.kt` |
| `Logic/Shared/Private/enums/PayButtonTypeEnum.swift` | `enums/enums.kt` → `SCHEMES`, `ThreeDsPayButtonType` |
| `Logic/Shared/Private/Models/TapRedirection.swift` | `models/TapRedirection.kt` |
| `Logic/Shared/Private/views/ThreeDSReturn.swift` | `views/ThreeDSReturn.kt` |
| `Logic/Shared/Private/views/ThreeDSPasskeySession.swift` | `views/ThreeDSPasskeySession.kt` |
| `ASWebAuthenticationSession` presentation | `views/ThreeDSBrowser.kt` (Chrome Custom Tab) |
| `Logic/Shared/Private/views/ThreeDSViewController.swift` | `threeDsWebview/ThreeDsWebViewActivityButton.kt` |
| `Logic/Shared/Private/views/PoweredByTapView.swift` | `TapBrandView.kt` + `threeDsWebview/ThreeDsBottomSheetFragmentButton.kt` |
| `SharedDataModels-iOS/Utils/WebUrlUtils.swift` | `utils/WebUrlUtils.kt` |
| `Logic/PayButtonSdk/private/views/PayButtonSdkView.swift` | `PayButton.kt` |
| `PayButtonSdk+NavigationPolicy.swift` | `paybuttonsdk/PayButtonSdkNavigationPolicy.kt` |
| `PayButtonSdk+SdkEvents.swift` | `paybuttonsdk/PayButtonSdkSdkEvents.kt` |
| `PayButtonSdk+CardEvents.swift` | `paybuttonsdk/PayButtonSdkCardEvents.kt` |
| `PayButtonSdk+ThreeDS.swift` | `paybuttonsdk/PayButtonSdkThreeDS.kt` |
| `PayButtonSdk+Popup.swift` + `PayButtonPopupViewController.swift` | `paybuttonsdk/PayButtonSdkPopup.kt` |
| `PayButtonSDK/PayButtonExample.swift` | `app/.../MainActivity.kt` |
| `PayButtonSDK/PayButtonSettingsViewController.swift` | `app/res/xml/preferences.xml` + `SettingsActivity.kt` |
| `PayButtonSDK/IntentJSONEditorViewController.swift` | `app/.../IntentJsonEditorActivity.kt` |
| `PayButtonSDK/IntentRequest.swift` | `MainActivity.buildIntentJson()` |

Swift grows a type with `extension`, Kotlin with extension functions on it, so a Swift
`extension PayButtonSdk { … }` is a Kotlin file of `internal fun PayButton.…`. The one thing
Kotlin cannot do is add an interface conformance from the outside, so
`extension PayButtonSdk: ThreeDSPasskeySessionDelegate` is a small class,
`PayButtonPasskeyDelegate`, at the bottom of the same file.

The demo settings rows map by type: `SwitchRow`→`SwitchPreferenceCompat`,
`TextRow`→`EditTextPreference`, `DecimalRow`→`EditTextPreference` (numberDecimal),
`AlertRow`→`DropDownPreference`, `MultipleSelectorRow`→`MultiSelectListPreference`. The
allowed values live in `app/res/values/strings.xml` as string arrays and must stay verbatim
equal to the Swift enums.

One payload, one place: `buildIntentJson()` is the only builder, `currentIntentJson()` is what
every caller reads, and whatever was saved in the json editor wins over both. The button, the
app side intent creation and the editor all see the same payload.

## Names that carry across

| iOS | Android |
| --- | --- |
| `decidePolicyFor` | `decidePolicyFor` |
| `handleCardWebSdkCallback` | `handleCardWebSdkCallback` |
| `handleOnChargeCreated` / `handleOnSuccess` / `handleOnCancel` / `handleOnError` | same |
| `handleCardRedirection` | `handleCardRedirection` |
| `passCardAuthenticationToSDK` | `passCardAuthenticationToSDK` |
| `handleCardAuthenticationCanceled` | `handleCardAuthenticationCanceled` |
| `requiresSystemBrowser(threeDsUrl:key:)` | `requiresSystemBrowser(threeDsUrl, key)` |
| `startFidoAuthentication` | `startFidoAuthentication` |
| `showRedirectionView` | `showRedirectionView` |
| `teardown` / `reset` | `teardown` / `reset` |
| `updateHeight(to:)` | `updateHeight(height)` |
| `tap_extractDataFromUrl` | `tapExtractDataFromUrl` |
| `redirectionReached` / `idleForWhile` / `threeDSCanceled` | same, on the 3ds activity |

## Where the platforms genuinely differ

These are the only places the Android side is not a transcription, and each one is a
platform difference rather than a decision that can be made the same on both sides.

1. **How a passkey ends.** `ASWebAuthenticationSession` claims the callback scheme itself,
   closes the browser on it and hands the url back. Android has no equivalent, so
   `ThreeDSPasskeyCallbackActivity` is the manifest component that claims
   `tapcardwebsdk://onpasskeyredirect` and hands what arrived to the running session.

2. **Taking the browser down takes three things, not one.** The tab is launched into the app's
   own task (no `FLAG_ACTIVITY_NEW_TASK`); the callback activity receives the bounce; and it
   then relaunches the screen the passkey started from with
   `FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP`, which finishes everything above that
   screen .. the tab. `ThreeDSPasskeySession.returnToHost` does it, off the activity class
   recorded when the browser opened.

   `singleTask` on the receiver is **not** what ends the tab, though it looks like it should
   be. It clears the activities above it only when reusing an instance already in the task,
   and the instance the browser launches is always new, so on its own the receiver finishes
   and uncovers the tab again. `SINGLE_TOP` matters too: with `CLEAR_TOP` alone the host
   screen is destroyed and recreated, taking the button's web view with it, and nothing is
   then left holding the card form to finish the authentication into.

3. **Noticing the payer left the browser.** iOS gets `canceledLogin`. Android gets nothing,
   so the app returning to the foreground is the signal, delivered by `AppLifecycleObserver`
   to `ThreeDSPasskeySession.hostResumed()`. What it means is
   `PayButtonView.threeDSAssumesReturnOnDismiss`, the same switch as iOS.

4. **Where the passkey is shown.** `ThreeDSBrowser.kt` opens a Chrome Custom Tab, the
   browser's own engine drawn over the app, so `navigator.credentials` and the platform
   authenticator work and the payer never leaves. A device whose browser does not offer custom
   tabs falls back to a plain VIEW intent. Package visibility for the probe is declared in the
   library manifest under `<queries>`; without it every device on Android 11 and up looks like
   it has no custom tabs.

5. **`threeDSPrefersEphemeralSession`** exists for parity and is read by nothing. A custom tab
   has no public way to ask for a private session.

6. **Presentation.** iOS presents `ThreeDSView` as a page sheet. Android runs it as
   `ThreeDsWebViewActivityButton` with a bottom sheet, which is why the "come forward once
   idle" logic lives in the activity rather than in a closure the caller sets.

7. **Native handoffs.** `samsungpay://`, `intent://` and the app store url have no iOS
   counterpart. They live in one marked section of `PayButtonSdkNavigationPolicy.kt`.

8. **`reset()` after a terminal event.** iOS calls `reset()` at the end of `handleOnSuccess`
   and `handleOnError`, reloading the button page. Android does not do it automatically: the
   page url is built from an intent id that has been consumed by then, so reloading it shows
   an error state rather than a fresh button. `reset()` exists and can be called by the host.

## Known gap on the iOS side

`ThreeDSPasskeySession.redirectionUrl(from:)` on iOS decodes the callback's `data` and then
does `URL(string: unwrapped)`. The decoded value is sometimes a json string rather than a bare
one, ex `"https://sdk.dev.tap.company/?auth_payer=XXXX"` with the quotes part of the value, and
that does not parse as a url .. so the authentication falls back to a return url it had to
guess. Android handles it in `ThreeDSPasskeySession.unquoted`. The same fix is worth making
in Swift.

## Adding a new callback

1. Add the case to `enums/CallBackSchemeEnum.kt`, matching the Swift spelling exactly.
   Matching is case sensitive on both sides, and it has to stay that way .. `onCancel`
   contains `cancel` once casing stops mattering.
2. Route it in `PayButtonSdkSdkEvents.kt` or `PayButtonSdkCardEvents.kt`, whichever scheme
   it arrives on.
3. Add the method to `PayButtonStatusDelegate` **with a default body**, so an integrator who
   has not implemented it still compiles.
4. Surface it in `app/.../MainActivity.kt` the way the iOS example surfaces it.
