<?php

it('rejects invalid colors', function () {
    expect(fn () => $this->kit->make('x')->background('red'))->toThrow(InvalidArgumentException::class)
        ->and(fn () => $this->kit->make('x')->foreground('#12'))->toThrow(InvalidArgumentException::class)
        ->and(fn () => $this->kit->make('x')->iconColor('123456'))->toThrow(InvalidArgumentException::class)
        ->and(fn () => $this->kit->make('x')->actionColor('#gggggg'))->toThrow(InvalidArgumentException::class);
});

it('rejects an invalid duration', function () {
    expect(fn () => $this->kit->make('x')->duration(0))->toThrow(InvalidArgumentException::class)
        ->and(fn () => $this->kit->make('x')->duration(-5))->toThrow(InvalidArgumentException::class);
});

it('rejects an invalid position', function () {
    expect(fn () => $this->kit->make('x')->position('left'))->toThrow(InvalidArgumentException::class);
});

it('rejects an invalid animation', function () {
    expect(fn () => $this->kit->make('x')->animation('spin'))->toThrow(InvalidArgumentException::class);
});

it('rejects an invalid strategy', function () {
    expect(fn () => $this->kit->make('x')->strategy('grid'))->toThrow(InvalidArgumentException::class);
});

it('rejects an invalid action', function () {
    expect(fn () => $this->kit->make('x')->action('', 'retry'))->toThrow(InvalidArgumentException::class)
        ->and(fn () => $this->kit->make('x')->action('Retry', ''))->toThrow(InvalidArgumentException::class);
});

it('rejects an invalid custom ID', function () {
    expect(fn () => $this->kit->make('x')->id(''))->toThrow(InvalidArgumentException::class)
        ->and(fn () => $this->kit->make('x')->id('   '))->toThrow(InvalidArgumentException::class);
});

it('rejects an invalid maxVisible', function () {
    expect(fn () => $this->kit->make('x')->maxVisible(0))->toThrow(InvalidArgumentException::class)
        ->and(fn () => $this->kit->make('x')->maxVisible(-1))->toThrow(InvalidArgumentException::class);
});

it('rejects negative padding and corner radius', function () {
    expect(fn () => $this->kit->make('x')->padding(-1))->toThrow(InvalidArgumentException::class)
        ->and(fn () => $this->kit->make('x')->cornerRadius(-1))->toThrow(InvalidArgumentException::class);
});

it('rejects an empty dismiss ID', function () {
    expect(fn () => $this->kit->dismiss(' '))->toThrow(InvalidArgumentException::class);
});

it('rejects an empty update ID', function () {
    expect(fn () => $this->kit->update(' '))->toThrow(InvalidArgumentException::class);
});

it('rejects an update with no changes', function () {
    expect(fn () => $this->kit->update('one')->show())->toThrow(InvalidArgumentException::class);
});
