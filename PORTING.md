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
| `minimumButtonHeight` | `PayButton.MINIMUM_BUTTON_HEIGHT` |
| `tap_extractDataFromUrl` | `tapExtractDataFromUrl` |
| `redirectionReached` / `idleForWhile` / `threeDSCanceled` | same, on the 3ds activity |

## Where the platforms genuinely differ

These are the only places the Android side is not a transcription, and each one is a
platform difference rather than a decision that can be made the same on both sides.

1. **How a passkey ends.** `ASWebAuthenticationSession` claims the callback scheme itself,
   closes the browser on it and hands the url back. Android has no equivalent, so
   `ThreeDSPasskeyCallbackActivity` is the manifest component that claims
   `tapcardwebsdk://onpasskeyredirect` and hands what arrived to the running session.

2. **A passkey is recognised from `on3dsRedirect`, not from the url.** Both sides route to the
   browser only when the card form announces the challenge and `requiresSystemBrowser` agrees
   (`threeDsUrl` contains `passkey` **and** the keyword is `auth_payer`). Android used to also
   sniff any navigation for `/passkey/` in `decidePolicyFor`, in `shouldInterceptRequest` and in
   the popup's `onPageStarted`; that is gone, matching iOS.

3. **Taking the browser down takes three things, not one.** The tab is launched into the app's
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

4. **Noticing the payer left the browser.** iOS gets `canceledLogin`. Android gets nothing,
   so the app returning to the foreground is the signal, delivered by `AppLifecycleObserver`
   to `ThreeDSPasskeySession.hostResumed()`. What it means is
   `PayButtonView.threeDSAssumesReturnOnDismiss`, the same switch as iOS.

5. **Where the passkey is shown.** `ThreeDSBrowser.kt` opens a Chrome Custom Tab, the
   browser's own engine drawn over the app, so `navigator.credentials` and the platform
   authenticator work and the payer never leaves. A device whose browser does not offer custom
   tabs falls back to a plain VIEW intent. Package visibility for the probe is declared in the
   library manifest under `<queries>`; without it every device on Android 11 and up looks like
   it has no custom tabs.

6. **`threeDSPrefersEphemeralSession`** exists for parity and is read by nothing. A custom tab
   has no public way to ask for a private session.

7. **Presentation.** iOS presents `ThreeDSView` as a page sheet. Android runs it as
   `ThreeDsWebViewActivityButton` with a bottom sheet, which is why the "come forward once
   idle" logic lives in the activity rather than in a closure the caller sets.

8. **Native handoffs.** `samsungpay://`, `intent://` and the app store url have no iOS
   counterpart. They live in one marked section of `PayButtonSdkNavigationPolicy.kt`.

9. **Zoom.** iOS needs two separate things, a scroll view setting and an injected viewport,
   because the pinch and the double tap take different paths. Android settles both from
   `WebView.tapDisableZoom()`: with zoom unsupported neither gesture does anything, so there is
   no viewport to inject into a page that is not ours to edit.

10. **The bottom of the 3ds sheet.** iOS stops a scroll view padding its own content for the
   safe area (`contentInsetAdjustmentBehavior = .never`). Android has no such padding, but had
   the same symptom from a different cause: the web area sat in a `ScrollView` whose child was
   `wrap_content`, so the page was handed an unbounded height and scrolled inside a scroller
   that was also scrolling. The `ScrollView` is gone and the page owns its height.

11. **The sdk's own screens open in the app's task.** The scanner, the nfc reader and the
   passkey tab are all started from the activity the button is living in, found by walking out
   through the context wrappers (`tapHostActivity`). `FLAG_ACTIVITY_NEW_TASK` is only used when
   there is no activity to start from. Setting it otherwise puts the screen in a task of its own,
   which the system shows as a second window in Recents labelled with the app's launcher activity
   .. so a payer tapping the scan icon gets a separate window named after whatever that launcher
   happens to be called. Handing off to another app entirely, ex samsung pay or an `intent://`,
   is the one place the flag belongs.

12. **Reading a card over nfc.** Android answers `onNfcClick` itself, through
   `CardNfcActivity` and TapNFCCardReaderKit, landing in the same `window.fillCardInputs` the
   scanner uses. iOS has no counterpart .. it fires the event and leaves it to the merchant,
   because a WKWebView payment form has no equivalent reader to reach for. Followed from
   Card-Android, which answers the same event from the same web sdk the same way. The delegate
   is still told either way.

13. **The card scanner.** Both sides answer `onScannerClick` themselves now. iOS asks for the
   camera before presenting its controller; Android asks inside `CardScannerActivity`, because a
   view has nothing to request a permission with and the screen that needs the camera is already
   an Activity. The library manifest declares `CAMERA`, which merges into every host app.
   `fillScannedCard` is public on Android so a merchant can still run their own scanner instead.

**No longer a difference:** `reset()` after a terminal event. iOS used to call it at the end of
`handleOnSuccess`, `handleOnCancel` and `handleOnError`, and Android deliberately did not,
because the Android page url is built from an intent id that is consumed by then. As of
`e34791c` the iOS calls are commented out too, so both sides now leave the page alone and
`reset()` is something the host calls.

Nothing restarts on its own on either side either. iOS added an automatic start-over in the demo
(`24d0c8b`) and took it back out (`f4be114`) .. it pulled the result off screen and swapped the
button under whoever was reading it. Both demos now offer **Refresh**, which appears once a
payment has ended and creates the next intent when it is asked to.

**Also no longer a difference:** how the demo creates an intent. Both demos had a setting for
choosing between the sdk creating it and the app posting it. iOS dropped it in `d87571f` and so
has Android .. the sdk creates it, which is the path a merchant integrating the button actually
takes, and the demo's own http call is gone with the setting.

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
