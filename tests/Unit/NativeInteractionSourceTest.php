<?php

function nativeSource(string $path): string
{
    return file_get_contents(dirname(__DIR__, 2).'/'.$path);
}

it('attaches the Android host immediately when showing the first toast', function () {
    $functions = nativeSource('resources/android/src/com/victorycodedev/plugins/toastkit/ToastKitFunctions.kt');
    $installer = nativeSource('resources/android/src/com/victorycodedev/plugins/toastkit/presentation/ToastKitHostInstaller.kt');

    expect($functions)->toContain('ToastKitHostInstaller.ensureAttached(activity)')
        ->and($installer)->toContain('fun ensureAttached(activity: Activity)')
        ->and($installer)->toContain('Handler(Looper.getMainLooper()).post');
});

it('routes iOS touches only inside measured toast frames', function () {
    $host = nativeSource('resources/ios/Sources/Presentation/ToastKitHost.swift');
    $installer = nativeSource('resources/ios/Sources/Presentation/ToastKitHostInstaller.swift');
    $manager = nativeSource('resources/ios/Sources/Manager/ToastKitManager.swift');

    expect($host)->toContain('ToastKitFramePreferenceKey')
        ->and($host)->toContain('proxy.frame(in: .global)')
        ->and($installer)->toContain('containsInteractivePoint(point)')
        ->and($manager)->toContain('interactiveFrames.values.contains');
});

it('treats Android bridge JSON nulls as absent optional values', function () {
    $normalizer = nativeSource('resources/android/src/com/victorycodedev/plugins/toastkit/bridge/ToastKitBridgeNormalizer.kt');

    expect($normalizer)
        ->toContain('value === JSONObject.NULL')
        ->toContain('optionalValue(values["icon"])')
        ->toContain('optionalValue(values["action"])');
});

it('drives native toast enter and exit transitions explicitly', function () {
    $androidHost = nativeSource('resources/android/src/com/victorycodedev/plugins/toastkit/presentation/ToastKitHost.kt');
    $iosManager = nativeSource('resources/ios/Sources/Manager/ToastKitManager.swift');

    expect($androidHost)
        ->toContain('MutableTransitionState(false)')
        ->toContain('visibility.targetState = shouldBeVisible')
        ->and($iosManager)
        ->toContain('withAnimation(animation(for: state.configuration))');
});
