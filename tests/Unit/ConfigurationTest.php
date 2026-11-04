<?php

use Victorycodedev\ToastKit\Enums\ToastAnimation;
use Victorycodedev\ToastKit\Enums\ToastDirection;
use Victorycodedev\ToastKit\Enums\ToastPosition;
use Victorycodedev\ToastKit\Enums\ToastStrategy;
use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;

// ── Content ────────────────────────────────────────────────────────────────

it('sets the message and title', function () {
    $this->kit->make('Body')->title('Heading')->show();

    expect($this->calls[0][1]['message'])->toBe('Body')
        ->and($this->calls[0][1]['title'])->toBe('Heading');
});

it('allows clearing the title with null', function () {
    $this->kit->make('Body')->title('Heading')->title(null)->show();

    expect($this->calls[0][1]['title'])->toBeNull();
});

it('sets a logical icon name', function () {
    $this->kit->make('Saved')->icon('check')->show();

    expect($this->calls[0][1]['icon'])->toBe(['name' => 'check']);
});

it('sets platform icon overrides', function () {
    $this->kit->make('Saved')
        ->icon('check', ios: 'checkmark.circle.fill', android: 'done')
        ->show();

    expect($this->calls[0][1]['icon'])->toBe([
        'name' => 'check',
        'ios' => 'checkmark.circle.fill',
        'android' => 'done',
    ]);
});

it('supports custom icons shared and mixed with platform icons', function () {
    $shared = $this->kit->make('Shared')->icon('custom:payment')->payload();
    $mixed = $this->kit->make('Mixed')
        ->icon('check', ios: 'checkmark.circle.fill', android: 'custom:payment-android')
        ->payload();
    $reverse = $this->kit->make('Reverse')
        ->icon('custom:payment', ios: 'custom:payment-ios', android: 'verified')
        ->payload();

    expect($shared['icon'])->toBe(['name' => 'custom:payment'])
        ->and($mixed['icon'])->toBe([
            'name' => 'check',
            'ios' => 'checkmark.circle.fill',
            'android' => 'custom:payment-android',
        ])
        ->and($reverse['icon'])->toBe([
            'name' => 'custom:payment',
            'ios' => 'custom:payment-ios',
            'android' => 'verified',
        ]);
});

it('rejects invalid custom icon names in every icon slot', function () {
    expect(fn () => $this->kit->make('x')->icon('custom:'))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->icon(ios: 'custom:Payment'))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->icon(android: 'custom:payment_icon'))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->icon('custom:icons/payment'))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->icon('custom:payment--icon'))->toThrow(InvalidToastConfigurationException::class);
});

it('sets a platform override without a logical name', function () {
    $this->kit->make('Saved')->icon(ios: 'star.fill')->show();

    expect($this->calls[0][1]['icon'])->toBe(['ios' => 'star.fill']);
});

it('sets custom platform overrides without a logical name', function () {
    $ios = $this->kit->make('iOS')->icon(ios: 'custom:payment-ios')->payload();
    $android = $this->kit->make('Android')->icon(android: 'custom:payment-android')->payload();

    expect($ios['icon'])->toBe(['ios' => 'custom:payment-ios'])
        ->and($android['icon'])->toBe(['android' => 'custom:payment-android']);
});

it('rejects an icon with no name or overrides', function () {
    expect(fn () => $this->kit->make('x')->icon())->toThrow(InvalidToastConfigurationException::class);
});

it('sets a predefined icon size', function (string $size) {
    $this->kit->make('Saved')->icon('check')->iconSize($size)->show();

    expect($this->calls[0][1]['icon_size'])->toBe($size);
})->with(['sm', 'md', 'lg']);

it('rejects an invalid icon size', function () {
    expect(fn () => $this->kit->make('x')->iconSize('xl'))
        ->toThrow(InvalidToastConfigurationException::class);
});

it('sets an action with a label and ID', function () {
    $this->kit->make('Failed')->action('Retry', 'retry')->show();

    expect($this->calls[0][1]['action'])->toBe(['id' => 'retry', 'label' => 'Retry']);
});

it('sets one or two actions with required labels and IDs', function () {
    $this->kit->make('Delete draft?')->actions([
        ['label' => 'Cancel', 'id' => 'cancel'],
        ['label' => 'Delete', 'id' => 'delete', 'color' => '#ef4444'],
    ])->show();

    expect($this->calls[0][1]['actions'])->toBe([
        ['id' => 'cancel', 'label' => 'Cancel'],
        ['id' => 'delete', 'label' => 'Delete', 'color' => '#EF4444'],
    ]);
});

it('lets the last single or multiple action method win', function () {
    $actions = [
        ['label' => 'Cancel', 'id' => 'cancel'],
        ['label' => 'Delete', 'id' => 'delete'],
    ];

    $single = $this->kit->make('One')->actions($actions)->action('Undo', 'undo')->payload();
    $multiple = $this->kit->make('Two')->action('Undo', 'undo')->actions($actions)->payload();

    expect($single)->toHaveKey('action', ['id' => 'undo', 'label' => 'Undo'])
        ->not->toHaveKey('actions')
        ->and($multiple)->toHaveKey('actions')
        ->not->toHaveKey('action');
});

