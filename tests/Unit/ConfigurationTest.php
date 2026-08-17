<?php

use Victorycodedev\ToastKit\Enums\ToastAnimation;
use Victorycodedev\ToastKit\Enums\ToastPosition;
use Victorycodedev\ToastKit\Enums\ToastStrategy;

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

it('sets a platform override without a logical name', function () {
    $this->kit->make('Saved')->icon(ios: 'star.fill')->show();

    expect($this->calls[0][1]['icon'])->toBe(['ios' => 'star.fill']);
});

it('rejects an icon with no name or overrides', function () {
    expect(fn () => $this->kit->make('x')->icon())->toThrow(InvalidArgumentException::class);
});

it('sets an action with a label and ID', function () {
    $this->kit->make('Failed')->action('Retry', 'retry')->show();

    expect($this->calls[0][1]['action'])->toBe(['id' => 'retry', 'label' => 'Retry']);
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
})->with(['fade', 'slide', 'scale', 'spring']);

it('accepts a ToastAnimation enum', function () {
    $this->kit->make('Hello')->animation(ToastAnimation::Slide)->show();

    expect($this->calls[0][1]['animation'])->toBe('slide');
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
