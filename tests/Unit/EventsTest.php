<?php

use Victorycodedev\ToastKit\Events\ToastActionPressed;
use Victorycodedev\ToastKit\Events\ToastDismissed;
use Victorycodedev\ToastKit\Events\ToastShown;

it('builds a ToastShown event with a toast ID', function () {
    $event = new ToastShown('one');

    expect($event->toastId)->toBe('one');
});

it('builds a ToastDismissed event with a reason', function () {
    $event = new ToastDismissed('one', 'swipe');

    expect($event->toastId)->toBe('one')
        ->and($event->reason)->toBe('swipe');
});

it('builds a ToastActionPressed event with an action ID', function () {
    $event = new ToastActionPressed('one', 'retry');

    expect($event->toastId)->toBe('one')
        ->and($event->actionId)->toBe('retry');
});

it('accepts every supported dismissal reason', function (string $reason) {
    expect((new ToastDismissed('one', $reason))->reason)->toBe($reason);
})->with(['timeout', 'swipe', 'programmatic', 'action', 'replaced']);

it('rejects an unknown dismissal reason', function () {
    expect(fn () => new ToastDismissed('one', 'magic'))->toThrow(InvalidArgumentException::class);
});
