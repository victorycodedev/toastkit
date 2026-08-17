<?php

use Illuminate\Container\Container;
use Illuminate\Support\Facades\Facade;
use Victorycodedev\ToastKit\Enums\ToastAnimation;
use Victorycodedev\ToastKit\Enums\ToastPosition;
use Victorycodedev\ToastKit\Events\ToastActionPressed;
use Victorycodedev\ToastKit\Events\ToastDismissed;
use Victorycodedev\ToastKit\Events\ToastShown;
use Victorycodedev\ToastKit\Facades\Toast;
use Victorycodedev\ToastKit\PendingToast;
use Victorycodedev\ToastKit\ToastKit;

beforeEach(function () {
    $this->calls = [];
    $this->kit = new ToastKit(function (string $method, array $payload) {
        $this->calls[] = [$method, $payload];
    });
});

it('resolves the Toast facade', function () {
    $container = new Container();
    $container->instance('toastkit', $this->kit);
    Facade::setFacadeApplication($container);
    expect(Toast::make('Hello'))->toBeInstanceOf(PendingToast::class);
    Facade::clearResolvedInstances();
});

it('builds the default show payload and generates a UUID', function () {
    $id = $this->kit->make()->message('Hello')->title('Greeting')->show();
    [$method, $payload] = $this->calls[0];
    expect($id)->toMatch('/^[0-9a-f-]{36}$/')->and($method)->toBe('ToastKit.Show')
        ->and($payload)->toMatchArray([
            'id' => $id, 'contract_version' => 1, 'message' => 'Hello', 'title' => 'Greeting',
            'variant' => 'neutral', 'position' => 'bottom', 'duration' => 3000,
            'persistent' => false, 'animation' => 'spring', 'swipe_to_dismiss' => true,
            'dismissible' => false, 'strategy' => 'queue', 'max_visible' => 3,
            'overflow_behavior' => 'queue',
        ]);
});

it('uses a custom ID', function () {
    expect($this->kit->make('Hello')->id('custom')->show())->toBe('custom');
});

it('supports every variant shortcut', function (string $method, string $variant) {
    $this->kit->{$method}('Message')->show();
    expect($this->calls[0][1]['variant'])->toBe($variant);
})->with([['success', 'success'], ['error', 'error'], ['warning', 'warning'], ['info', 'info'], ['neutral', 'neutral']]);

it('lets explicit customization override variant defaults', function () {
    $this->kit->make('Saved')->icon('star')->background('#123')->success()->show();
    $sent = $this->calls[0][1];
    expect($sent['icon'])->toBe(['name' => 'star'])->and($sent['style']['background'])->toBe('#123')
        ->and($sent['style']['icon_color'])->toBe('#86EFAC');
});

it('supports presentation action style and stack options', function () {
    $this->kit->make('Hello')->position(ToastPosition::Top)->animation(ToastAnimation::Slide)
        ->swipeToDismiss(false)->dismissible()->action('Retry', 'retry')
        ->background('#111827')->foreground('#fff')->iconColor('#22c55e')->actionColor('#60a5fa')
        ->cornerRadius(18)->padding(16)->shadow(false)->stack()->maxVisible(4)->show();
    $payload = $this->calls[0][1];
    expect($payload)->toMatchArray([
        'position' => 'top', 'animation' => 'slide', 'swipe_to_dismiss' => false,
        'dismissible' => true, 'action' => ['id' => 'retry', 'label' => 'Retry'],
        'strategy' => 'stack', 'max_visible' => 4,
    ])->and($payload['style'])->toMatchArray([
        'background' => '#111827', 'foreground' => '#FFF', 'icon_color' => '#22C55E',
        'action_color' => '#60A5FA', 'corner_radius' => 18.0, 'padding' => 16.0, 'shadow' => false,
    ]);
});

it('supports persistence and duration transitions', function () {
    $this->kit->make('Hello')->persistent()->show();
    expect($this->calls[0][1])->toMatchArray(['persistent' => true, 'duration' => null]);
    $this->kit->make('Hello')->persistent()->duration(500)->show();
    expect($this->calls[1][1])->toMatchArray(['persistent' => false, 'duration' => 500]);
});

it('isolates builder state', function () {
    $this->kit->success('Saved')->position('top')->show();
    $this->kit->error('Failed')->show();
    expect($this->calls[1][1]['variant'])->toBe('error')->and($this->calls[1][1]['position'])->toBe('bottom');
});

it('updates only explicitly changed fields and retains the ID', function () {
    $id = $this->kit->update('upload')->message('50%')->background('#123456')->show();
    expect($id)->toBe('upload')->and($this->calls[0])->toBe([
        'ToastKit.Update', ['id' => 'upload', 'changes' => ['message' => '50%', 'style' => ['background' => '#123456']]],
    ]);
});

it('sends dismiss and dismissAll payloads', function () {
    $this->kit->dismiss('one');
    $this->kit->dismissAll();
    expect($this->calls)->toBe([['ToastKit.Dismiss', ['id' => 'one']], ['ToastKit.DismissAll', []]]);
});

it('constructs native events', function () {
    expect(new ToastShown('one'))->toastId->toBe('one')
        ->and(new ToastDismissed('one', 'swipe'))->reason->toBe('swipe')
        ->and(new ToastActionPressed('one', 'retry'))->actionId->toBe('retry');
});

it('registers the v1 bridge events and platform baselines', function () {
    $manifest = json_decode(file_get_contents(dirname(__DIR__).'/nativephp.json'), true, flags: JSON_THROW_ON_ERROR);
    expect(array_column($manifest['bridge_functions'], 'name'))->toBe([
        'ToastKit.Show', 'ToastKit.Update', 'ToastKit.Dismiss', 'ToastKit.DismissAll',
    ])->and($manifest['events'])->toBe([ToastShown::class, ToastDismissed::class, ToastActionPressed::class])
        ->and($manifest['android']['min_version'])->toBe(29)->and($manifest['ios']['min_version'])->toBe('18.0');
});

it('has matching placeholder native bridge classes', function () {
    $root = dirname(__DIR__);
    $android = file_get_contents($root.'/resources/android/src/com/victorycodedev/plugins/toastkit/ToastKitFunctions.kt');
    $ios = file_get_contents($root.'/resources/ios/Sources/ToastKitFunctions.swift');
    foreach (['Show', 'Update', 'Dismiss', 'DismissAll'] as $class) {
        expect($android)->toContain("class {$class}(")->and($android)->toContain(': BridgeFunction')
            ->and($ios)->toContain("class {$class}: BridgeFunction");
    }
});

it('rejects invalid input', function (Closure $operation) {
    expect(fn () => $operation($this->kit))->toThrow(InvalidArgumentException::class);
})->with([
    'missing message' => fn (ToastKit $kit) => $kit->make()->show(),
    'empty message' => fn (ToastKit $kit) => $kit->make(' '),
    'duration' => fn (ToastKit $kit) => $kit->make('x')->duration(0),
    'position' => fn (ToastKit $kit) => $kit->make('x')->position('left'),
    'animation' => fn (ToastKit $kit) => $kit->make('x')->animation('bounce'),
    'variant' => fn (ToastKit $kit) => $kit->make('x')->variant('danger'),
    'color' => fn (ToastKit $kit) => $kit->make('x')->background('red'),
    'action' => fn (ToastKit $kit) => $kit->make('x')->action('', 'retry'),
    'max visible' => fn (ToastKit $kit) => $kit->make('x')->maxVisible(0),
    'empty id' => fn (ToastKit $kit) => $kit->dismiss(' '),
    'empty update' => fn (ToastKit $kit) => $kit->update('one')->show(),
]);
