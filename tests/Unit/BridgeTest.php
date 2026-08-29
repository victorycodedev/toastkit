<?php

it('sends the show contract with full defaults', function () {
    $id = $this->kit->make()->message('Hello')->show();

    [$method, $payload] = $this->calls[0];

    expect($method)->toBe('ToastKit.Show')
        ->and($payload)->toMatchArray([
            'id' => $id,
            'contract_version' => 1,
            'message' => 'Hello',
            'title' => null,
            'variant' => 'neutral',
            'position' => 'bottom',
            'duration' => 3000,
            'persistent' => false,
            'animation' => 'scale',
            'swipe_to_dismiss' => true,
            'dismissible' => false,
            'strategy' => 'queue',
            'max_visible' => 3,
            'overflow_behavior' => 'queue',
        ])
        ->and($payload['style'])->toMatchArray([
            'background' => '#1F2937',
            'foreground' => '#FFFFFF',
            'corner_radius' => 16.0,
            'padding' => 16.0,
            'shadow' => true,
        ]);
});

it('applies variant defaults for success', function () {
    $this->kit->success('Saved')->show();

    $payload = $this->calls[0][1];

    expect($payload['icon'])->toBe(['name' => 'check'])
        ->and($payload['style'])->toMatchArray([
            'background' => '#166534',
            'foreground' => '#FFFFFF',
            'icon_color' => '#86EFAC',
        ]);
});

it('lets explicit options override variant defaults', function () {
    $this->kit->success('Saved')->icon('star')->background('#123')->show();

    $payload = $this->calls[0][1];

    expect($payload['icon'])->toBe(['name' => 'star'])
        ->and($payload['style']['background'])->toBe('#123')
        ->and($payload['style']['icon_color'])->toBe('#86EFAC');
});

it('merges custom style onto variant style without dropping icon color', function () {
    $this->kit->success('Saved')->foreground('#000')->show();

    expect($this->calls[0][1]['style']['icon_color'])->toBe('#86EFAC')
        ->and($this->calls[0][1]['style']['foreground'])->toBe('#000');
});

it('sends a sparse update payload', function () {
    $id = $this->kit->update('upload')->message('Done')->show();

    expect($id)->toBe('upload')
        ->and($this->calls[0])->toBe([
            'ToastKit.Update',
            ['id' => 'upload', 'changes' => ['message' => 'Done']],
        ]);
});

it('nests style changes inside the update changes', function () {
    $this->kit->update('upload')->background('#123456')->show();

    expect($this->calls[0])->toBe([
        'ToastKit.Update',
        ['id' => 'upload', 'changes' => ['style' => ['background' => '#123456']]],
    ]);
});

it('sends the dismiss payload', function () {
    $this->kit->dismiss('one');

    expect($this->calls[0])->toBe(['ToastKit.Dismiss', ['id' => 'one']]);
});

it('sends the dismissAll payload', function () {
    $this->kit->dismissAll();

    expect($this->calls[0])->toBe(['ToastKit.DismissAll', []]);
});

it('sends unique show, update, and dismiss contracts', function () {
    $this->kit->error('Offline')->unique('network-status')->show();
    $this->kit->updateUnique('network-status')->message('Online')->show();
    $this->kit->dismissUnique('network-status');

    expect($this->calls[0][1]['unique_key'])->toBe('network-status')
        ->and($this->calls[1])->toBe([
            'ToastKit.UpdateUnique',
            ['unique_key' => 'network-status', 'changes' => ['message' => 'Online']],
        ])
        ->and($this->calls[2])->toBe([
            'ToastKit.DismissUnique',
            ['unique_key' => 'network-status'],
        ]);
});
