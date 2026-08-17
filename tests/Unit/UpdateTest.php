<?php

it('retains the ID across an update', function () {
    expect($this->kit->update('upload')->message('50%')->show())->toBe('upload');
});

it('sends only explicitly changed properties', function () {
    $this->kit->update('upload')->message('Done')->success()->show();

    expect($this->calls[0][1]['changes'])->toBe(['message' => 'Done', 'variant' => 'success']);
});

it('changes the variant through an update', function () {
    $this->kit->update('upload')->error()->show();

    expect($this->calls[0][1]['changes']['variant'])->toBe('error');
});

it('changes the timer through a duration update', function () {
    $this->kit->update('upload')->duration(2000)->show();

    expect($this->calls[0][1]['changes'])->toMatchArray(['duration' => 2000, 'persistent' => false]);
});

it('changes persistence through a persistent update', function () {
    $this->kit->update('upload')->persistent()->show();

    expect($this->calls[0][1]['changes'])->toBe(['persistent' => true, 'duration' => null]);
});

it('updates icon and style independently', function () {
    $this->kit->update('upload')->icon('check')->iconColor('#FBBF24')->show();

    expect($this->calls[0][1]['changes'])->toBe([
        'icon' => ['name' => 'check'],
        'style' => ['icon_color' => '#FBBF24'],
    ]);
});

it('updates title and action together', function () {
    $this->kit->update('upload')->title('Done')->action('Retry', 'retry')->show();

    expect($this->calls[0][1]['changes'])->toBe([
        'title' => 'Done',
        'action' => ['id' => 'retry', 'label' => 'Retry'],
    ]);
});
