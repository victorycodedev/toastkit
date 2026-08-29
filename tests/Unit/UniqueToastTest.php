<?php

use Victorycodedev\ToastKit\Exceptions\InvalidToastConfigurationException;
use Victorycodedev\ToastKit\Exceptions\ToastKitException;
use Victorycodedev\ToastKit\PendingToast;
use Victorycodedev\ToastKit\ToastKit;

beforeEach(function () {
    $this->native = new class
    {
        public array $active = [];
        public array $unique = [];
        public array $shown = [];
        public array $updates = [];

        public function call(string $method, array $payload): array
        {
            return match ($method) {
                'ToastKit.Show' => $this->show($payload),
                'ToastKit.UpdateUnique' => $this->updateUnique($payload),
                'ToastKit.DismissUnique' => $this->dismissUnique($payload),
                'ToastKit.Dismiss' => $this->dismiss($payload['id']),
                'ToastKit.DismissAll' => $this->dismissAll(),
                default => ['accepted' => true],
            };
        }

        public function terminate(string $id, string $reason): void
        {
            if (! isset($this->active[$id])) return;
            $key = $this->active[$id]['unique_key'] ?? null;
            unset($this->active[$id]);
            if ($key !== null && ($this->unique[$key] ?? null) === $id) unset($this->unique[$key]);
        }

        private function show(array $payload): array
        {
            $key = $payload['unique_key'] ?? null;
            if ($key !== null && isset($this->unique[$key])) {
                return ['id' => $this->unique[$key], 'accepted' => false];
            }
            $this->active[$payload['id']] = $payload;
            if ($key !== null) $this->unique[$key] = $payload['id'];
            $this->shown[] = $payload['id'];
            return ['id' => $payload['id'], 'accepted' => true];
        }

        private function updateUnique(array $payload): array
        {
            $key = $payload['unique_key'];
            if (! isset($this->unique[$key])) return $this->missing($key);
            $id = $this->unique[$key];
            $this->active[$id] = array_replace_recursive($this->active[$id], $payload['changes']);
            $this->updates[] = ['id' => $id, 'changes' => $payload['changes']];
            return ['id' => $id, 'unique_key' => $key, 'accepted' => true];
        }

        private function dismissUnique(array $payload): array
        {
            $key = $payload['unique_key'];
            if (! isset($this->unique[$key])) return $this->missing($key);
            $id = $this->unique[$key];
            $this->terminate($id, 'programmatic');
            return ['id' => $id, 'unique_key' => $key, 'accepted' => true];
        }

        private function dismiss(string $id): array
        {
            $this->terminate($id, 'programmatic');
            return ['id' => $id, 'accepted' => true];
        }

        private function dismissAll(): array
        {
            foreach (array_keys($this->active) as $id) $this->terminate($id, 'programmatic');
            return ['accepted' => true];
        }

        private function missing(string $key): array
        {
            return ['status' => 'error', 'message' => "Unique toast [{$key}] is not active."];
        }
    };

    $native = $this->native;
    $this->uniqueKit = new ToastKit(fn(string $method, array $payload) => $native->call($method, $payload));
});

it('allows repeated ordinary toasts', function () {
    $this->uniqueKit->success('Saved')->show();
    $this->uniqueKit->success('Saved')->show();
    expect($this->native->active)->toHaveCount(2);
});

it('ignores an active duplicate and returns the original UUID', function () {
    $first = $this->uniqueKit->error('Offline')->unique('network-status')->show();
    $second = $this->uniqueKit->error('Still offline')->unique('network-status')->show();

    expect($second)->toBe($first)
        ->and($this->native->active)->toHaveCount(1)
        ->and($this->native->shown)->toBe([$first])
        ->and($this->native->active[$first]['message'])->toBe('Offline');
});

it('allows different unique keys in a stack', function () {
    $this->uniqueKit->error('Offline')->unique('network')->stack()->show();
    $this->uniqueKit->warning('Reconnecting')->unique('websocket')->stack()->show();
    expect($this->native->active)->toHaveCount(2);
});

it('reserves unique keys for queued and stacked toasts', function (string $strategy) {
    $this->uniqueKit->info('First')->unique('status')->strategy($strategy)->show();
    $this->uniqueKit->info('Second')->unique('status')->strategy($strategy)->show();
    expect($this->native->active)->toHaveCount(1);
})->with(['queue', 'stack']);

it('releases a unique key after every native terminal path', function (string $reason) {
    $first = $this->uniqueKit->info('Active')->unique('status')->show();
    $this->native->terminate($first, $reason);
    $second = $this->uniqueKit->info('Active again')->unique('status')->show();
    expect($second)->not->toBe($first)->and($this->native->active)->toHaveCount(1);
})->with(['timeout', 'swipe', 'action', 'programmatic', 'replaced', 'overflow']);

it('releases unique keys through dismiss and dismissAll', function () {
    $first = $this->uniqueKit->info('One')->unique('one')->show();
    $this->uniqueKit->dismiss($first);
    $second = $this->uniqueKit->info('One again')->unique('one')->show();
    $this->uniqueKit->info('Two')->unique('two')->show();
    $this->uniqueKit->dismissAll();

    expect($second)->not->toBe($first)->and($this->native->unique)->toBe([]);
    expect($this->uniqueKit->info('After all')->unique('two')->show())->toBeString();
});

it('updates and dismisses the same toast through its semantic key', function () {
    $id = $this->uniqueKit->error('Offline')->unique('network')->native(ios: true, android: false)->persistent()->show();
    $updatedId = $this->uniqueKit->updateUnique('network')->success()->message('Online')->duration(2000)->show();

    expect($updatedId)->toBe($id)
        ->and($this->native->active)->toHaveCount(1)
        ->and($this->native->active[$id]['unique_key'])->toBe('network')
        ->and($this->native->active[$id]['native'])->toBe(['ios' => true, 'android' => false])
        ->and($this->native->active[$id]['message'])->toBe('Online');

    expect($this->uniqueKit->error('Duplicate')->unique('network')->show())->toBe($id);
    $this->uniqueKit->dismissUnique('network');
    expect($this->native->active)->toBe([]);
});

it('throws a package exception when a semantic target is missing', function () {
    expect(fn() => $this->uniqueKit->updateUnique('missing')->message('x')->show())->toThrow(ToastKitException::class)
        ->and(fn() => $this->uniqueKit->dismissUnique('missing'))->toThrow(ToastKitException::class);
});

it('supports unique presets without leaking builder state', function () {
    $this->uniqueKit->definePreset('offline', fn(PendingToast $toast) => $toast
        ->error()->persistent()->unique('network'));
    $this->uniqueKit->definePreset('syncing', fn(PendingToast $toast) => $toast
        ->info()->persistent()->unique('sync'));

    $first = $this->uniqueKit->preset('offline')->message('Offline')->show();
    $duplicate = $this->uniqueKit->preset('offline')->message('Offline again')->show();
    $other = $this->uniqueKit->preset('syncing')->message('Syncing')->show();

    expect($duplicate)->toBe($first)->and($other)->not->toBe($first)
        ->and($this->native->active)->toHaveCount(2);
});

it('validates unique keys across all PHP entry points', function () {
    expect(fn() => $this->uniqueKit->make('x')->unique(' '))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn() => $this->uniqueKit->updateUnique(''))->toThrow(InvalidToastConfigurationException::class)
        ->and(fn() => $this->uniqueKit->dismissUnique("\t"))->toThrow(InvalidToastConfigurationException::class);
});
