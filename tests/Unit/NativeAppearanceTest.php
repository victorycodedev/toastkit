<?php

use Victorycodedev\ToastKit\PendingToast;

it('uses the custom renderer on both platforms by default', function () {
    $this->kit->success('Saved')->show();

    expect($this->calls[0][1]['native'])->toBe(['ios' => false, 'android' => false]);
});

it('selects native appearance per platform', function (bool $ios, bool $android) {
    $this->kit->success('Saved')->native(ios: $ios, android: $android)->show();

    expect($this->calls[0][1]['native'])->toBe(['ios' => $ios, 'android' => $android]);
})->with([
    'both platforms' => [true, true],
    'iOS only' => [true, false],
    'Android only' => [false, true],
    'neither platform' => [false, false],
]);

it('keeps both default arguments enabled when only ios is named', function () {
    $this->kit->success('Saved')->native(ios: true)->show();

    expect($this->calls[0][1]['native'])->toBe(['ios' => true, 'android' => true]);
});

it('preserves custom styling regardless of chain order', function () {
    $before = $this->kit->make('Before')->background('#000000')->native(ios: true, android: false)->payload();
    $after = $this->kit->make('After')->native(ios: true, android: false)->background('#000000')->payload();

    expect($before['native'])->toBe($after['native'])
        ->and($before['style'])->toBe($after['style'])
        ->and($before['style']['background'])->toBe('#000000');
});

it('retains content behavior and visual configuration in native mode', function () {
    $payload = $this->kit->make('Payment complete')
        ->title('Success')
        ->success()
        ->icon(ios: 'checkmark.seal', android: 'verified')
        ->action('View', 'view')
        ->position('top')
        ->persistent()
        ->loading()
        ->dismissible()
        ->swipeToDismiss(false)
        ->stack()
        ->background('#000000')
        ->text(size: 'sm')
        ->animation('bounce')
        ->direction('top')
        ->native(ios: true, android: false)
        ->payload();

    expect($payload)->toMatchArray([
        'message' => 'Payment complete',
        'title' => 'Success',
        'native' => ['ios' => true, 'android' => false],
        'action' => ['id' => 'view', 'label' => 'View'],
        'position' => 'top',
        'persistent' => true,
        'loading' => true,
        'dismissible' => true,
        'swipe_to_dismiss' => false,
        'strategy' => 'stack',
        'animation' => 'bounce',
        'direction' => 'top',
    ])->and($payload['icon']['ios'])->toBe('checkmark.seal')
        ->and($payload['icon']['android'])->toBe('verified')
        ->and($payload['style']['background'])->toBe('#000000')
        ->and($payload['text'])->toBe(['size' => 'sm']);
});

it('sends native mode as a sparse update', function () {
    $this->kit->update('toast')->native(ios: false, android: true)->show();

    expect($this->calls[0][1]['changes'])->toBe([
        'native' => ['ios' => false, 'android' => true],
    ]);
});

it('preserves native configuration in fresh presets without leaking state', function () {
    $this->kit->definePreset('system-feedback', fn (PendingToast $toast) => $toast
        ->native(ios: true, android: false)
        ->position('top'));

    $this->kit->preset('system-feedback')->message('One')->show();
    $this->kit->preset('system-feedback')->message('Two')->native(false, false)->show();
    $this->kit->preset('system-feedback')->message('Three')->show();

    expect($this->calls[0][1]['native'])->toBe(['ios' => true, 'android' => false])
        ->and($this->calls[1][1]['native'])->toBe(['ios' => false, 'android' => false])
        ->and($this->calls[2][1]['native'])->toBe(['ios' => true, 'android' => false]);
});

it('keeps unique identity orthogonal to native appearance', function () {
    $payload = $this->kit->warning('Offline')->unique('network')->native(ios: true, android: false)->payload();

    expect($payload['unique_key'])->toBe('network')
        ->and($payload['native'])->toBe(['ios' => true, 'android' => false]);
});