it('rejects invalid multiple actions', function () {
    expect(fn () => $this->kit->make('x')->actions([]))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->actions([
            ['label' => 'One', 'id' => 'one'],
            ['label' => 'Two', 'id' => 'two'],
            ['label' => 'Three', 'id' => 'three'],
        ]))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->actions([['label' => 'Missing ID']]))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->actions([
            ['label' => 'One', 'id' => 'same'],
            ['label' => 'Two', 'id' => 'same'],
        ]))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn () => $this->kit->make('x')->actions([
            ['label' => 'One', 'id' => 'one', 'color' => 'red'],
        ]))->toThrow(InvalidToastConfigurationException::class);
});

// ── Presentation ───────────────────────────────────────────────────────────

it('sets the position', function (string $position) {
    $this->kit->make('Hello')->position($position)->show();

    expect($this->calls[0][1]['position'])->toBe($position);
})->with(['top', 'center', 'bottom']);

it('accepts a ToastPosition enum', function () {
    $this->kit->make('Hello')->position(ToastPosition::Top)->show();

    expect($this->calls[0][1]['position'])->toBe('top');
});

it('sets a custom duration and clears persistence', function () {
    $this->kit->make('Hello')->duration(5000)->show();

    expect($this->calls[0][1]['duration'])->toBe(5000)
        ->and($this->calls[0][1]['persistent'])->toBeFalse();
});

it('marks a toast persistent with no duration', function () {
    $this->kit->make('Hello')->persistent()->show();

    expect($this->calls[0][1]['persistent'])->toBeTrue()
        ->and($this->calls[0][1]['duration'])->toBeNull();
});

it('allows persistent(false) without a duration', function () {
    $this->kit->make('Hello')->persistent()->persistent(false)->show();

    expect($this->calls[0][1]['persistent'])->toBeFalse()
        ->and($this->calls[0][1]['duration'])->toBeNull();
});

it('sets the animation', function (string $animation) {
    $this->kit->make('Hello')->animation($animation)->show();

    expect($this->calls[0][1]['animation'])->toBe($animation);
})->with(['fade', 'slide', 'scale', 'spring', 'snap', 'pop', 'reveal', 'bounce']);

it('accepts a ToastAnimation enum', function () {
    $this->kit->make('Hello')->animation(ToastAnimation::Slide)->show();

    expect($this->calls[0][1]['animation'])->toBe('slide');
});

it('sets animation direction independently', function () {
    $this->kit->make('Hello')->direction(ToastDirection::Left)->show();
    expect($this->calls[0][1]['direction'])->toBe('left');
});

it('accepts every direction string', function (string $direction) {
    $this->kit->make('Hello')->direction($direction)->show();
    expect($this->calls[0][1]['direction'])->toBe($direction);
})->with(['auto', 'left', 'right', 'top', 'bottom']);

it('clamps determinate progress', function (int|float $input, int|float $expected) {
    $this->kit->make('Working')->progress($input)->show();
    expect($this->calls[0][1]['progress'])->toBe($expected);
})->with([[42, 42], [12.5, 12.5], [-2, 0], [120, 100]]);

it('configures indeterminate loading', function () {
    $this->kit->make('Working')->loading()->show();
    expect($this->calls[0][1]['loading'])->toBeTrue();
});

it('controls swipe-to-dismiss independently', function () {
    $this->kit->make('Hello')->swipeToDismiss(false)->show();

    expect($this->calls[0][1]['swipe_to_dismiss'])->toBeFalse();
});

it('enables the visible close control with dismissible()', function () {
    $this->kit->make('Hello')->dismissible()->show();

    expect($this->calls[0][1]['dismissible'])->toBeTrue();
});

it('selects the queue strategy', function () {
    $this->kit->make('Hello')->queue()->show();

    expect($this->calls[0][1]['strategy'])->toBe('queue');
});

it('selects the stack strategy', function () {
    $this->kit->make('Hello')->stack()->show();

    expect($this->calls[0][1]['strategy'])->toBe('stack');
});

it('accepts a ToastStrategy enum', function () {
    $this->kit->make('Hello')->strategy(ToastStrategy::Stack)->show();

    expect($this->calls[0][1]['strategy'])->toBe('stack');
});

it('sets the maximum visible stack size', function () {
    $this->kit->make('Hello')->stack()->maxVisible(5)->show();

    expect($this->calls[0][1]['max_visible'])->toBe(5);
});

// ── Styling ────────────────────────────────────────────────────────────────

it('sends custom style overrides', function () {
    $this->kit->make('Hello')
        ->background('#111827')
        ->foreground('#FFFFFF')
        ->iconColor('#FBBF24')
        ->actionColor('#60A5FA')
        ->cornerRadius(18)
        ->padding(12)
        ->shadow(false)
        ->show();

    expect($this->calls[0][1]['style'])->toMatchArray([
        'background' => '#111827',
        'foreground' => '#FFFFFF',
        'icon_color' => '#FBBF24',
        'action_color' => '#60A5FA',
        'corner_radius' => 18.0,
        'padding' => 12.0,
        'shadow' => false,
    ]);
});

it('normalizes colors to uppercase', function () {
    $this->kit->make('Hello')->background('#abc')->show();

    expect($this->calls[0][1]['style']['background'])->toBe('#ABC');
});

it('accepts #RGB, #RRGGBB and #AARRGGBB color formats', function (string $color) {
    $this->kit->make('Hello')->background($color)->show();

    expect($this->calls[0][1]['style']['background'])->toBe(strtoupper($color));
})->with(['#abc', '#a1b2c3', '#ffa1b2c3']);
