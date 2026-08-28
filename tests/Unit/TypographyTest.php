<?php

use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;

use Victorycodedev\ToastKit\Enums\ToastTextAlign;
use Victorycodedev\ToastKit\Enums\ToastTextSize;
use Victorycodedev\ToastKit\Enums\ToastTextWeight;

// ── text() ─────────────────────────────────────────────────────────────────

it('sends text typography with all options', function () {
    $this->kit->make('Body')
        ->text(font: 'Inter', size: 'sm', weight: 'normal', align: 'left', italic: false)
        ->show();

    expect($this->calls[0][1]['text'])->toBe([
        'font' => 'Inter',
        'size' => 'sm',
        'weight' => 'normal',
        'align' => 'left',
        'italic' => false,
    ]);
});

it('sends title_text typography with all options', function () {
    $this->kit->make('Body')
        ->title('Heading')
        ->titleText(font: 'Inter', size: 'lg', weight: 'bold', align: 'center', italic: true)
        ->show();

    expect($this->calls[0][1]['title_text'])->toBe([
        'font' => 'Inter',
        'size' => 'lg',
        'weight' => 'bold',
        'align' => 'center',
        'italic' => true,
    ]);
});

it('sends only supplied typography options', function () {
    $this->kit->make('Body')->text(weight: 'semibold')->show();

    expect($this->calls[0][1]['text'])->toBe(['weight' => 'semibold']);
});

it('supports individual size and alignment options', function () {
    $this->kit->make('Body')->text(size: 'sm', align: 'center')->show();

    expect($this->calls[0][1]['text'])->toBe(['size' => 'sm', 'align' => 'center']);
});

it('supports individual title font option', function () {
    $this->kit->make('Body')->titleText(font: 'Inter')->show();

    expect($this->calls[0][1]['title_text'])->toBe(['font' => 'Inter']);
});

it('accepts typography enums', function () {
    $this->kit->make('Body')
        ->text(size: ToastTextSize::Small, weight: ToastTextWeight::Medium, align: ToastTextAlign::Center)
        ->show();

    expect($this->calls[0][1]['text'])->toBe([
        'size' => 'sm',
        'weight' => 'medium',
        'align' => 'center',
    ]);
});

it('accepts string typography values', function () {
    $this->kit->make('Body')->text(size: 'sm', weight: 'medium', align: 'center')->show();

    expect($this->calls[0][1]['text'])->toBe([
        'size' => 'sm',
        'weight' => 'medium',
        'align' => 'center',
    ]);
});

it('supports italic true and false', function () {
    $this->kit->make('Body')->text(italic: true)->show();
    $this->kit->make('Body')->text(italic: false)->show();

    expect($this->calls[0][1]['text']['italic'])->toBeTrue()
        ->and($this->calls[1][1]['text']['italic'])->toBeFalse();
});

it('keeps message and title typography independent', function () {
    $this->kit->make('Body')
        ->title('Heading')
        ->text(size: 'sm')
        ->titleText(size: 'lg', weight: 'bold')
        ->show();

    expect($this->calls[0][1]['text'])->toBe(['size' => 'sm'])
        ->and($this->calls[0][1]['title_text'])->toBe(['size' => 'lg', 'weight' => 'bold']);
});

it('merges repeated text calls on the same builder', function () {
    $this->kit->make('Body')
        ->text(font: 'Inter')
        ->text(weight: 'bold')
        ->show();

    expect($this->calls[0][1]['text'])->toBe(['font' => 'Inter', 'weight' => 'bold']);
});

it('does not include typography in the payload when not configured', function () {
    $this->kit->success('Saved')->show();

    expect($this->calls[0][1])->not->toHaveKey('text')
        ->and($this->calls[0][1])->not->toHaveKey('title_text');
});

it('isolates typography between toast builders', function () {
    $this->kit->make('A')->text(weight: 'bold')->show();
    $this->kit->make('B')->show();

    expect($this->calls[1][1])->not->toHaveKey('text');
});

// ── Update support ─────────────────────────────────────────────────────────

it('sends typography through a sparse update', function () {
    $this->kit->update('upload')->text(weight: 'bold')->show();

    expect($this->calls[0])->toBe([
        'ToastKit.Update',
        ['id' => 'upload', 'changes' => ['text' => ['weight' => 'bold']]],
    ]);
});

it('updates title typography independently', function () {
    $this->kit->update('upload')->titleText(size: 'lg')->show();

    expect($this->calls[0][1]['changes'])->toBe(['title_text' => ['size' => 'lg']]);
});

it('does not reset other typography properties on a partial text update', function () {
    $this->kit->update('upload')->text(weight: 'bold')->show();

    expect($this->calls[0][1]['changes']['text'])->toBe(['weight' => 'bold']);
});

it('sends message and typography together in an update', function () {
    $this->kit->update('upload')->message('Done')->text(weight: 'semibold')->show();

    expect($this->calls[0][1]['changes'])->toBe([
        'message' => 'Done',
        'text' => ['weight' => 'semibold'],
    ]);
});

// ── Validation ─────────────────────────────────────────────────────────────

it('rejects an invalid text size', function () {
    expect(fn() => $this->kit->make('x')->text(size: 'huge'))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an invalid text weight', function () {
    expect(fn() => $this->kit->make('x')->text(weight: 'heavy'))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an invalid text alignment', function () {
    expect(fn() => $this->kit->make('x')->text(align: 'justify'))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects an empty font name', function () {
    expect(fn() => $this->kit->make('x')->text(font: ''))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn() => $this->kit->make('x')->titleText(font: '   '))->toThrow(InvalidToastConfigurationException::class);
});

it('rejects invalid title typography values', function () {
    expect(fn() => $this->kit->make('x')->titleText(size: 'xxl'))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn() => $this->kit->make('x')->titleText(weight: 'black'))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn() => $this->kit->make('x')->titleText(align: 'middle'))->toThrow(InvalidToastConfigurationException::class);
});

it('treats text() with no arguments as a no-op', function () {
    $this->kit->make('Body')->text()->show();

    expect($this->calls[0][1])->not->toHaveKey('text');
});
